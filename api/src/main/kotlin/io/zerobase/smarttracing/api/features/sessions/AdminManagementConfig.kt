package io.zerobase.smarttracing.api.features.sessions

import io.zerobase.smarttracing.api.config.AmazonServiceConfig
import software.amazon.awssdk.arns.Arn
import software.amazon.awssdk.regions.Region
import java.net.URI

data class DynamoConfig(val region: Region, val endpoint: URI? = null, val tableName: String)

data class OauthClient(val id: String, val secret: String)

data class OauthConfig(val client: Arn, val callback: URI)

// No region on this one because the key ARN has a region in it
data class SigningConfig(val key: Arn, val endpoint: URI? = null)

data class SessionManagementConfig(
    val ssm: AmazonServiceConfig,
    val oauth: OauthConfig,
    val signing: SigningConfig,
    val database: DynamoConfig,
)
