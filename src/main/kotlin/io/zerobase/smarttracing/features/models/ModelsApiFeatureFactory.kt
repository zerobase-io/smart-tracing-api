package io.zerobase.smarttracing.features.models

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.config.AppConfig
import io.zerobase.smarttracing.features.FeatureFactory

@JsonTypeName("models")
class ModelsApiFeatureFactory(
    override val enabled: Boolean
) : FeatureFactory {
    override fun build(env: Environment, config: AppConfig, injector: Injector) {
        if (enabled || config.enableAllFeatures) {
            env.jersey().register(ModelsResource(config.siteTypeCategories, config.scannableTypes))
        }
    }
}
