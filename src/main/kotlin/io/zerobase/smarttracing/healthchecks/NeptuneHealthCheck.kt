package io.zerobase.smarttracing.healthchecks

import com.codahale.metrics.health.annotation.Async
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.slf4j.MarkerFactory
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck
import java.util.concurrent.TimeUnit

@Async(period = 2, unit = TimeUnit.SECONDS)
class NeptuneHealthCheck(private val graph: GraphTraversalSource) : NamedHealthCheck() {
    companion object {
        private val log by LoggerDelegate()
        private val MARKER = MarkerFactory.getMarker("healthcheck")
    }

    override fun check(): Result {
        try {
            graph.inject(0)
            return Result.healthy()
        } catch (ex: Exception) {
            log.warn(MARKER, "neptune healthcheck failed.", ex)
            return Result.unhealthy(ex)
        }
    }

    override fun getName(): String = "neptune"
}
