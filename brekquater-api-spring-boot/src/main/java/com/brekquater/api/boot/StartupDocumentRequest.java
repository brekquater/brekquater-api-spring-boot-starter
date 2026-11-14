// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A stand-in request for the OpenAPI document, used by strict mode before any real request exists (a JDK dynamic
 * proxy). springdoc needs a request only to work out the server URL it writes into the document, so this one
 * describes {@code http://localhost:80{context-path}} and answers every other question with an empty value: no
 * headers, no parameters, no attributes.
 */
final class StartupDocumentRequest implements InvocationHandler {

    private static final String SCHEME = "http";
    private static final String HOST = "localhost";
    private static final int PORT = 80;

    private final String contextPath;
    private final Map<String, Object> answers;

    private StartupDocumentRequest(String contextPath) {
        this.contextPath = contextPath;
        this.answers = Map.of(
                "getScheme", SCHEME,
                "getServerName", HOST,
                "getServerPort", PORT,
                "getContextPath", contextPath,
                "getServletPath", "",
                "getRequestURI", contextPath,
                "getMethod", "GET",
                "getProtocol", "HTTP/1.1",
                "getLocale", Locale.getDefault());
    }

    static HttpServletRequest create(String contextPath) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                new StartupDocumentRequest(contextPath));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) {
        String name = method.getName();
        if (name.equals("getRequestURL")) {
            // A new buffer each time, because callers may append to it.
            return new StringBuffer(SCHEME + "://" + HOST + ":" + PORT + contextPath);
        }
        return objectMethod(proxy, name, arguments)
                .or(() -> Optional.ofNullable(answers.get(name)))
                .orElseGet(() -> emptyValueOf(method.getReturnType()));
    }

    private static Optional<Object> objectMethod(Object proxy, String name, Object[] arguments) {
        return switch (name) {
            case "equals" -> Optional.of(proxy == arguments[0]);
            case "hashCode" -> Optional.of(System.identityHashCode(proxy));
            case "toString" -> Optional.of("StartupDocumentRequest");
            default -> Optional.empty();
        };
    }

    // Nothing in this request is known, so every other answer is the empty value of its type.
    private static Object emptyValueOf(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (Enumeration.class.isAssignableFrom(type)) {
            return Collections.emptyEnumeration();
        }
        if (Map.class.isAssignableFrom(type)) {
            return Map.of();
        }
        return Collection.class.isAssignableFrom(type) ? List.of() : null;
    }
}
