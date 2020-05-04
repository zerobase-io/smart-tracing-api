package io.zerobase.smarttracing.api.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.collect.Multimap
import io.dropwizard.Configuration
import io.zerobase.smarttracing.api.features.FeatureFactory
import software.amazon.awssdk.arns.Arn
import software.amazon.awssdk.regions.Region
import java.net.URI

data class AmazonEmailConfig(val region: Region, val endpoint: URI? = null)

data class S3Config(val region: Region, val endpoint: URI? = null)

data class SnsConfig(val region: Region, val endpoint: URI? = null)

data class AmazonConfig(val ses: AmazonEmailConfig, val s3: S3Config)

data class EmailNotificationConfig(val fromAddress: String)

data class NotificationConfig(
    val email: EmailNotificationConfig,
    val templateLocation: String = "notifications",
    val staticResourcesBucket: String
)

data class AppConfig(
    val database: GraphDatabaseFactory = GraphDatabaseFactory(),
    val aws: AmazonConfig,
    val notifications: NotificationConfig,
    val baseQrCodeLink: URI,
    val allowedOrigins: String,
    val siteTypeCategories: Multimap<String, String>,
    val scannableTypes: Set<String>,
    val eventsTopicArn: String?,
    // Workaround for tests
    val enableAllFeatures: Boolean,
    @JsonProperty("features")
    val featureFactories: List<FeatureFactory>
): Configuration()
