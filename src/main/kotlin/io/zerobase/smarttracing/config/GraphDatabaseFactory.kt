package io.zerobase.smarttracing.config

import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.SigV4WebSocketChannelizer
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

data class Credentials(val username: String, val password: String)

data class Endpoints(val write: String, val read: String?)


@OptIn(ExperimentalUnsignedTypes::class)
class GraphDatabaseFactory {
    companion object {
        val log by LoggerDelegate()
    }

    enum class Mode { READ, WRITE }

    var endpoints: Endpoints = Endpoints(write = "localhost", read = null)
    var port: UInt = 8182u
    var path: String? = null
    var maxConnectionPoolSize: UInt? = null
    var minConnectionPoolSize: UInt? = null
    var workerPoolSize: UInt? = null
    var credentials: Credentials? = null
    var enableAwsSigner: Boolean = false
    var enableSsl: Boolean = false

    fun build(environment: Environment, mode: Mode = Mode.WRITE): GraphTraversalSource {
        val endpoint = if (mode == Mode.WRITE || endpoints.read == null) {
            endpoints.write
        } else {
            endpoints.read
        }
        val builder = Cluster.build()
            .addContactPoints(endpoint)
            .port(port.toInt())
            .enableSsl(enableSsl)
        path?.also { builder.path(it) }
        maxConnectionPoolSize?.let(UInt::toInt)?.also { builder.maxConnectionPoolSize(it) }
        minConnectionPoolSize?.let(UInt::toInt)?.also { builder.minConnectionPoolSize(it) }
        workerPoolSize?.let(UInt::toInt)?.also { builder.workerPoolSize(it) }
        credentials?.also { (u, p) -> builder.credentials(u, p) }

        if (enableAwsSigner) {
            log.info("adding SigV4 to gremlin websocket...")
            builder.channelizer(SigV4WebSocketChannelizer::class.java)
        }

        val cluster = builder.create()

        return AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster))
    }
}
