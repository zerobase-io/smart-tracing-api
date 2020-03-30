package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.*
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeBodyPart
import javax.mail.util.ByteArrayDataSource
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Properties
import java.io.File
import java.io.InputStream
import com.google.common.io.Resources
import javax.activation.DataHandler

// Currently only implements the AmazonSES
class Email(private val fromEmailSES: String, private val amazonSES: AmazonSES,
            private val config: EmailConfig) {
    private val session: Session

    init {
        session = Session.getDefaultInstance(Properties())
    }

    /**
     * Sends an email
     *
     * @param subject subject to send this under
     * @param html html to send
     * @param to address to send to
     * @param attachment possible attachment to use
     * @param type type of email to send
     *
     * @throw AddressException when the address is incorrect
     * @throw MessagingException when we cannot message
     * @throw IOException input output exception
     */
    fun send(to: String, attachment: ByteArray?, type: EmailType) {
        if (config[type] == null) {
            throw InvalidEmailType("Invalid email type ${type}")
        }
        val subject = config[type]!!.subject
        val resource_name = config[type]!!.resource
        val html_string = Resources.toString(
            Resources.getResource("${resource_name}.html"),
            Charsets.UTF_8
        )
        val text_string = Resources.toString(
            Resources.getResource("${resource_name}.txt"),
            Charsets.UTF_8
        )
        if (fromEmailSES != "") {
            val message = MimeMessage(session)

            message.setSubject(subject, "UTF-8")
            message.setFrom(InternetAddress(fromEmailSES))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))

            val msgBody = MimeMultipart("alternative")
            val wrap = MimeBodyPart()
            val html = MimeBodyPart()
            val text = MimeBodyPart()

            html.setContent(html_string, "text/html; charset=UTF-8")
            text.setContent(text_string, "text/plain; charset=UTF-8")
            msgBody.addBodyPart(text)
            msgBody.addBodyPart(html)
            wrap.setContent(msgBody)

            val msg = MimeMultipart("mixed")

            message.setContent(msg)
            msg.addBodyPart(wrap)

            if (attachment != null) {
                val attach_name = config[type]!!.attach_name ?: "undefined"
                val bds = ByteArrayDataSource(attachment, attach_name)
                val att = MimeBodyPart()
                att.setDataHandler(DataHandler(bds))
                att.setFileName(attach_name)
                msg.addBodyPart(att)
            }

            val outStream = ByteArrayOutputStream()

            message.writeTo(outStream)

            val arr = outStream.toByteArray()

            amazonSES.sendEmail(arr)
        }
    }
}
