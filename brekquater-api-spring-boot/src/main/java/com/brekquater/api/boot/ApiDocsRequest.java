// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.MappingMatch;

/**
 * The current request, presented to springdoc as if it were a request for the OpenAPI document.
 *
 * <p>springdoc works out the server URL it writes into the document by cutting the API docs path off the end of
 * the request URL. A request for the assessment endpoint would give it a wrong URL, so this wrapper reports the
 * URL the document itself is served at, on the same host, context path and servlet path.
 */
final class ApiDocsRequest extends HttpServletRequestWrapper {

    private final String apiDocsPath;

    ApiDocsRequest(HttpServletRequest request, String apiDocsPath) {
        super(request);
        this.apiDocsPath = apiDocsPath;
    }

    @Override
    public StringBuffer getRequestURL() {
        HttpServletRequest request = (HttpServletRequest) getRequest();
        String origin = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return new StringBuffer(origin).append(getRequestURI());
    }

    @Override
    public String getRequestURI() {
        return getContextPath() + getServletPath();
    }

    @Override
    public String getServletPath() {
        return servletPrefix() + apiDocsPath;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    // A DispatcherServlet mapped to a prefix such as /api/* serves the document under that prefix too.
    private String servletPrefix() {
        HttpServletRequest request = (HttpServletRequest) getRequest();
        HttpServletMapping mapping = request.getHttpServletMapping();
        boolean prefixMapped = mapping != null && mapping.getMappingMatch() == MappingMatch.PATH;
        return prefixMapped ? request.getServletPath() : "";
    }
}
