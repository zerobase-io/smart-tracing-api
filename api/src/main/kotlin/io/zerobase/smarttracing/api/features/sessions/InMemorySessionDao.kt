package io.zerobase.smarttracing.api.features.sessions

class InMemorySessionDao: SessionsDao {
    private val sessions = mutableMapOf<String, Session>()
    private val inFlightStateTokens = mutableSetOf<String>()

    override fun save(session: Session) {
        sessions[session.id] = session
    }

    override fun remove(id: String): Boolean {
        return sessions.remove(id) != null
    }

    override fun removeInFlightStateToken(state: String): Boolean {
        return inFlightStateTokens.remove(state)
    }

    override fun saveInFlightStateToken(stateParameter: String) {
        inFlightStateTokens.add(stateParameter)
    }
}
