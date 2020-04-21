package io.zerobase.smarttracing.features.notifications

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.AppConfig
import io.zerobase.smarttracing.features.FeatureFactory

@JsonTypeName("notifications")
class NotificationsFeatureFactory(override val enabled: Boolean) : FeatureFactory {
    override fun build(env: Environment, config: AppConfig, injector: Injector) {
        // nothing to do, guice is handling it all
    }
}
