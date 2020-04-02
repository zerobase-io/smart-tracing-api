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
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import com.github.mustachejava.DefaultMustacheFactory
import io.zerobase.smarttracing.notifications.AmazonEmailSender
import io.zerobase.smarttracing.notifications.NotificationFactory
import io.zerobase.smarttracing.notifications.NotificationManager
import java.net.URI
import javax.mail.Session

typealias MultiMap<K,V> = Map<K, List<V>>

data class AmazonEmailConfig(val region: Region, val endpoint: URI? = null)
data class AmazonConfig(val ses: AmazonEmailConfig)
data class EmailNotificationConfig(val fromAddress: String)
data class NotificationConfig(val email: EmailNotificationConfig, val templateLocation: String = "notifications")
data class Config(
        val database: GraphDatabaseFactory = GraphDatabaseFactory(),
        val siteTypeCategories: MultiMap<String, String>,
        val scannableTypes: List<String>,
        val aws: AmazonConfig,
        val notifications: NotificationConfig
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

        val session = Session.getDefaultInstance(Properties())

        /**
         * For phone number verification.
         */
        val phoneUtil = PhoneNumberUtil.getInstance()

        // For emails
        val sesClientBuilder = SesClient.builder().region(config.aws.ses.region)
        config.aws.ses.endpoint?.let(sesClientBuilder::endpointOverride)
        val emailSender = AmazonEmailSender(sesClientBuilder.build(), session, config.notifications.email.fromAddress)
        val notificationManager = NotificationManager(emailSender)
        val notificationFactory = NotificationFactory(DefaultMustacheFactory(config.notifications.templateLocation))

        val dao = GraphDao(graph, phoneUtil)

        env.jersey().register(InvalidPhoneNumberExceptionMapper())
        env.jersey().register(InvalidIdExceptionMapper())
        env.jersey().register(Router(dao))
        env.jersey().register(CreatorFilter())
        env.jersey().register(OrganizationsResource(dao, config.siteTypeCategories, config.scannableTypes, notificationManager, notificationFactory))
        env.jersey().register(DevicesResource(dao))
        env.jersey().register(UsersResource(dao, notificationManager, notificationFactory))
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
