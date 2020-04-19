package io.zerobase.smarttracing

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.GraphDatabaseFactory
import io.zerobase.smarttracing.config.GraphDatabaseFactory.Mode.WRITE
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config

class AppModule: AbstractModule() {

    @Provides
    @Singleton
    fun graph(env: Environment, @Config factory: GraphDatabaseFactory): GraphTraversalSource = factory.build(env, WRITE)
}
