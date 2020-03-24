package io.zerobase.smarttracing

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.resources.*
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import java.net.URI
import java.util.*
import javax.servlet.DispatcherType
import javax.servlet.FilterRegistration

typealias MultiMap<K,V> = Map<K, List<V>>

data class Config(val database: Neo4jConfig, val siteTypeCategories: MultiMap<String, String>): Configuration()
data class Neo4jConfig(val url: URI, val username: String, val password: String)

typealias ConnectionConfig = org.neo4j.driver.Config

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
        val driver = GraphDatabase.driver(
                config.database.url,
                AuthTokens.basic(config.database.username, config.database.password),
                ConnectionConfig.builder().withEncryption().build()
        )

        env.jersey().register(Router(GraphDao(driver)))
        env.jersey().register(CreatorFilter())
        env.jersey().register(OrganizationsResource())
        env.jersey().register(DevicesResource())
        env.jersey().register(UsersResource())
        env.jersey().register(ModelsResource(config.siteTypeCategories))

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
