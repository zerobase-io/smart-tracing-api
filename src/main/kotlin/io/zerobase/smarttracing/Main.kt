package io.zerobase.smarttracing

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.GraphDatabaseFactory
import io.zerobase.smarttracing.resources.*
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.eclipse.jetty.servlets.CrossOriginFilter
import java.util.*
import javax.servlet.DispatcherType
import javax.servlet.FilterRegistration

typealias MultiMap<K,V> = Map<K, List<V>>

data class Config(
        val database: GraphDatabaseFactory = GraphDatabaseFactory(),
        val siteTypeCategories: MultiMap<String, String>,
        val scannableTypes: List<String>
): Configuration()

fun main(vararg args: String) {
    Main().run(*args)
}

class Main: Application<Config>() {
    override fun initialize(bootstrap: Bootstrap<Config>) {
        bootstrap.objectMapper.registerModule(KotlinModule())
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor(false)
        )
    }

    override fun run(config: Config, env: Environment) {
        val graph: GraphTraversalSource = config.database.build(env)

        /**
         * For phone number verification.
         */
        val phoneUtil = PhoneNumberUtil.getInstance()

        val dao = GraphDao(graph, phoneUtil)

        env.jersey().register(InvalidPhoneNumberExceptionMapper())
        env.jersey().register(InvalidIdExceptionMapper())
        env.jersey().register(Router(dao))
        env.jersey().register(CreatorFilter())
        env.jersey().register(OrganizationsResource(dao, config.siteTypeCategories, config.scannableTypes))
        env.jersey().register(DevicesResource(dao))
        env.jersey().register(UsersResource(dao))
        env.jersey().register(ModelsResource(config.siteTypeCategories, config.scannableTypes))

        addCorsFilter(env)
    }

    private fun addCorsFilter(env: Environment) {
        val cors: FilterRegistration.Dynamic = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)

        // Configure CORS parameters

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*")
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization")
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD")
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true")

        // Add URL mapping

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")
    }
}
