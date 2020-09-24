package io.zerobase.smarttracing.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Scopes
import com.google.inject.Singleton
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.api.config.GraphDatabaseFactory
import io.zerobase.smarttracing.api.config.GraphDatabaseFactory.Mode.WRITE
import io.zerobase.smarttracing.api.config.SnsConfig
import io.zerobase.smarttracing.api.healthchecks.NeptuneHealthCheck
import io.zerobase.smarttracing.api.resources.CreatorFilter
import io.zerobase.smarttracing.api.resources.InvalidIdExceptionMapper
import io.zerobase.smarttracing.api.resources.TraceIdFilter
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config
import software.amazon.awssdk.services.sns.SnsClient

class AppModule: AbstractModule() {

    override fun configure() {
        bind(NeptuneHealthCheck::class.java).`in`(Scopes.SINGLETON)
        bind(CreatorFilter::class.java).`in`(Scopes.SINGLETON)
        bind(TraceIdFilter::class.java).`in`(Scopes.SINGLETON)
        bind(InvalidIdExceptionMapper::class.java).`in`(Scopes.SINGLETON)
    }

    @Provides
    fun objectMapper(env: Environment): ObjectMapper = env.objectMapper

    @Provides
    fun sns(@Config config: SnsConfig): SnsClient {
        val builder = SnsClient.builder().region(config.region);
        config.endpoint?.let(builder::endpointOverride)
        return builder.build()
    }

    @Provides
    @Singleton
    fun graph(env: Environment, @Config factory: GraphDatabaseFactory): GraphTraversalSource = factory.build(env, WRITE)
}
