package io.zerobase.smarttracing.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.SubscriberExceptionHandler
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.dropwizard.Application
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.api.config.AppConfig
import io.zerobase.smarttracing.api.resources.CreatorFilter
import io.zerobase.smarttracing.api.resources.InvalidIdExceptionMapper
import io.zerobase.smarttracing.api.resources.InvalidPhoneNumberExceptionMapper
import io.zerobase.smarttracing.api.resources.TraceIdFilter
import io.zerobase.smarttracing.common.LoggerDelegate
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.slf4j.Logger
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.GuiceyOptions
import ru.vyarus.guicey.eventbus.EventBusBundle
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.servlet.DispatcherType
import javax.servlet.FilterRegistration

fun main(vararg args: String) {
    Main().run(*args)
}

class Main : Application<AppConfig>() {
    companion object {
        private val log: Logger by LoggerDelegate()
    }

    private val eventBus = AsyncEventBus(
        saneThreadPool("default-event-bus"),
        SubscriberExceptionHandler { exception, context ->
            log.warn(
                "event handler failed. bus={} handler={}.{} event={}",
                context.eventBus.identifier(), context.subscriber::class, context.subscriberMethod.name, context.event, exception
            )
        }
    )

    private val guiceBundle: GuiceBundle = GuiceBundle.builder()
        .modules(AppModule())
        .option(GuiceyOptions.UseCoreInstallers, true)
        .bundles(EventBusBundle(eventBus))
        .build()

    override fun initialize(bootstrap: Bootstrap<AppConfig>) {
        bootstrap.objectMapper.registerModules(KotlinModule(), SimpleModule().addDeserializer(Region::class.java, object : JsonDeserializer<Region>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Region = Region.of(p.valueAsString)
        }))
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
            bootstrap.configurationSourceProvider,
            EnvironmentVariableSubstitutor(false)
        )

        bootstrap.addBundle(guiceBundle)
    }

    override fun run(config: AppConfig, env: Environment) {
        env.jersey().register(TraceIdFilter())
        env.jersey().register(InvalidPhoneNumberExceptionMapper())
        env.jersey().register(InvalidIdExceptionMapper())
        env.jersey().register(CreatorFilter())

        config.featureFactories.forEach { it.build(env, config, guiceBundle.injector) }

        addCorsFilter(config.allowedOrigins, env)

        config.eventsTopicArn?.also {
            log.info("Found event topic ARN. Setting up event publisher. topic={}", it)
            val sns = guiceBundle.injector.getInstance(SnsClient::class.java)
            eventBus.register(EventPublisher(sns, it, env.objectMapper))
        }
    }

    private fun addCorsFilter(allowedOrigins: String, env: Environment) {
        val cors: FilterRegistration.Dynamic = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, allowedOrigins)
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization")
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD")
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true")

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
