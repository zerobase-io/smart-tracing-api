package io.zerobase.smarttracing

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Scopes
import com.google.inject.Singleton
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.GraphDatabaseFactory
import io.zerobase.smarttracing.config.GraphDatabaseFactory.Mode.WRITE
import io.zerobase.smarttracing.healthchecks.NeptuneHealthCheck
import io.zerobase.smarttracing.resources.CreatorFilter
import io.zerobase.smarttracing.resources.InvalidIdExceptionMapper
import io.zerobase.smarttracing.resources.TraceIdFilter
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config

class AppModule: AbstractModule() {

    override fun configure() {
        bind(NeptuneHealthCheck::class.java).`in`(Scopes.SINGLETON)
        bind(CreatorFilter::class.java).`in`(Scopes.SINGLETON)
        bind(TraceIdFilter::class.java).`in`(Scopes.SINGLETON)
        bind(InvalidIdExceptionMapper::class.java).`in`(Scopes.SINGLETON)
    }

    @Provides
    @Singleton
    fun graph(env: Environment, @Config factory: GraphDatabaseFactory): GraphTraversalSource = factory.build(env, WRITE)
}
