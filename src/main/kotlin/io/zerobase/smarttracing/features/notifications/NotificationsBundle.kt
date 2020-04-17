package io.zerobase.smarttracing.features.notifications

import io.zerobase.smarttracing.config.AppConfig
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyEnvironment

class NotificationsBundle: GuiceyBundle {
    override fun run(environment: GuiceyEnvironment) {
        val config = environment.configuration<AppConfig>()
        val factory = config.featureFactories.first { it is NotificationsFeatureFactory } ?: return
        if (factory.enabled) {
            environment.modules(NotificaationModule())
        }
    }
}
