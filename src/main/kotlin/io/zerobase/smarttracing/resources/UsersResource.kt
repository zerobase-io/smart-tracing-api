package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.IdWrapper
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

//region Request Models

//endregion

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class UsersResource {

    @POST
    @Creator
    fun createUser(request: Any): IdWrapper {
        TODO("not implemented")
    }

    @Path("/{id}")
    @DELETE
    fun deleteUser(@PathParam("id") id: String) {
        TODO("not implemented")
    }

    @Path("/{id}/summary")
    @GET
    fun getUserDump(@PathParam("id") id: String): Any {
        TODO("not implemented")
    }
}
