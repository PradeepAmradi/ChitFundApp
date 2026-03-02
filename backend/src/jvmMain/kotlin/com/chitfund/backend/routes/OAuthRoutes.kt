package com.chitfund.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.chitfund.shared.data.*
import com.chitfund.shared.utils.Result
import com.chitfund.backend.db.*
import com.chitfund.backend.services.AuthService
import com.chitfund.backend.services.OAuthService
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

@Serializable
data class OAuthExchangeRequest(val code: String)

@Serializable
data class OAuthProviderInfo(
    val name: String,
    val enabled: Boolean,
    val authUrl: String? = null
)

fun Route.oauthRoutes() {
    val oauthService = OAuthService()
    val authService = AuthService()

    route("/auth/oauth") {

        // ── List available providers ─────────────────────────────────────
        get("/providers") {
            val providers = listOf(
                OAuthProviderInfo(
                    name    = "google",
                    enabled = oauthService.isGoogleConfigured(),
                    authUrl = if (oauthService.isGoogleConfigured()) "/api/v1/auth/oauth/google" else null
                ),
                OAuthProviderInfo(
                    name    = "microsoft",
                    enabled = oauthService.isMicrosoftConfigured(),
                    authUrl = if (oauthService.isMicrosoftConfigured()) "/api/v1/auth/oauth/microsoft" else null
                )
            )
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = providers))
        }

        // ══════════════════════════════════════════════════════════════════
        //  GOOGLE
        // ══════════════════════════════════════════════════════════════════

        /** Redirect the browser to Google's consent screen. */
        get("/google") {
            if (!oauthService.isGoogleConfigured()) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ApiResponse<String>(success = false, message = "Google OAuth is not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables.")
                )
                return@get
            }
            try {
                val authUrl = oauthService.buildGoogleAuthUrl()
                call.respondRedirect(authUrl)
            } catch (e: Exception) {
                application.log.error("Failed to build Google auth URL", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<String>(success = false, message = "Failed to initiate Google sign-in: ${e.message}")
                )
            }
        }

        /** Google redirects the user back here after consent. */
        get("/google/callback") {
            val code  = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]
            val error = call.request.queryParameters["error"]

            if (error != null) {
                application.log.warn("Google OAuth error: $error")
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Google sign-in was cancelled or failed: $error"))
                return@get
            }
            if (code.isNullOrBlank() || state.isNullOrBlank()) {
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Missing authorization code or state parameter"))
                return@get
            }

            try {
                val exchangeCode = oauthService.handleGoogleCallback(code, state)
                call.respondRedirect(oauthService.buildFrontendRedirectUrl(exchangeCode))
            } catch (e: SecurityException) {
                application.log.warn("Google OAuth security error: ${e.message}")
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Security validation failed: ${e.message}"))
            } catch (e: Exception) {
                application.log.error("Google OAuth callback error", e)
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Google sign-in failed: ${e.message}"))
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  MICROSOFT
        // ══════════════════════════════════════════════════════════════════

        get("/microsoft") {
            if (!oauthService.isMicrosoftConfigured()) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ApiResponse<String>(success = false, message = "Microsoft OAuth is not configured. Set MICROSOFT_CLIENT_ID and MICROSOFT_CLIENT_SECRET environment variables.")
                )
                return@get
            }
            try {
                val authUrl = oauthService.buildMicrosoftAuthUrl()
                call.respondRedirect(authUrl)
            } catch (e: Exception) {
                application.log.error("Failed to build Microsoft auth URL", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<String>(success = false, message = "Failed to initiate Microsoft sign-in: ${e.message}")
                )
            }
        }

        get("/microsoft/callback") {
            val code  = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]
            val error = call.request.queryParameters["error"]

            if (error != null) {
                val desc = call.request.queryParameters["error_description"] ?: error
                application.log.warn("Microsoft OAuth error: $desc")
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Microsoft sign-in was cancelled or failed: $desc"))
                return@get
            }
            if (code.isNullOrBlank() || state.isNullOrBlank()) {
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Missing authorization code or state parameter"))
                return@get
            }

            try {
                val exchangeCode = oauthService.handleMicrosoftCallback(code, state)
                call.respondRedirect(oauthService.buildFrontendRedirectUrl(exchangeCode))
            } catch (e: SecurityException) {
                application.log.warn("Microsoft OAuth security error: ${e.message}")
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Security validation failed: ${e.message}"))
            } catch (e: Exception) {
                application.log.error("Microsoft OAuth callback error", e)
                call.respondRedirect(oauthService.buildFrontendErrorUrl("Microsoft sign-in failed: ${e.message}"))
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  EXCHANGE one-time code → JWT
        // ══════════════════════════════════════════════════════════════════

        /**
         * Frontend POSTs the one-time code received via the redirect hash.
         * Backend validates it, finds or creates the user, then issues a JWT.
         */
        post("/exchange") {
            val request = try {
                call.receive<OAuthExchangeRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Invalid request body"))
                return@post
            }

            try {
                val oauthUser = oauthService.consumeExchangeCode(request.code)

                // Find or create user in the database, and record OAuth link
                val authResponse = transaction {
                    val user = findOrCreateOAuthUser(oauthUser.email, oauthUser.name)

                    // Record the OAuth link (idempotent)
                    linkOAuthAccount(
                        userId     = UUID.fromString(user.id),
                        provider   = oauthUser.provider,
                        providerId = oauthUser.providerId,
                        email      = oauthUser.email
                    )

                    val tokens = authService.generateTokens(user.id)
                    AuthResponse(
                        success      = true,
                        token        = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
                        user         = user,
                        message      = "Signed in with ${oauthUser.provider.replaceFirstChar { it.titlecase() }}"
                    )
                }

                call.respond(HttpStatusCode.OK, authResponse)
            } catch (e: SecurityException) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse<String>(success = false, message = e.message ?: "Invalid exchange code"))
            } catch (e: Exception) {
                application.log.error("OAuth exchange error", e)
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, message = "OAuth exchange failed: ${e.message}"))
            }
        }
    }
}

