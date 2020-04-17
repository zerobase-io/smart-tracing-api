package io.zerobase.smarttracing.features

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.inject.Injector
import io.dropwizard.jackson.Discoverable
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.AppConfig

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
interface FeatureFactory: Discoverable {
    val enabled: Boolean
    fun build(env: Environment, config: AppConfig, injector: Injector)
}
