package io.zerobase.smarttracing.api.features.notifications

import io.zerobase.smarttracing.api.config.AppConfig
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyEnvironment

class NotificationsBundle: GuiceyBundle {
    override fun run(environment: GuiceyEnvironment) {
        val config = environment.configuration<AppConfig>()
        val factory = config.featureFactories.first { it is NotificationsFeatureFactory }
        if (factory.enabled || config.enableAllFeatures) {
            environment.modules(NotificationModule())
        }
    }
}
