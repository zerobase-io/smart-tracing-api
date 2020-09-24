package io.zerobase.smarttracing.api.features.sessions

import io.zerobase.smarttracing.api.aws.attributeValue
import io.zerobase.smarttracing.common.LoggerDelegate
import org.slf4j.Logger
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Named

class DynamoSessionsDao @Inject constructor(
    private val db: DynamoDbClient,
    @Named("tableName") private val tableName: String
) : SessionsDao {

    companion object {
        private val log: Logger by LoggerDelegate()
    }

    private val sessions: MutableMap<String, Session> = mutableMapOf()

    override fun saveInFlightStateToken(stateParameter: String) {
        TODO("Not yet implemented")
    }

    override fun save(session: Session) {
        sessions[session.id] = session
//        TODO("Not yet implemented")
    }

    override fun remove(id: String): Boolean {
        return try {
            db.deleteItem {
                it.tableName(tableName).key(mutableMapOf("pk" to attributeValue("SESSION#$id")))
            }
            true
        } catch (ex: Exception) {
            log.error("failed to delete session. id={}", ex)
            false
        }
    }

    override fun removeInFlightStateToken(state: String): Boolean {
        TODO("Not yet implemented")
    }
}
