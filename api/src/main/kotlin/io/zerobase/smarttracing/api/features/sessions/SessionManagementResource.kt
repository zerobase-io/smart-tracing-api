package io.zerobase.smarttracing.api.features.sessions

import com.github.scribejava.core.oauth.OAuth20Service
import java.net.URI
import java.util.*
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.Response

@Path("/sessions")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
class SessionManagementResource
    @Inject constructor(private val sessions: SessionsDao, private val oauth: OAuth20Service) {

    // Have to return a response here because we need to return a 302
    @POST
    fun createNewSession(oauthToken: String): Response {
        val oauthStateParameter = UUID.randomUUID().toString()
        sessions.saveInFlightStateToken(oauthStateParameter)

        val authorizationUrl = oauth.createAuthorizationUrlBuilder().state(oauthStateParameter).build()

        return Response.temporaryRedirect(URI.create(authorizationUrl)).build()
    }

    @DELETE
    @Path("/{id}")
    fun terminateExistingSession(@PathParam("id") sessionId: String) {
        if (!sessions.remove(sessionId)) {
            throw NotFoundException("Session ID not found.")
        }
    }
}
