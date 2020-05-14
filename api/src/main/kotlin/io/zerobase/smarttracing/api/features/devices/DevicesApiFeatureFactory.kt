package io.zerobase.smarttracing.api.features.devices

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.api.features.FeatureFactory
import io.zerobase.smarttracing.api.config.AppConfig

@JsonTypeName("devices")
class DevicesApiFeatureFactory(override val enabled: Boolean): FeatureFactory {
    override fun build(env: Environment, config: AppConfig, injector: Injector) {
        if (enabled || config.enableAllFeatures) {
            env.jersey().register(DevicesResource(injector.getInstance(DevicesDao::class.java)))
            env.jersey().register(UsersResource(injector.getInstance(UsersDao::class.java)))
        }
    }
}