// ── DB helpers (run inside a transaction) ────────────────────────────────────

/** Find an existing user by email, or create a new verified user. */
private fun findOrCreateOAuthUser(email: String, name: String): User {
    val existing = Users.select { Users.email eq email }.firstOrNull()

    return if (existing != null) {
        // Update name if currently a placeholder
        val currentName = existing[Users.name]
        if (currentName.isBlank() || currentName == email.substringBefore("@")) {
            Users.update({ Users.id eq existing[Users.id] }) {
                it[Users.name] = name
                it[isEmailVerified] = true
            }
        }
        User(
            id              = existing[Users.id].toString(),
            email           = existing[Users.email],
            mobile          = existing[Users.mobile],
            name            = if (currentName.isBlank() || currentName == email.substringBefore("@")) name else currentName,
            isEmailVerified = true,
            isMobileVerified = existing[Users.isMobileVerified],
            createdAt       = existing[Users.createdAt].toString()
        )
    } else {
        val userId = UUID.randomUUID()
        Users.insert {
            it[id]              = userId
            it[Users.email]     = email
            it[mobile]          = ""
            it[Users.name]      = name
            it[isEmailVerified] = true
            it[isMobileVerified] = false
        }
        User(
            id              = userId.toString(),
            email           = email,
            mobile          = "",
            name            = name,
            isEmailVerified = true,
            isMobileVerified = false,
            createdAt       = LocalDateTime.now().toString()
        )
    }
}

/** Insert an OAuthAccounts record if one does not already exist. */
private fun linkOAuthAccount(userId: UUID, provider: String, providerId: String, email: String) {
    val exists = OAuthAccounts.select {
        (OAuthAccounts.provider eq provider) and (OAuthAccounts.providerId eq providerId)
    }.firstOrNull()

    if (exists == null) {
        OAuthAccounts.insert {
            it[OAuthAccounts.id]            = UUID.randomUUID()
            it[OAuthAccounts.userId]        = userId
            it[OAuthAccounts.provider]      = provider
            it[OAuthAccounts.providerId]    = providerId
            it[OAuthAccounts.providerEmail] = email
        }
    }
}
