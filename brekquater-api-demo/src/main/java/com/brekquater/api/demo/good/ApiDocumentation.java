// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.good;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Describes the API as a whole: its name, purpose, owner and how callers authenticate. The demo documents bearer
 * authentication but does not enforce it, to stay small; a real service would also configure Spring Security.
 */
@Profile("good")
@Configuration(proxyBeanMethods = false)
class ApiDocumentation {

    private static final String BEARER = "bearer";

    @Bean
    OpenAPI ordersApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Orders API")
                        .version("1.0.0")
                        .description("Places, finds and cancels customer orders.")
                        .contact(new Contact().name("Orders team").email("orders@example.com")))
                .components(new Components().addSecuritySchemes(
                        BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme(BEARER)))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
