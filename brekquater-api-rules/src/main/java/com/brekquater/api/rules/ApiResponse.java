// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One documented response of an operation.
 *
 * @param code the key under {@code responses}: a status code such as {@code 404}, a range such as {@code 4XX}, or
 *     {@code default}
 * @param pointer the location of the response in the document
 * @param content the media types the response can be sent as, ordered by name; empty when it has no body
 */
public record ApiResponse(String code, JsonPointer pointer, List<ApiMediaType> content) {

    // A 4xx or 5xx status code, a 4XX or 5XX range, or the catch-all "default" response.
    private static final Pattern ERROR = Pattern.compile("[45](\\d\\d|[xX]{2})|default");
    private static final Pattern SUCCESS = Pattern.compile("2(\\d\\d|[xX]{2})");

    public ApiResponse {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pointer, "pointer");
        content = List.copyOf(content);
    }

    /** Returns whether this response describes a failure, which is also what {@code default} is used for. */
    public boolean isError() {
        return ERROR.matcher(code).matches();
    }

    public boolean isSuccess() {
        return SUCCESS.matcher(code).matches();
    }
}
