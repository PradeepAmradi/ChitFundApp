package com.chitfund.backend.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles OAuth2 Authorization Code Flow with PKCE for Google and Microsoft.
 *
 * Security measures:
 *  - PKCE (Proof Key for Code Exchange) prevents authorization code interception
 *  - Random state parameter prevents CSRF
 *  - One-time exchange codes prevent token leakage in URLs
 *  - All state/codes are short-lived and single-use
 *  - Provider tokens are never returned to the frontend — only our own JWT is issued
 *
 * Production setup:
 *  Set these environment variables (or application.conf overrides):
 *    GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
 *    MICROSOFT_CLIENT_ID, MICROSOFT_CLIENT_SECRET
 *    APP_BASE_URL  (e.g. https://chitfund-webapp.azurewebsites.net)
 *    FRONTEND_URL  (same as APP_BASE_URL for single-origin deployments)
 */
class OAuthService(
    private val googleClientId: String = System.getenv("GOOGLE_CLIENT_ID") ?: "",
    private val googleClientSecret: String = System.getenv("GOOGLE_CLIENT_SECRET") ?: "",
    private val microsoftClientId: String = System.getenv("MICROSOFT_CLIENT_ID") ?: "",
    private val microsoftClientSecret: String = System.getenv("MICROSOFT_CLIENT_SECRET") ?: "",
    private val microsoftTenantId: String = System.getenv("MICROSOFT_TENANT_ID") ?: "common",
    private val baseUrl: String = System.getenv("APP_BASE_URL") ?: "http://localhost:8080",
    private val frontendUrl: String = System.getenv("FRONTEND_URL") ?: System.getenv("APP_BASE_URL") ?: "http://localhost:8080"
) {

    // ── Constants ────────────────────────────────────────────────────────────
    companion object {
        // Google OAuth2
        private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

        // Microsoft OAuth2 (Azure AD v2.0)
        private fun microsoftAuthUrl(tenant: String) =
            "https://login.microsoftonline.com/$tenant/oauth2/v2.0/authorize"
        private fun microsoftTokenUrl(tenant: String) =
            "https://login.microsoftonline.com/$tenant/oauth2/v2.0/token"
        private const val MICROSOFT_USERINFO_URL = "https://graph.microsoft.com/v1.0/me"

        private const val STATE_EXPIRY_MS  = 10 * 60 * 1000L   // 10 minutes
        private const val CODE_EXPIRY_MS   = 60 * 1000L         // 60 seconds
    }

    // ── HTTP Client ──────────────────────────────────────────────────────────
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    // ── In-memory stores (use Redis / DB for multi-instance production) ──────
    private val oauthStates   = ConcurrentHashMap<String, OAuthStateData>()
    private val exchangeCodes = ConcurrentHashMap<String, ExchangeCodeData>()
    private val secureRandom  = SecureRandom()

    // ── Data classes ─────────────────────────────────────────────────────────
    data class OAuthStateData(
        val provider: String,
        val codeVerifier: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + STATE_EXPIRY_MS
    )

    data class ExchangeCodeData(
        val email: String,
        val name: String,
        val provider: String,
        val providerId: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + CODE_EXPIRY_MS
    )

    /** Returned to the frontend after exchanging the one-time code. */
    data class OAuthUserInfo(
        val email: String,
        val name: String,
        val provider: String,
        val providerId: String
    )

    // ── Provider token / userinfo response DTOs ──────────────────────────────
    @Serializable
    data class GoogleTokenResponse(
        @SerialName("access_token")  val accessToken: String,
        @SerialName("token_type")    val tokenType: String? = null,
        @SerialName("expires_in")    val expiresIn: Int? = null,
        @SerialName("id_token")      val idToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null
    )

    @Serializable
    data class GoogleUserInfo(
        val sub: String,
        val email: String,
        val name: String? = null,
        @SerialName("given_name")    val givenName: String? = null,
        @SerialName("family_name")   val familyName: String? = null,
        val picture: String? = null,
        @SerialName("email_verified") val emailVerified: Boolean? = null
    )

    @Serializable
    data class MicrosoftTokenResponse(
        @SerialName("access_token")  val accessToken: String,
        @SerialName("token_type")    val tokenType: String? = null,
        @SerialName("expires_in")    val expiresIn: Int? = null,
        @SerialName("id_token")      val idToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null
    )

    @Serializable
    data class MicrosoftUserInfo(
        val id: String,
        val displayName: String? = null,
        val givenName: String? = null,
        val surname: String? = null,
        val mail: String? = null,
        val userPrincipalName: String? = null
    )

    @Serializable
    data class OAuthErrorResponse(
        val error: String? = null,
        @SerialName("error_description") val errorDescription: String? = null
    )

    // ── Public: provider availability ────────────────────────────────────────

    fun isGoogleConfigured(): Boolean =
        googleClientId.isNotBlank() && googleClientSecret.isNotBlank()

    fun isMicrosoftConfigured(): Boolean =
        microsoftClientId.isNotBlank() && microsoftClientSecret.isNotBlank()

    // ── PKCE helpers ─────────────────────────────────────────────────────────

    private fun randomUrlSafe(bytes: Int = 32): String {
        val buf = ByteArray(bytes)
        secureRandom.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    private fun sha256Base64Url(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        oauthStates.entries.removeIf   { it.value.expiresAt < now }
        exchangeCodes.entries.removeIf { it.value.expiresAt < now }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GOOGLE
    // ══════════════════════════════════════════════════════════════════════════

    /** Step 1: Build the Google authorization URL and return it. */
    fun buildGoogleAuthUrl(): String {
        cleanupExpired()
        require(isGoogleConfigured()) { "Google OAuth is not configured" }

        val state        = randomUrlSafe()
        val codeVerifier = randomUrlSafe()
        val codeChallenge = sha256Base64Url(codeVerifier)

        oauthStates[state] = OAuthStateData(provider = "google", codeVerifier = codeVerifier)

        val callbackUrl = "$baseUrl/api/v1/auth/oauth/google/callback"

        return URLBuilder(GOOGLE_AUTH_URL).apply {
            parameters.append("client_id",             googleClientId)
            parameters.append("redirect_uri",          callbackUrl)
            parameters.append("response_type",         "code")
            parameters.append("scope",                 "openid email profile")
            parameters.append("state",                 state)
            parameters.append("code_challenge",        codeChallenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("access_type",           "offline")
            parameters.append("prompt",                "select_account")
        }.buildString()
    }

    /**
     * Step 2: Handle Google callback.
     * Validates state, exchanges code for tokens, fetches userinfo,
     * returns a one-time exchange code to embed in the redirect URL.
     */
    suspend fun handleGoogleCallback(code: String, state: String): String {
        // Validate state
        val stateData = oauthStates.remove(state)
            ?: throw SecurityException("Invalid or expired OAuth state")
        if (stateData.provider != "google") throw SecurityException("Provider mismatch")
        if (stateData.expiresAt < System.currentTimeMillis()) throw SecurityException("OAuth state expired")

        // Exchange authorization code for tokens
        val callbackUrl = "$baseUrl/api/v1/auth/oauth/google/callback"

        val tokenResponse: GoogleTokenResponse = httpClient.submitForm(
            url = GOOGLE_TOKEN_URL,
            formParameters = parameters {
                append("client_id",     googleClientId)
                append("client_secret", googleClientSecret)
                append("code",          code)
                append("redirect_uri",  callbackUrl)
                append("grant_type",    "authorization_code")
                append("code_verifier", stateData.codeVerifier)
            }
        ).body()

        // Fetch user info
        val userInfo: GoogleUserInfo = httpClient.get(GOOGLE_USERINFO_URL) {
            header(HttpHeaders.Authorization, "Bearer ${tokenResponse.accessToken}")
        }.body()

        if (userInfo.email.isBlank()) throw IllegalStateException("Google did not return an email address")

        // Generate one-time exchange code
        val exchangeCode = randomUrlSafe()
        exchangeCodes[exchangeCode] = ExchangeCodeData(
            email      = userInfo.email,
            name       = userInfo.name ?: userInfo.email.substringBefore("@"),
            provider   = "google",
            providerId = userInfo.sub
        )

        return exchangeCode
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MICROSOFT
    // ══════════════════════════════════════════════════════════════════════════

    /** Step 1: Build the Microsoft authorization URL. */
    fun buildMicrosoftAuthUrl(): String {
        cleanupExpired()
        require(isMicrosoftConfigured()) { "Microsoft OAuth is not configured" }

        val state        = randomUrlSafe()
        val codeVerifier = randomUrlSafe()
        val codeChallenge = sha256Base64Url(codeVerifier)

        oauthStates[state] = OAuthStateData(provider = "microsoft", codeVerifier = codeVerifier)

        val callbackUrl = "$baseUrl/api/v1/auth/oauth/microsoft/callback"

        return URLBuilder(microsoftAuthUrl(microsoftTenantId)).apply {
            parameters.append("client_id",             microsoftClientId)
            parameters.append("redirect_uri",          callbackUrl)
            parameters.append("response_type",         "code")
            parameters.append("scope",                 "openid email profile User.Read")
            parameters.append("state",                 state)
            parameters.append("code_challenge",        codeChallenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("prompt",                "select_account")
            parameters.append("response_mode",         "query")
        }.buildString()
    }

    /** Step 2: Handle Microsoft callback. */
    suspend fun handleMicrosoftCallback(code: String, state: String): String {
        val stateData = oauthStates.remove(state)
            ?: throw SecurityException("Invalid or expired OAuth state")
        if (stateData.provider != "microsoft") throw SecurityException("Provider mismatch")
        if (stateData.expiresAt < System.currentTimeMillis()) throw SecurityException("OAuth state expired")

        val callbackUrl = "$baseUrl/api/v1/auth/oauth/microsoft/callback"

        val tokenResponse: MicrosoftTokenResponse = httpClient.submitForm(
            url = microsoftTokenUrl(microsoftTenantId),
            formParameters = parameters {
                append("client_id",     microsoftClientId)
                append("client_secret", microsoftClientSecret)
                append("code",          code)
                append("redirect_uri",  callbackUrl)
                append("grant_type",    "authorization_code")
                append("code_verifier", stateData.codeVerifier)
            }
        ).body()

        // Fetch user info from Microsoft Graph
        val userInfo: MicrosoftUserInfo = httpClient.get(MICROSOFT_USERINFO_URL) {
            header(HttpHeaders.Authorization, "Bearer ${tokenResponse.accessToken}")
        }.body()

        val email = userInfo.mail
            ?: userInfo.userPrincipalName
            ?: throw IllegalStateException("Microsoft did not return an email address")

        val exchangeCode = randomUrlSafe()
        exchangeCodes[exchangeCode] = ExchangeCodeData(
            email      = email,
            name       = userInfo.displayName ?: email.substringBefore("@"),
            provider   = "microsoft",
            providerId = userInfo.id
        )

        return exchangeCode
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXCHANGE CODE → USER INFO
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Consume the one-time exchange code and return the verified user info.
     * The code is deleted immediately (single-use).
     */
    fun consumeExchangeCode(code: String): OAuthUserInfo {
        cleanupExpired()
        val data = exchangeCodes.remove(code)
            ?: throw SecurityException("Invalid or expired exchange code")
        if (data.expiresAt < System.currentTimeMillis()) {
            throw SecurityException("Exchange code expired")
        }
        return OAuthUserInfo(
            email      = data.email,
            name       = data.name,
            provider   = data.provider,
            providerId = data.providerId
        )
    }

    /** Frontend redirect URL with the one-time code embedded in the hash. */
    fun buildFrontendRedirectUrl(exchangeCode: String): String =
        "$frontendUrl/#oauth-callback=$exchangeCode"

    /** Frontend redirect URL for error notification. */
    fun buildFrontendErrorUrl(error: String): String =
        "$frontendUrl/#oauth-error=${java.net.URLEncoder.encode(error, "UTF-8")}"
}
