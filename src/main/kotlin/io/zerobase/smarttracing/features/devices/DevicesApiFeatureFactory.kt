package io.zerobase.smarttracing.features.devices

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.GraphDao
import io.zerobase.smarttracing.config.AppConfig
import io.zerobase.smarttracing.features.FeatureFactory
import io.zerobase.smarttracing.resources.DevicesResource

@JsonTypeName("devices")
class DevicesApiFeatureFactory(override val enabled: Boolean): FeatureFactory {
    override fun build(env: Environment, config: AppConfig, injector: Injector) {
        if (enabled) {
            env.jersey().register(DevicesResource(injector.getInstance(GraphDao::class.java)))
        }
    }
}
