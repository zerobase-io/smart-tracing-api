package io.zerobase.smarttracing.notifications

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.RawMessage
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.activation.DataHandler
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

class AmazonEmailSender(
    private val client: SesClient,
    private val session: Session,
    private val fromAddress: String
) : EmailSender {

    override fun sendEmail(subject: String,
                           toAddress: String, body: String,
                           contentType: String,
                           attachment: Attachment?) {
        if (attachment == null) {
            client.sendEmail {
                it.destination { d -> d.toAddresses(toAddress) }
                    .source(fromAddress)
                    .message { m -> m
                        .subject { c -> c.charset(StandardCharsets.UTF_8.displayName()) }
                        .body { b -> b.html { c -> c.charset(StandardCharsets.UTF_8.displayName()) } } }
            }
        } else {
            val message = buildRawMessage(subject, toAddress, body, contentType, attachment)

            val outStream = ByteArrayOutputStream()

            message.writeTo(outStream)

            val arr = outStream.toByteArray()
            val data = SdkBytes.fromByteArray(arr)
            val rawMessage = RawMessage.builder().data(data).build()
            val rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()

            client.sendRawEmail(rawEmailRequest)
        }
    }

    private fun buildRawMessage(subject: String, toAddress: String, body: String, contentType: String, attachment: Attachment): MimeMessage {
        val message = MimeMessage(session)

        message.setSubject(subject, "UTF-8")
        message.setFrom(InternetAddress(fromAddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress))

        val msgBody = MimeBodyPart()

        msgBody.setContent(body, contentType)

        val msg = MimeMultipart("mixed")

        message.setContent(msg)

        msg.addBodyPart(msgBody)
        val bds = ByteArrayDataSource(attachment.array, attachment.name)
        val att = MimeBodyPart()
        att.dataHandler = DataHandler(bds, attachment.contentType.toString())
        att.fileName = attachment.name
        msg.addBodyPart(att)
        return message
    }
}
