package io.zerobase.smarttracing.common.models

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    Type(SimpleOrganizationCreated::class)
)
interface ZerobaseEvent

@JsonTypeName("simple-organization-created")
data class SimpleOrganizationCreated(val organization: Organization, val defaultQrCode: String): ZerobaseEvent
