package io.zerobase.smarttracing.api.aws

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

fun attributeValue(value: String): AttributeValue = AttributeValue.builder().s(value).build()
fun attributeValue(value: Number): AttributeValue = AttributeValue.builder().n("$value").build()
fun attributeValue(value: Boolean): AttributeValue = AttributeValue.builder().bool(value).build()
fun attributeValue(): AttributeValue = AttributeValue.builder().nul(true).build()

