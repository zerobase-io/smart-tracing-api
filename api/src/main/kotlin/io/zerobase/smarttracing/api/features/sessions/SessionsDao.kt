package io.zerobase.smarttracing.api.features.sessions

interface SessionsDao {
    fun save(session: Session)
    fun remove(id: String): Boolean
    fun removeInFlightStateToken(state: String): Boolean
    fun saveInFlightStateToken(stateParameter: String)
}
