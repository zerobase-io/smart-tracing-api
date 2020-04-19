package io.zerobase.smarttracing.features.organizations

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.common.eventbus.EventBus
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.AppConfig
import io.zerobase.smarttracing.features.FeatureFactory

@JsonTypeName("organizations")
class OrganizationsApiFeatureFactory(override val enabled: Boolean) : FeatureFactory {
    override fun build(env: Environment, config: AppConfig, injector: Injector) {
        if (enabled || config.enableAllFeatures) {
            env.jersey().register(OrganizationsResource(
                injector.getInstance(OrganizationsDao::class.java),
                config.siteTypeCategories,
                config.scannableTypes,
                injector.getInstance(EventBus::class.java)
            ))
        }
    }
}
