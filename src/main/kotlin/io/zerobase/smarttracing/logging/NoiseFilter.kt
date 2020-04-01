package io.zerobase.smarttracing.logging

import ch.qos.logback.access.spi.IAccessEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.logging.filter.FilterFactory
import java.time.Duration
import java.time.Instant

@JsonTypeName("noise")
class RequestNoiseFilterFactory(
    val path: String,
    val suppressionInterval: Duration = Duration.ofMinutes(10)
) : FilterFactory<IAccessEvent> {

    override fun build(): Filter<IAccessEvent> {
        return NoiseFilter(value = path, suppressionInterval = suppressionInterval)
    }
}

class NoiseFilter(private val value: String, private val suppressionInterval: Duration): Filter<IAccessEvent>() {
    private var lastAllowedTimestamp: Instant? = null
    private var lastAcceptedStatusCode: Int? = null

    override fun decide(event: IAccessEvent): FilterReply {
        val lastAllowedTimestamp = lastAllowedTimestamp
        if (lastAllowedTimestamp == null) {
            this.lastAllowedTimestamp = Instant.now()
            lastAcceptedStatusCode = event.statusCode
            return FilterReply.NEUTRAL
        }
        if (event.statusCode != lastAcceptedStatusCode) {
            this.lastAllowedTimestamp = Instant.now()
            lastAcceptedStatusCode = event.statusCode
            return FilterReply.NEUTRAL
        }
        if (Instant.now().isAfter(lastAllowedTimestamp.plus(suppressionInterval))) {
            this.lastAllowedTimestamp = Instant.now()
            lastAcceptedStatusCode = event.statusCode
            return FilterReply.NEUTRAL
        }
        return when (event.requestURI.contains(value)) {
            true -> FilterReply.DENY
            false -> {
                this.lastAllowedTimestamp = Instant.now()
                lastAcceptedStatusCode = event.statusCode
                FilterReply.NEUTRAL
            }
        }
    }
}
