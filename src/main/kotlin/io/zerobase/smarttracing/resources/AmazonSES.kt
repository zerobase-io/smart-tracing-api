package io.zerobase.smarttracing.resources

import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.RawMessage

class AmazonSES(val client: SesClient) : EmailSender {

    override fun sendEmail(arr: ByteArray) {
        val data = SdkBytes.fromByteArray(arr)
        val rawMessage = RawMessage.builder().data(data).build()
        val rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()

        client.sendRawEmail(rawEmailRequest)
    }
}
