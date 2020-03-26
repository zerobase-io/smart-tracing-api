package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.User
import io.zerobase.smarttracing.models.UserId
import io.zerobase.smarttracing.models.DeviceId
import io.zerobase.smarttracing.models.IdWrapper
import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.models.InvalidPhoneNumberException
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
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
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
    fun createUser(request: CreateUserRequest): IdWrapper? {
        try {
            val id = dao.createUser(
                request.name, request.contact.phone,
                request.contact.email, DeviceId(request.deviceId)
            )
            return id?.let { IdWrapper(id) }
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
    fun getUserDump(@PathParam("id") id: String): User {
        return dao.getUser(UserId(id))
    }
}
