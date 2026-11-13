// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.MappingMatch;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletMapping;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiDocsRequestTest {

    @Test
    void looksLikeARequestForTheApiDocsOnTheSameServer() {
        MockHttpServletRequest assessmentRequest = new MockHttpServletRequest("GET", "/shop/brekquater/api/assessment");
        assessmentRequest.setServerName("orders.example.com");
        assessmentRequest.setServerPort(8443);
        assessmentRequest.setScheme("https");
        assessmentRequest.setContextPath("/shop");
        assessmentRequest.setQueryString("refresh=1");

        ApiDocsRequest request = new ApiDocsRequest(assessmentRequest, "/v3/api-docs");

        assertThat(request.getRequestURL()).hasToString("https://orders.example.com:8443/shop/v3/api-docs");
        assertThat(request.getRequestURI()).isEqualTo("/shop/v3/api-docs");
        assertThat(request.getServletPath()).isEqualTo("/v3/api-docs");
        assertThat(request.getPathInfo()).isNull();
        assertThat(request.getQueryString()).isNull();
    }

    @Test
    void keepsTheServletPrefixOfADispatcherServletMappedToAPath() {
        MockHttpServletRequest assessmentRequest = new MockHttpServletRequest("GET", "/api/brekquater/api/assessment");
        assessmentRequest.setServletPath("/api");
        assessmentRequest.setHttpServletMapping(new MockHttpServletMapping("brekquater/api/assessment", "/api/*",
                "dispatcherServlet", MappingMatch.PATH));

        ApiDocsRequest request = new ApiDocsRequest(assessmentRequest, "/v3/api-docs");

        assertThat(request.getRequestURL()).hasToString("http://localhost:80/api/v3/api-docs");
    }
}
