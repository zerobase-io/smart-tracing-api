package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.models.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 * Request data types to simplify coding.
 */
data class ContactUser(
    val phone: String?,
    val email: String?
)

data class CreateUserRequest(
    val name: String?,
    val contact: ContactUser,
    val deviceId: String
)

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class UsersResource(val dao: GraphDao) {

    @POST
    @Creator
    fun createUser(request: CreateUserRequest): IdWrapper? {
        if (request.name == null && request.contact.phone == null && request.contact.email == null) {
            throw BadRequestException("At least one contact method or a name is required to create a user")
        }
        try {
            return dao.createUser(
                request.name, request.contact.phone,
                request.contact.email, DeviceId(request.deviceId)
            ).let(::IdWrapper)
        } catch (e: InvalidPhoneNumberException) {
            throw BadRequestException(e.message)
        }
    }

    @Path("/{id}")
    @DELETE
    fun deleteUser(@PathParam("id") id: String) {
        dao.deleteUser(UserId(id))
        // TODO: Add email or something
    }

    @Path("/{id}/summary")
    @GET
    fun getUserDump(@PathParam("id") id: String): User? {
        return dao.getUser(UserId(id)) ?: throw NotFoundException()
    }
}
