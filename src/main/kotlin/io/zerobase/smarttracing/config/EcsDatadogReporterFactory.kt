package io.zerobase.smarttracing.config

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.CharStreams
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.coursera.metrics.datadog.DynamicTagsCallback
import org.coursera.metrics.datadog.DynamicTagsCallbackFactory
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@JsonTypeName("ecs")
class EcsMetadataTagFactory: DynamicTagsCallbackFactory {
    companion object {
        val log by LoggerDelegate()
        private val objectMapper = jacksonObjectMapper()
    }

    override fun build(): DynamicTagsCallback {
        val metadataPath = System.getenv("ECS_CONTAINER_METADATA_FILE")?.let { Paths.get(it) }
        if (metadataPath == null || !Files.exists(metadataPath)) {
            log.info("No ECS metadata file available. Disabling dynamic tags.")
            return DynamicTagsCallback { listOf<String>() }
        }
        val metadata: JsonNode = objectMapper.readTree(metadataPath.toFile())
        val taskRevision = metadata["TaskDefinitionRevision"].asInt()
        val containerId = metadata["ContainerID"].asText()
        val dockerImage = metadata["ImageName"].asText()
        val az = metadata["AvailabilityZone"].asText()

        val instanceId = CharStreams.toString(
            InputStreamReader(
                URL("http://169.254.169.254/latest/meta-data/instance-id").openStream(),
                StandardCharsets.UTF_8
            )
        )

        val tags = listOf(
            "revision:$taskRevision",
            "container_id:$containerId",
            "image:$dockerImage",
            "availibility_zone:$az",
            "instance_id:$instanceId"
        )

        return DynamicTagsCallback { tags }
    }
}
