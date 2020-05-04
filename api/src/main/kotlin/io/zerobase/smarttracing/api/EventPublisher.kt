package io.zerobase.smarttracing.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.eventbus.Subscribe
import io.zerobase.smarttracing.common.models.ZerobaseEvent
import software.amazon.awssdk.services.sns.SnsClient

class EventPublisher(private val sns: SnsClient, private val topicArn: String, private val objectMapper: ObjectMapper) {

    @Subscribe
    fun onEvent(event: ZerobaseEvent) = sns.publish { it.message(objectMapper.writeValueAsString(event)).topicArn(topicArn) }
}
