package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.*
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeBodyPart
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Properties
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.RawMessage
import io.zerobase.smarttracing.AmazonConfig

// Currently only implements the AmazonSES
class Amazon(private val config: AmazonConfig) {
    private val session: Session
    private val client: SesClient

    init {
        session = Session.getDefaultInstance(Properties())
        client = SesClient.builder().region(Region.US_EAST_1).build()
    }

    /**
     * Sends a normal email (abstraction to simplify sending messages)
     *
     * @param subject subject to send this under
     * @param html html to send
     * @param to address to send to
     *
     * @throw AddressException when the address is incorrect
     * @throw MessagingException when we cannot message
     * @throw IOException input output exception
     */
    private fun sendNormal(subject: String, html_string: String, to: String) {
        if (config.from != "") {
            val message = MimeMessage(session)

            message.setSubject(subject, "UTF-8")
            message.setFrom(InternetAddress(config.from))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))

            val msgBody = MimeMultipart("alternative")
            val wrap = MimeBodyPart()
            val html = MimeBodyPart()

            html.setContent(html_string, "text/html; charset=UTF-8")
            msgBody.addBodyPart(html)
            wrap.setContent(msgBody)

            val msg = MimeMultipart("mixed")

            message.setContent(msg)
            msg.addBodyPart(wrap)

            val outStream = ByteArrayOutputStream()

            message.writeTo(outStream)

            val arr = outStream.toByteArray()

            val data = SdkBytes.fromByteArray(arr)
            val rawMessage = RawMessage.builder().data(data).build()
            val rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()

            client.sendRawEmail(rawEmailRequest)
        }
    }

    /**
     * Sends a qr code email (abstraction to simplify sending messages)
     *
     * @param subject subject to send this under
     * @param html html to send
     * @param to address to send to
     * @param url the url to encode in a qr code and send
     *
     * @throw AddressException when the address is incorrect
     * @throw MessagingException when we cannot message
     * @throw IOException input output exception
     */
    private fun sendQRScannable(subject: String, html_string: String, to: String, url: String) {
        if (config.from != "") {
            val message = MimeMessage(session)

            message.setSubject(subject, "UTF-8")
            message.setFrom(InternetAddress(config.from))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))

            val attachment = MimeBodyPart()
            val html = MimeBodyPart()

            html.setContent(html_string, "text/html; charset=UTF-8")
            // TODO: Make the qr code generator
            //attachment.setDataHandler(DataHandler(createQR(url)))
            //attachment.setFileName("scannable-qr.pdf")

            val msg = MimeMultipart("mixed")

            message.setContent(msg)
            msg.addBodyPart(html)
            msg.addBodyPart(attachment)

            val outStream = ByteArrayOutputStream()

            message.writeTo(outStream)

            val arr = outStream.toByteArray()

            val data = SdkBytes.fromByteArray(arr)
            val rawMessage = RawMessage.builder().data(data).build()
            val rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()

            client.sendRawEmail(rawEmailRequest)
        }
    }

    /**
     * Sends thank you for joining email to a user
     *
     * @param to address to send to
     *
     * @throw AddressException when the address is incorrect
     * @throw MessagingException when we cannot message
     * @throw IOException input output exception
     */
    fun thanksForJoiningUser(to: String) {
        sendNormal("Thanks for Opting In", "You have opted into the zerobase program", to)
    }

    /**
     * Sends thank you for joining email to an organization
     *
     * @param to address to send to
     *
     * @throw AddressException when the address is incorrect
     * @throw MessagingException when we cannot message
     * @throw IOException input output exception
     */
    fun thanksForJoiningOrg(to: String) {
        sendNormal("Thanks for Opting In", "You have opted into the zerobase program, please place some scannables", to)
    }

    /**
     * Sends thank you for deletine email to a user
     *
     * @param to address to send to
     *
     * @throw AddressException when the address is incorrect
     * @throw MessagingException when we cannot message
     * @throw IOException input output exception
     */
    fun thanksForDeletingUser(to: String) {
        sendNormal("Thanks for Deleting", "We are sorry to hear you go, if you could we would like you to fill out your thoughts on us", to)
    }

    /**
     * Sends thank you for joining email to an organization
     *
     * @param to address to send to
     *
     * @throw AddressException when the address is incorrect
     * @throw MessagingException when we cannot message
     * @throw IOException input output exception
     */
    fun scannableOrgEmail(to: String, url: String) {
        sendQRScannable("Here is your scannable", "Thank you for using zerobase for your point tracing needs", to, url)
    }
}
