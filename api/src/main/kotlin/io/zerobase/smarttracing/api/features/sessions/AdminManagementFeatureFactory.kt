package io.zerobase.smarttracing.api.features.sessions

import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.inject.Injector
import io.dropwizard.setup.Environment
import io.zerobase.smarttracing.api.config.AppConfig
import io.zerobase.smarttracing.api.features.FeatureFactory
import io.zerobase.smarttracing.api.guice.getInstance

@JsonTypeName("admin")
class AdminManagementFeatureFactory(
    override val enabled: Boolean,
    val config: SessionManagementConfig
) : FeatureFactory {

    override fun build(env: Environment, appConfig: AppConfig, injector: Injector) {
        if (enabled || appConfig.enableAllFeatures) {
            val childInjector = injector.createChildInjector(AdminManagementModule(config))

            env.jersey().register(childInjector.getInstance<SessionManagementResource>())
            env.jersey().register(childInjector.getInstance<OauthResource>())
        }
    }
}
