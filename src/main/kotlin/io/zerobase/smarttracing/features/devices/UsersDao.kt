package io.zerobase.smarttracing.features.devices

import com.google.inject.Inject
import io.zerobase.smarttracing.gremlin.execute
import io.zerobase.smarttracing.gremlin.getIfPresent
import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.now
import io.zerobase.smarttracing.utils.LoggerDelegate
import io.zerobase.smarttracing.validatePhoneNumber
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import java.util.*

class UsersDao @Inject constructor(private val graph: GraphTraversalSource) {
    companion object {
        private val log by LoggerDelegate()
    }

    /**
     * Creates a user node and links to device id.
     *
     * @param name name of the user.
     * @param phone phone of the user.
     * @param email email of the user.
     * @param id id of the device used to create it.
     *
     * @returns id of the user.
     */
    fun createUser(name: String?, phone: String?, email: String?, deviceId: DeviceId): UserId {
        phone?.apply { validatePhoneNumber(phone) }
        val id = UUID.randomUUID().toString()
        val deviceVertex = graph.V(deviceId.value).getIfPresent() ?: throw InvalidIdException(deviceId)
        try {
            graph.addV("USER")
                .property(T.id, id).property("name", name).property("phone", phone).property("email", email)
                .property("deleted", false).property("timestamp", now())
                .addE("OWNS").to(deviceVertex)
                .execute()
            return UserId(id)
        } catch (ex: Exception) {
            log.error("error creating user for device. device={}", deviceId, ex)
            throw EntityCreationException("Error creating user.", ex)
        }
    }

    /**
     * "Deletes" the user
     *
     * @param id id of the user to delete
     */
    fun deleteUser(id: UserId) {
        try {
            graph.V(id.value).property("deleted", true).execute()
        } catch (ex: Exception) {
            log.error("failed to delete user. id={}", id, ex)
            throw ex
        }
    }

    /**
     * Gets the user
     *
     * @param id the id of the user
     *
     * @return User struct
     */
    fun getUser(id: UserId): User? {
        try {
            val vertex = graph.V(id.value).has("deleted", false).propertyMap<String>().getIfPresent() ?: return null
            return User(
                id = id,
                name = vertex["name"],
                contactInfo = ContactInfo(phoneNumber = vertex["phone"],  email = vertex["email"])
            )
        } catch (ex: Exception) {
            log.error("error getting user. id={}", ex)
            throw ex
        }
    }
}
