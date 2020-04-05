package io.zerobase.smarttracing

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.amazonaws.partitions.model.Endpoint
import com.google.common.io.Resources
import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.notifications.AmazonEmailSender
import io.zerobase.smarttracing.notifications.NotificationFactory
import io.zerobase.smarttracing.notifications.NotificationManager
import io.zerobase.smarttracing.pdf.DocumentFactory
import io.zerobase.smarttracing.qr.QRCodeGenerator
import org.slf4j.LoggerFactory
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.w3c.tidy.Tidy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.mail.Session
import javax.ws.rs.core.UriBuilder

fun main() {
    (LoggerFactory.getLogger("org.thymeleaf") as Logger).level = Level.TRACE
    val qrCodeGenerator = QRCodeGenerator(
        baseLink = UriBuilder.fromUri(URI.create("https://zerobase.io/")),
        logo = Resources.getResource("qr/qr-code-logo.png")
    )
    val qrCodeId = ScannableId("qr01")
    val resolver = ClassLoaderTemplateResolver().apply {
        suffix = ".html"
        characterEncoding = StandardCharsets.UTF_8.displayName()
    }
    val templateEngine = TemplateEngine().apply {
        templateResolvers = setOf(resolver)
    }
    val documentFactory = DocumentFactory(templateEngine, Tidy().apply {
        inputEncoding = StandardCharsets.UTF_8.displayName()
        outputEncoding = StandardCharsets.UTF_8.displayName()
        xhtml = true
    })

    val fakeOrg = Organization(id = OrganizationId("fake"), name = "Fake Org",
        address = Address("", "", "Manchester", "New Hampshire", "", ""),
        contactName = "", contactInfo = ContactInfo("", ""))

    // welcome letter -- used existing pdf render to create welcome letter
    val bytes: ByteArray = documentFactory.siteOnboardingWelcome(fakeOrg).render()
    // create pdf
    Files.write(Paths.get("pdfs", "zerobase-welcome.pdf"), bytes)

    // qr pdf
    // val bytes: ByteArray = qrCodeGenerator.generate(qrCodeId.value).let { documentFactory.siteOnboarding(fakeOrg, it) }.render()
    // And finally, we create the PDF:
    // Files.write(Paths.get("pdfs", "zerobase-qr.pdf"), bytes)

    // testing out notifications but couldn't figure out configuration
//    val fakeOrg2 = Organization(id = OrganizationId("fake"), name = "Fake Org",
//        address = Address("", "", "Manchester", "New Hampshire", "", ""),
//        contactName = "", contactInfo = ContactInfo("yongldich@gmail.com", ""))

//    val sesClientBuilder = SesClient.builder().region(Region.US_EAST_1)
//    val session = Session.getDefaultInstance(Properties())
//    val emailSender = AmazonEmailSender(sesClientBuilder.build(), session, "yongldich@gmail.com")
//    val notificationFactory = NotificationFactory(templateEngine, documentFactory, qrCodeGenerator)
//
//    val notificationManager = NotificationManager(emailSender, notificationFactory)
//    notificationManager.handleNotificationRequest(SimpleOrganizationCreated(fakeOrg2,ScannableId("qr1")))

}
