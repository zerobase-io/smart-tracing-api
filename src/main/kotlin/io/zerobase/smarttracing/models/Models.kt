package io.zerobase.smarttracing.models

interface Id {
    val value: String
}

inline class Fingerprint(val value: String)
inline class DeviceId(override val value: String): Id
inline class SiteId(override val value: String): Id
inline class ScanId(override val value: String): Id
inline class OrganizationId(override val value: String): Id
inline class UserId(override val value: String): Id
inline class ScannableId(override val value: String): Id

data class IdWrapper(val id: Id)
