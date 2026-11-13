// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleRegistry;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.Severity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

class BrekquaterApiAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BrekquaterApiAutoConfiguration.class));

    @Test
    void assessesTheApiWhenSpringdocIsPresent() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(AssessmentService.class)
                .hasSingleBean(RuleRegistry.class)
                .hasSingleBean(OpenApiDocumentSource.class));
    }

    @Test
    void backsOffWithoutSpringdoc() {
        runner.withClassLoader(new FilteredClassLoader(OpenApiWebMvcResource.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(AssessmentService.class));
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("brekquater.api.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(AssessmentService.class));
    }

    @Test
    void backsOffOutsideAServletWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BrekquaterApiAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(AssessmentService.class));
    }

    @Test
    void servesNothingWithoutADeveloperProfile() {
        runner.run(context -> assertThat(context)
                .doesNotHaveBean(AssessmentEndpoint.class)
                .doesNotHaveBean(PanelPageController.class)
                .doesNotHaveBean(SwaggerIndexTransformer.class));
    }

    @Test
    void servesTheStandalonePageWhereverTheAssessmentIsServed() {
        runner.withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).hasSingleBean(PanelPageController.class));
    }

    @Test
    void addsNothingToSwaggerUiWhenSwaggerUiIsNotRunning() {
        runner.withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).doesNotHaveBean(SwaggerIndexTransformer.class));
    }

    @Test
    void servesTheAssessmentWithoutSwaggerUiOnTheClasspath() {
        runner.withClassLoader(new FilteredClassLoader(SwaggerIndexPageTransformer.class))
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(AssessmentEndpoint.class)
                        .hasSingleBean(PanelPageController.class)
                        .doesNotHaveBean(SwaggerIndexTransformer.class));
    }

    @Test
    void keepsTheApplicationsOwnSwaggerUiTransformer() {
        runner.withPropertyValues("spring.profiles.active=dev")
                .withUserConfiguration(OwnSwaggerUiTransformer.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(SwaggerIndexTransformer.class)
                        .getBean(SwaggerIndexTransformer.class)
                        .isSameAs(OwnSwaggerUiTransformer.TRANSFORMER));
    }

    @Test
    void warnsAboutExposureWheneverTheStarterIsActive() {
        runner.run(context -> assertThat(context).hasSingleBean(ExposureWarning.class));
    }

    @Test
    void servesTheAssessmentPubliclyInTheDevProfile() {
        runner.withPropertyValues("spring.profiles.active=dev").run(context -> {
            assertThat(context).hasSingleBean(AssessmentEndpoint.class);
            assertThat(context.getBean(AccessGuard.class).admitsCurrentCaller()).isTrue();
        });
    }

    @Test
    void servesTheAssessmentInTheLocalProfile() {
        runner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasSingleBean(AssessmentEndpoint.class));
    }

    @Test
    void anExplicitModeWinsOverTheProfile() {
        runner.withPropertyValues("spring.profiles.active=dev", "brekquater.api.panel.mode=disabled")
                .run(context -> assertThat(context).doesNotHaveBean(AssessmentEndpoint.class));
        runner.withPropertyValues("spring.profiles.active=prod", "brekquater.api.panel.mode=public")
                .run(context -> assertThat(context).hasSingleBean(AssessmentEndpoint.class));
    }

    @Test
    void theAuthenticatedModeAdmitsOnlyAuthenticatedCallers() {
        runner.withPropertyValues("brekquater.api.panel.mode=authenticated").run(context -> {
            SecurityContextHolder.clearContext();
            assertThat(context.getBean(AccessGuard.class)).isInstanceOf(AuthenticatedCallersOnly.class);
            assertThat(context.getBean(AccessGuard.class).admitsCurrentCaller()).isFalse();
        });
    }

    @Test
    void theAuthenticatedModeWithoutSpringSecurityFailsWithAnExplanation() {
        runner.withClassLoader(new FilteredClassLoader(SecurityContextHolder.class))
                .withPropertyValues("brekquater.api.panel.mode=authenticated")
                .run(context -> assertThat(analysisOf(context))
                        .hasValueSatisfying(analysis -> {
                            assertThat(analysis.getDescription())
                                    .contains("brekquater.api.panel.mode", "'authenticated'");
                            assertThat(analysis.getAction()).contains("spring-boot-starter-security");
                        }));
    }

    @Test
    void anApplicationBeanReplacesTheStartersBean() {
        runner.withUserConfiguration(OwnAssessmentService.class).run(context -> assertThat(context)
                .hasSingleBean(AssessmentService.class)
                .getBean(AssessmentService.class)
                .isSameAs(OwnAssessmentService.SERVICE));
    }

    @Test
    void addsTheApplicationsOwnRulesToTheBuiltInOnes() {
        runner.withUserConfiguration(OwnRule.class).run(context -> assertThat(
                        context.getBean(RuleRegistry.class).ids())
                .contains("BQ-OWN-001", "BQ-DOC-001"));
    }

    @Test
    void leavesOutDisabledRulesAndAppliesSeverityOverrides() {
        runner.withPropertyValues(
                        "brekquater.api.rules.disabled=BQ-DES-001,BQ-NAM-003",
                        "brekquater.api.rules.severity-overrides.BQ-DOC-001=MAJOR")
                .run(context -> {
                    RuleRegistry registry = context.getBean(RuleRegistry.class);
                    assertThat(registry.ids()).doesNotContain("BQ-DES-001", "BQ-NAM-003");
                    assertThat(registry.rules())
                            .filteredOn(rule -> rule.id().equals("BQ-DOC-001"))
                            .extracting(Rule::severity)
                            .containsExactly(Severity.MAJOR);
                });
    }

    @Test
    void anUnknownDisabledRuleIdFailsWithAnExplanation() {
        runner.withPropertyValues("brekquater.api.rules.disabled=BQ-TYPO-001")
                .run(context -> assertThat(analysisOf(context)).hasValueSatisfying(analysis -> {
                    assertThat(analysis.getDescription()).contains("brekquater.api.rules.disabled", "BQ-TYPO-001");
                    assertThat(analysis.getAction()).contains("BQ-DOC-001");
                }));
    }

    @Test
    void raisingANonSecurityRuleToBlockerFailsWithAnExplanation() {
        runner.withPropertyValues("brekquater.api.rules.severity-overrides.BQ-DOC-001=BLOCKER")
                .run(context -> assertThat(analysisOf(context)).hasValueSatisfying(analysis -> assertThat(
                                analysis.getDescription())
                        .contains("brekquater.api.rules.severity-overrides.BQ-DOC-001", "BLOCKER")));
    }

    @Test
    void aNegativeWeightFailsWithAnExplanation() {
        runner.withPropertyValues("brekquater.api.score.weights.minor=-1")
                .run(context -> assertThat(analysisOf(context)).hasValueSatisfying(analysis -> {
                    assertThat(analysis.getDescription()).contains("brekquater.api.score.weights.minor", "'-1'");
                    assertThat(analysis.getAction()).contains("0 or more");
                }));
    }

    @Test
    void gradesOutOfOrderFailWithAnExplanation() {
        runner.withPropertyValues("brekquater.api.score.grades.b=95")
                .run(context -> assertThat(analysisOf(context)).hasValueSatisfying(analysis -> assertThat(
                                analysis.getDescription())
                        .contains("brekquater.api.score.grades", "b=95")));
    }

    @Test
    void anInvalidBasePathFailsWithAnExplanation() {
        runner.withPropertyValues("brekquater.api.panel.base-path=brekquater/api/")
                .run(context -> assertThat(analysisOf(context)).hasValueSatisfying(analysis -> assertThat(
                                analysis.getDescription())
                        .contains("brekquater.api.panel.base-path")));
    }

    private static Optional<FailureAnalysis> analysisOf(AssertableWebApplicationContext context) {
        assertThat(context).hasFailed();
        return Optional.ofNullable(new BrekquaterConfigurationFailureAnalyzer().analyze(context.getStartupFailure()));
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnAssessmentService {

        static final AssessmentService SERVICE = document -> {
            throw new UnsupportedOperationException("not called in this test");
        };

        @Bean
        AssessmentService ownAssessmentService() {
            return SERVICE;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnSwaggerUiTransformer {

        static final SwaggerIndexTransformer TRANSFORMER = (request, resource, chain) -> resource;

        @Bean
        SwaggerIndexTransformer ownSwaggerIndexTransformer() {
            return TRANSFORMER;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnRule {

        @Bean
        Rule ownRule() {
            return new Rule() {
                @Override
                public String id() {
                    return "BQ-OWN-001";
                }

                @Override
                public Severity severity() {
                    return Severity.MINOR;
                }

                @Override
                public Category category() {
                    return Category.DESIGN;
                }

                @Override
                public RuleInput input() {
                    return RuleInput.OPENAPI;
                }

                @Override
                public RuleResult evaluate(AssessmentContext context) {
                    return RuleResult.notApplicable();
                }
            };
        }
    }
}
