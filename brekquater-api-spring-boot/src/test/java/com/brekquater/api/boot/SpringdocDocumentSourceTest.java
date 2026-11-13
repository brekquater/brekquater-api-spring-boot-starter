// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;

/** Reading a real springdoc document is covered by the demo's integration test; this covers the absent case. */
class SpringdocDocumentSourceTest {

    @Test
    void findsNoDocumentWhenSpringdocServesNone() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("springDocConfigProperties", new SpringDocConfigProperties());
        SpringdocDocumentSource source = new SpringdocDocumentSource(
                beans.getBeanProvider(OpenApiWebMvcResource.class),
                beans.getBeanProvider(SpringDocConfigProperties.class));

        assertThat(source.fetch(new MockHttpServletRequest())).isEmpty();
    }

    @Test
    void anOpenApiDocumentIsIdentifiedByItsContent() {
        assertThat(OpenApiDocument.of("{}").fingerprint())
                .isEqualTo(OpenApiDocument.of("{}").fingerprint())
                .isNotEqualTo(OpenApiDocument.of("{ }").fingerprint())
                .hasSize(64);
    }
}
