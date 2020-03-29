package io.zerobase.smarttracing.config

import io.dropwizard.setup.Environment
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.SigV4WebSocketChannelizer
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

data class Credentials(val username: String, val password: String)

@OptIn(ExperimentalUnsignedTypes::class)
class GraphDatabaseFactory {
    var hosts: Array<String> = arrayOf("localhost")
    var port: UInt = 8182u
    var path: String? = null
    var maxConnectionPoolSize: UInt? = null
    var minConnectionPoolSize: UInt? = null
    var workerPoolSize: UInt? = null
    var credentials: Credentials? = null
    var enableAwsSigner: Boolean = false

    fun build(environment: Environment): GraphTraversalSource {
        val builder = Cluster.build()
                .addContactPoints(*hosts)
                .port(port.toInt())
        path?.also { builder.path(it) }
        maxConnectionPoolSize?.let(UInt::toInt)?.also { builder.maxConnectionPoolSize(it) }
        minConnectionPoolSize?.let(UInt::toInt)?.also { builder.minConnectionPoolSize(it) }
        workerPoolSize?.let(UInt::toInt)?.also { builder.workerPoolSize(it) }
        credentials?.also { (u, p) -> builder.credentials(u, p) }

        if (enableAwsSigner) {
            builder.channelizer(SigV4WebSocketChannelizer::class.java)
        }

        val cluster = builder.create()

        return AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster))
    }
}
