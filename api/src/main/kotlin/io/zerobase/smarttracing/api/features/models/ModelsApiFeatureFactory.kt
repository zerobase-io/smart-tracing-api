package io.zerobase.smarttracing.api.features.models

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.common.collect.Multimap
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.api.features.FeatureFactory
import io.zerobase.smarttracing.api.config.AppConfig
import io.zerobase.smarttracing.api.features.models.ModelsResource

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
