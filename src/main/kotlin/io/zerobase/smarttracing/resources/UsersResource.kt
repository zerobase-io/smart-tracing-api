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
class UsersResource(val dao: GraphDao, val amazon: Amazon) {

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
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
    fun deleteUser(@PathParam("id") id: String) {
        val to = dao.deleteUser(UserId(id))
        to?.let { amazon.thanksForDeletingUser(it) }
    }

    @Path("/{id}/summary")
    @GET
    fun getUserDump(@PathParam("id") id: String): User? {
        return dao.getUser(UserId(id)) ?: throw NotFoundException()
    }
}
