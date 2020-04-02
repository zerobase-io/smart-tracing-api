package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.Attachment

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
import javax.activation.DataHandler

import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.RawMessage

class AmazonSES(val client: SesClient, val session: Session, val fromAddress: String) : EmailSender {

    override fun sendEmail(subject: String,
                           toAddress: String, body: String,
                           contentType: String,
                           attachment: Attachment?) {
        val message = MimeMessage(session)

        message.setSubject(subject, "UTF-8")
        message.setFrom(InternetAddress(fromAddress))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress))

        val msgBody = MimeBodyPart()

        msgBody.setContent(body, contentType)

        val msg = MimeMultipart("mixed")

        message.setContent(msg)

        msg.addBodyPart(msgBody)

        if (attachment != null) {
            val array = attachment.array
            val bds = ByteArrayDataSource(array, attachment.name)
            val att = MimeBodyPart()
            att.setDataHandler(DataHandler(bds))
            att.setFileName(attachment.name)
            msg.addBodyPart(att)
        }

        val outStream = ByteArrayOutputStream()

        message.writeTo(outStream)

        val arr = outStream.toByteArray()
        val data = SdkBytes.fromByteArray(arr)
        val rawMessage = RawMessage.builder().data(data).build()
        val rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()

        client.sendRawEmail(rawEmailRequest)
    }
}
