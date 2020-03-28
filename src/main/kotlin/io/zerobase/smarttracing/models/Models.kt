package io.zerobase.smarttracing.models

import com.fasterxml.jackson.annotation.JsonValue

interface Id {
    val value: String
}

inline class Fingerprint(val value: String)
inline class DeviceId(@JsonValue override val value: String): Id
inline class SiteId(@JsonValue override val value: String): Id
inline class ScanId(@JsonValue override val value: String): Id
inline class OrganizationId(@JsonValue override val value: String): Id
inline class UserId(@JsonValue override val value: String): Id
inline class ScannableId(@JsonValue override val value: String): Id

data class IdWrapper(val id: Id)

data class User(val name: String?, val phone: String?, val email: String?, val id: String)

data class Location(
    val latitude: Float,
    val longitude: Float
)
