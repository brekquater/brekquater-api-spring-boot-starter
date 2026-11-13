// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleEngine;
import com.brekquater.api.rules.RuleRegistry;
import com.brekquater.api.rules.report.ReportWriter;
import com.brekquater.api.rules.scoring.ScoreCalculator;
import java.time.Clock;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Assesses the application's own OpenAPI document and serves the result. Every bean backs off when the application
 * defines its own bean of the same type, and nothing is computed until the assessment is first requested.
 */
// Runs after springdoc's Swagger UI properties, so the panel can tell whether Swagger UI is on, and before
// springdoc's Swagger UI configuration, so that springdoc's own index transformer backs off for ours.
@AutoConfiguration(
        afterName = {
            "org.springdoc.core.properties.SwaggerUiConfigProperties",
            "org.springdoc.core.properties.SwaggerUiOAuthProperties"
        },
        beforeName = "org.springdoc.webmvc.ui.SwaggerConfig")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springdoc.webmvc.api.OpenApiWebMvcResource")
@ConditionalOnBooleanProperty(name = "brekquater.api.enabled", matchIfMissing = true)
@EnableConfigurationProperties(BrekquaterApiProperties.class)
public final class BrekquaterApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RuleRegistry brekquaterRuleRegistry(BrekquaterApiProperties properties, ObjectProvider<Rule> applicationRules) {
        Stream<Rule> builtIn = ServiceLoader.load(Rule.class, getClass().getClassLoader()).stream()
                .map(ServiceLoader.Provider::get);
        List<Rule> all = Stream.concat(builtIn, applicationRules.orderedStream()).toList();
        return RuleSelection.of(new RuleRegistry(all), properties.rules());
    }

    @Bean
    @ConditionalOnMissingBean
    ScoreCalculator brekquaterScoreCalculator(BrekquaterApiProperties properties) {
        return ScoreCalculator.riskWeighted(ScoringPolicies.of(properties.score()));
    }

    @Bean
    @ConditionalOnMissingBean
    ReportWriter brekquaterReportWriter() {
        return ReportWriter.json();
    }

    @Bean
    @ConditionalOnMissingBean
    OpenApiDocumentSource brekquaterDocumentSource(
            ObjectProvider<OpenApiWebMvcResource> resource, ObjectProvider<SpringDocConfigProperties> properties) {
        return new SpringdocDocumentSource(resource, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    AssessmentService brekquaterAssessmentService(RuleRegistry registry, ScoreCalculator calculator) {
        AssessmentService engine = new EngineAssessmentService(new RuleEngine(registry), calculator, Clock.systemUTC());
        return new CachingAssessmentService(engine);
    }

    @Bean
    ExposureWarning brekquaterExposureWarning() {
        return new ExposureWarning();
    }

    /** The parts that show the assessment, registered only when the panel mode lets someone see it. */
    @Configuration(proxyBeanMethods = false)
    @Conditional(OnPanelExposedCondition.class)
    static class PanelConfiguration {

        private static final String SPRING_SECURITY = "org.springframework.security.core.context.SecurityContextHolder";

        @Bean
        @ConditionalOnMissingBean
        AccessGuard brekquaterAccessGuard(Environment environment, ResourceLoader resourceLoader) {
            if (PanelExposure.modeIn(environment) != PanelMode.AUTHENTICATED) {
                return AccessGuard.everyone();
            }
            if (!ClassUtils.isPresent(SPRING_SECURITY, resourceLoader.getClassLoader())) {
                throw new InvalidBrekquaterConfigurationException(
                        PanelExposure.MODE_PROPERTY,
                        "authenticated",
                        "The authenticated mode needs Spring Security. Add spring-boot-starter-security, or set the "
                                + "mode to public or disabled.");
            }
            return new AuthenticatedCallersOnly();
        }

        @Bean
        AssessmentEndpoint brekquaterAssessmentEndpoint(
                AccessGuard guard, OpenApiDocumentSource source, AssessmentService service, ReportWriter writer) {
            return new AssessmentEndpoint(new AssessmentEndpoint.Collaborators(guard, source, service, writer));
        }

        @Bean
        PanelPageController brekquaterPanelPage() {
            return new PanelPageController();
        }

        @Bean
        ApiVersioningExemption brekquaterApiVersioningExemption(
                ObjectProvider<RequestMappingHandlerMapping> mappings, BrekquaterApiProperties properties) {
            return new ApiVersioningExemption(mappings, properties.panel().basePath());
        }
    }

    /** Adds the panel to Swagger UI, when Swagger UI is there to add it to. */
    @Configuration(proxyBeanMethods = false)
    @Conditional(OnPanelExposedCondition.class)
    @ConditionalOnClass(name = "org.springdoc.webmvc.ui.SwaggerIndexPageTransformer")
    @ConditionalOnBean(SwaggerUiConfigProperties.class)
    static class SwaggerUiPanelConfiguration {

        @Bean
        @ConditionalOnMissingBean(SwaggerIndexTransformer.class)
        SwaggerIndexTransformer brekquaterSwaggerIndexTransformer(
                SwaggerUiConfigProperties config,
                SwaggerUiOAuthProperties oAuth,
                SwaggerWelcomeCommon welcome,
                ObjectMapperProvider objectMapperProvider,
                BrekquaterApiProperties properties) {
            return new PanelInjectingIndexTransformer(
                    new PanelInjectingIndexTransformer.SwaggerUiCollaborators(config, oAuth, welcome, objectMapperProvider),
                    properties.panel().basePath());
        }
    }
}
