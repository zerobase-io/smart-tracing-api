package io.zerobase.smarttracing.notifications

import io.zerobase.smarttracing.utils.LoggerDelegate
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.RawMessage
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest.Builder as RawEmailRequestBuilder
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

fun RawEmailRequestBuilder.bytes(bytes: ByteArray): RawEmailRequestBuilder {
    return rawMessage {
        it.data(SdkBytes.fromByteArray(bytes))
    }
}

class AmazonEmailSender(
    private val client: SesClient,
    private val session: Session,
    private val fromAddress: String
) : EmailSender {
    companion object {
        val log by LoggerDelegate()
    }

    override fun sendEmail(subject: String,
                           toAddress: String, body: String,
                           contentType: String,
                           attachments: List<Attachment>) {
        if (attachments.isEmpty()) {
            log.debug("no attachments detected. using simple email api...")
            client.sendEmail {
                it.destination { d -> d.toAddresses(toAddress) }
                    .source(fromAddress)
                    .message { m -> m
                        .subject { c -> c.charset(StandardCharsets.UTF_8.displayName()) }
                        .body { b -> b.html { c -> c.charset(StandardCharsets.UTF_8.displayName()) } } }
            }
        } else {
            log.debug("email has {} attachment(s), using raw mail api...", attachments.size)
            val message = buildRawMessage(subject, toAddress, body, contentType, attachments)

            val outStream = ByteArrayOutputStream()

            message.writeTo(outStream)

            client.sendRawEmail {
                it.bytes(outStream.toByteArray())
            }
        }
    }

    private fun buildRawMessage(subject: String, toAddress: String, body: String, contentType: String, attachments: List<Attachment>): MimeMessage {
        val message = MimeMessage(session)

        message.setSubject(subject, "UTF-8")
        message.setFrom(InternetAddress(fromAddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress))

        val msg = MimeMultipart("mixed")

        val mainBody = MimeBodyPart()
        mainBody.setContent(body, contentType)
        msg.addBodyPart(mainBody)

        attachments.forEach {
            val bds = ByteArrayDataSource(it.data, it.name)
            val att = MimeBodyPart()
            att.dataHandler = DataHandler(bds, it.contentType.toString())
            att.fileName = it.name
            msg.addBodyPart(att)
        }

        message.setContent(msg)
        return message
    }
}
