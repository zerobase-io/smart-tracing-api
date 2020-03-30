package io.zerobase.smarttracing.resources

import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.RawMessage

class AmazonSES : EmailSender {
    private val client: SesClient

    init {
        client = SesClient.builder().region(Region.US_EAST_1).build()
    }

    override fun sendEmail(arr: ByteArray) {
        val data = SdkBytes.fromByteArray(arr)
        val rawMessage = RawMessage.builder().data(data).build()
        val rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build()

        client.sendRawEmail(rawEmailRequest)
    }
}
