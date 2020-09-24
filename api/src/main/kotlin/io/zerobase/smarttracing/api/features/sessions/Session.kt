package io.zerobase.smarttracing.api.features.sessions

import java.time.Instant

data class Session(val id: String, val token: String, val expiration: Instant)
