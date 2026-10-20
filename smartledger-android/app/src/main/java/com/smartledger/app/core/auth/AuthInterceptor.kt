package com.smartledger.app.core.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that attaches the Bearer JWT token to every outgoing request.
 *
 * Note: 401 auto-refresh logic is implemented in Phase 2 (AuthRepositoryImpl).
 * On 401, the interceptor currently passes through — Phase 2 will add retry logic.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getAccessToken()
        val request = chain.request().newBuilder()
            .apply {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        val response = chain.proceed(request)

        // 401 auto-refresh will be handled in Phase 2:
        // If 401 → call POST /auth/refresh → retry original request
        // If refresh fails → clearTokens() → navigate to Login

        return response
    }
}
