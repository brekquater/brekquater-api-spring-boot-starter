// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DOC-013: an {@code x-sunset} extension holds a date, such as {@code 2027-06-30}, or an RFC 3339 date-time.
 * Tools that warn clients about removals, such as oasdiff, read it as one.
 */
public final class SunsetIsDate extends OpenApiRule<ApiOperation> {

    @Override
    public String id() {
        return "BQ-DOC-013";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DOCUMENTATION;
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        return document.operations().stream().filter(operation -> operation.sunset().isPresent()).toList();
    }

    @Override
    protected JsonPointer locate(ApiOperation target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation target, OpenApiContext document) {
        String sunset = target.sunset().orElseThrow().strip();
        if (isDate(sunset)) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Sunset " + sunset + " is not a date.",
                "Write the date as YYYY-MM-DD, for example @ExtensionProperty(name = \"x-sunset\", value = "
                        + "\"2027-06-30\")."));
    }

    private static boolean isDate(String text) {
        try {
            LocalDate.parse(text);
            return true;
        } catch (DateTimeParseException notADate) {
            return isDateTime(text);
        }
    }

    private static boolean isDateTime(String text) {
        try {
            OffsetDateTime.parse(text);
            return true;
        } catch (DateTimeParseException notADateTime) {
            return false;
        }
    }
}
