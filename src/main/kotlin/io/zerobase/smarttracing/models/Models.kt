package io.zerobase.smarttracing.models

import com.fasterxml.jackson.annotation.JsonValue
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

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

data class User(val id: UserId, val name: String?, val phone: String?, val email: String?)

data class Location(
    val latitude: Float,
    val longitude: Float
)

@SuppressFBWarnings("EI_EXPOSE_REP", justification = "This is temporary, should be fixed later")
class Attachment(val array: ByteArray, val name: String)
