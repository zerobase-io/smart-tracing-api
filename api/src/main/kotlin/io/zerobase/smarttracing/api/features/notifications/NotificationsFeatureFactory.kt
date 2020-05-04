package io.zerobase.smarttracing.api.features.notifications

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.api.features.FeatureFactory
import io.zerobase.smarttracing.api.config.AppConfig

@JsonTypeName("notifications")
class NotificationsFeatureFactory(override val enabled: Boolean) : FeatureFactory {
    override fun build(env: Environment, config: AppConfig, injector: Injector) {
        // nothing to do, guice is handling it all
    }
}
