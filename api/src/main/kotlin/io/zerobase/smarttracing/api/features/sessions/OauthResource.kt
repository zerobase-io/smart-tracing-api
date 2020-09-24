package io.zerobase.smarttracing.api.features.sessions

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuth20Service
import io.fusionauth.jwt.JWTEncoder
import io.fusionauth.jwt.Signer
import io.fusionauth.jwt.domain.JWT
import io.zerobase.smarttracing.common.LoggerDelegate
import org.apache.http.client.utils.URIBuilder
import org.slf4j.Logger
import java.net.URI
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.*
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import com.github.scribejava.core.model.Response as OauthResponse

data class OauthResult(
    @QueryParam("code") val authorizationCode: String?,
    @QueryParam("state") val state: String,
    @QueryParam("error") val error: String?
)

data class GoogleUserProfile(
    val id: String,
    val email: String,
    @JsonProperty("hd")
    val domain: String,
    val name: String,
)

@Path("/oauth")
class OauthResource
    @Inject constructor(
        private val sessions: SessionsDao,
        private val service: OAuth20Service,
        private val signer: Signer,
        private val encoder: JWTEncoder,
        private val jsonMapper: ObjectMapper,
        @Named("acceptableDomains") private val acceptableDomains: Set<String>
    )
{
    companion object {
        private val log: Logger by LoggerDelegate()
        private const val profileUrl = "https://www.googleapis.com/userinfo/v2/me"
    }

    @GET
    fun initiateOauthFlow(): Response {
        val oauthStateParameter = UUID.randomUUID().toString()
        sessions.saveInFlightStateToken(oauthStateParameter)

        val authorizationUrl = service.createAuthorizationUrlBuilder().state(oauthStateParameter).build()

        return Response.temporaryRedirect(URI.create(authorizationUrl)).build()
    }

    @GET
    @Path("/callback")
    fun processCallback(@BeanParam result: OauthResult): Response {
        if (!sessions.removeInFlightStateToken(state = result.state)) {
            throw ForbiddenException("State token is not valid")
        }
        if (result.error != null) {
            log.error("oauth result was an error. error={}", result.error)
            TODO("Figure out the redirect url for failure")
        }

        val authorizationCode = result.authorizationCode ?: TODO("figure out where failures go")

        val accessToken: OAuth2AccessToken = service.getAccessToken(authorizationCode)

        val profileRequest = OAuthRequest(Verb.GET, profileUrl)
        service.signRequest(accessToken, profileRequest)
        val profileResponse: OauthResponse = service.execute(profileRequest)
        if (!profileResponse.isSuccessful) {
            return Response.temporaryRedirect(URI.create("failure")).build()
        }
        val body = profileResponse.body
        log.debug("google profile response: $body")
        val userProfile = jsonMapper.readValue<GoogleUserProfile>(body)
        if (acceptableDomains.isNotEmpty() && !acceptableDomains.contains(userProfile.domain)) {
            TODO("figure out where failures go")
        }
        //TODO: Create a user entry in the table.

        val sessionId = UUID.randomUUID().toString()
        val now = ZonedDateTime.now()
        val jwt = JWT().setIssuer("www.zerobase.io").setIssuedAt(now).setSubject("user-id").setExpiration(now.plusHours(1))
            .addClaim("sid", sessionId)

        val token = encoder.encode(jwt, signer)
        // TODO: Include the user id in the session object
        sessions.save(Session(sessionId, token, jwt.expiration.toInstant()))

        // TODO: Figure out the redirect url for success
        return Response.temporaryRedirect(UriBuilder.fromPath("something").queryParam("token", token).build())
            .build();
    }
}
