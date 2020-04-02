package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.models.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import com.github.mustachejava.MustacheFactory

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
class UsersResource(val dao: GraphDao,
                    private val mustacheFactory: MustacheFactory,
                    private val emailSender: EmailSender) {

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
        val user = dao.getUser(UserId(id))
        dao.deleteUser(UserId(id))
        if (user != null) {
            val toEmail = user?.email
            val userDeleteNotification = UserDeleteNotification(user.name ?: "No Name", mustacheFactory)
            val emailBody = userDeleteNotification.render()
            toEmail?.let { emailSender.sendEmail("Good luck", it, emailBody) }
        }
    }

    @Path("/{id}/summary")
    @GET
    fun getUserDump(@PathParam("id") id: String): User? {
        return dao.getUser(UserId(id)) ?: throw NotFoundException()
    }
}
