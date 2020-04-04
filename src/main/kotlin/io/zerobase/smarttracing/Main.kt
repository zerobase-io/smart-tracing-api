package io.zerobase.smarttracing

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.SubscriberExceptionHandler
import com.google.common.io.Resources
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.GraphDatabaseFactory
import io.zerobase.smarttracing.models.ScannableId
import io.zerobase.smarttracing.notifications.AmazonEmailSender
import io.zerobase.smarttracing.notifications.NotificationFactory
import io.zerobase.smarttracing.notifications.NotificationManager
import io.zerobase.smarttracing.pdf.DocumentFactory
import io.zerobase.smarttracing.qr.QRCodeGenerator
import io.zerobase.smarttracing.resources.*
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.w3c.tidy.Tidy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import java.lang.Exception
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.mail.Session
import javax.servlet.DispatcherType
import javax.servlet.FilterRegistration
import javax.ws.rs.core.UriBuilder

typealias MultiMap<K, V> = Map<K, List<V>>

data class AmazonEmailConfig(val region: Region, val endpoint: URI? = null)
data class AmazonConfig(val ses: AmazonEmailConfig)
data class EmailNotificationConfig(val fromAddress: String)
data class NotificationConfig(val email: EmailNotificationConfig, val templateLocation: String = "notifications")
data class Config(
        val database: GraphDatabaseFactory = GraphDatabaseFactory(),
        val siteTypeCategories: MultiMap<String, String>,
        val scannableTypes: List<String>,
        val aws: AmazonConfig,
        val notifications: NotificationConfig,
        val baseQrCodeLink: URI,
        val allowedOrigins: List<String>
): Configuration()

fun main(vararg args: String) {
    Main().run(*args)
}

class Main : Application<Config>() {
    companion object {
        val log by LoggerDelegate()
    }

    override fun initialize(bootstrap: Bootstrap<Config>) {
        bootstrap.objectMapper.registerModules(KotlinModule(), SimpleModule().addDeserializer(Region::class.java, object : JsonDeserializer<Region>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Region = Region.of(p.valueAsString)
        }))
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

        val resolver = ClassLoaderTemplateResolver().apply {
            prefix = "/pdfs"
            suffix = ".html"
            characterEncoding = StandardCharsets.UTF_8.displayName()
        }
        val templateEngine = TemplateEngine().apply {
            templateResolvers = setOf(resolver)
        }

        val dao = GraphDao(graph, phoneUtil)

        val eventBus = AsyncEventBus(
            saneThreadPool("default-event-bus"),
            SubscriberExceptionHandler { exception, context ->
                log.warn(
                    "event handler failed. bus={} handler={}.{} event={}",
                    context.eventBus.identifier(), context.subscriber::class, context.subscriberMethod.name, exception
                )
            }
        )

        val documentFactory = DocumentFactory(templateEngine, Tidy().apply {
            inputEncoding = StandardCharsets.UTF_8.displayName()
            outputEncoding = StandardCharsets.UTF_8.displayName()
            xhtml = true
        })

        val qrCodeGenerator = QRCodeGenerator(
            baseLink = UriBuilder.fromUri(config.baseQrCodeLink),
            logo = Resources.getResource("qr/qr-code-logo.png")
        )
        val notificationFactory = NotificationFactory(TemplateEngine().apply {
            templateResolvers = setOf(ClassLoaderTemplateResolver().apply {
                prefix = "/notifications/"
                characterEncoding = StandardCharsets.UTF_8.displayName()
            })
        }, documentFactory, qrCodeGenerator)

        val notificationManager = NotificationManager(emailSender, notificationFactory)

        eventBus.register(notificationManager)

        env.jersey().register(InvalidPhoneNumberExceptionMapper())
        env.jersey().register(InvalidIdExceptionMapper())
        env.jersey().register(CreatorFilter())
        env.jersey().register(OrganizationsResource(dao, config.siteTypeCategories, config.scannableTypes, eventBus))
        env.jersey().register(DevicesResource(dao))
        env.jersey().register(UsersResource(dao))
        env.jersey().register(ModelsResource(config.siteTypeCategories, config.scannableTypes))

        addCorsFilter(config.allowedOrigins, env)
    }

    private fun addCorsFilter(allowedOrigins: List<String>, env: Environment) {
        val cors: FilterRegistration.Dynamic = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)

        // Configure CORS parameters

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, allowedOrigins.joinToString(separator = ","))
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization")
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD")
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true")

        // Add URL mapping

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")
    }
}

fun saneThreadPool(name: String, coreSize: UInt = 0u, maxSize: UInt = 8u, maxIdleTime: Duration = Duration.ofMinutes(1),
                   daemon: Boolean = true): ExecutorService {
    val threadFactory = ThreadFactoryBuilder().setDaemon(daemon).setNameFormat("$name-%d").build()
    return ThreadPoolExecutor(coreSize.toInt(), maxSize.toInt(), maxIdleTime.toNanos(), TimeUnit.NANOSECONDS, SynchronousQueue(),
        threadFactory
    )
}
