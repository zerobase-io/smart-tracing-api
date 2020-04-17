package io.zerobase.smarttracing

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.inject.AbstractModule
import com.google.inject.Provides
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.AmazonEmailConfig
import io.zerobase.smarttracing.config.EmailNotificationConfig
import io.zerobase.smarttracing.config.GraphDatabaseFactory
import io.zerobase.smarttracing.notifications.AmazonEmailSender
import io.zerobase.smarttracing.notifications.EmailSender
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import software.amazon.awssdk.services.ses.SesClient
import java.nio.charset.StandardCharsets
import java.util.*
import com.google.inject.Singleton
import javax.mail.Session

class AppModule: AbstractModule() {

    @Provides
    @Singleton
    fun graphDao(env: Environment, factory: GraphDatabaseFactory): GraphDao {
        val source: GraphTraversalSource = factory.build(env, GraphDatabaseFactory.Mode.WRITE)
        return GraphDao(source, PhoneNumberUtil.getInstance())
    }
}
