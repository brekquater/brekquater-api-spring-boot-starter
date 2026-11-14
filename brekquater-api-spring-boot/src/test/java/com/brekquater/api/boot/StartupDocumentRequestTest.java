// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class StartupDocumentRequestTest {

    private final HttpServletRequest request = StartupDocumentRequest.create("/shop");

    @Test
    void describesAGetOnLocalhostUnderTheContextPath() {
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getRequestURL()).hasToString("http://localhost:80/shop");
        assertThat(request.getContextPath()).isEqualTo("/shop");
        assertThat(request.getServletPath()).isEmpty();
    }

    @Test
    void handsOutANewUrlBufferEachTime() {
        request.getRequestURL().append("/changed");

        assertThat(request.getRequestURL()).hasToString("http://localhost:80/shop");
    }

    @Test
    void hasNoHeadersParametersOrAttributes() {
        assertThat(Collections.list(request.getHeaderNames())).isEmpty();
        assertThat(Collections.list(request.getHeaders("Host"))).isEmpty();
        assertThat(request.getHeader("Host")).isNull();
        assertThat(request.getParameterMap()).isEmpty();
        assertThat(request.getAttribute("anything")).isNull();
        assertThat(request.getContentLength()).isZero();
        assertThat(request.isSecure()).isFalse();
    }

    @Test
    void behavesAsAnOrdinaryObject() {
        assertThat(request.equals(request)).isTrue();
        assertThat(request).isNotEqualTo(StartupDocumentRequest.create("/shop"));
        assertThat(request.hashCode()).isEqualTo(System.identityHashCode(request));
        assertThat(request).hasToString("StartupDocumentRequest");
    }
}
