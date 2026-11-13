// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiParameter;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.ApiSchema;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.HttpMethod;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-DES-001: a GET that returns a list accepts paging parameters, so the response cannot grow without limit.
 *
 * <p>"Returns a list" is a heuristic: the path does not end in a path parameter and a success response is an
 * array. An endpoint that already returns a page object, such as Spring Data's {@code Page}, is not an array and
 * is not checked.
 */
public final class CollectionGetIsPaginated extends OpenApiRule<ApiOperation> {

    private static final String PARAMETER_END = "}";

    // Compared without case and without separators, so "page_size", "pageSize" and "page-size" all match.
    private static final Set<String> PAGING_PARAMETERS = Set.of(
            "page", "size", "pagesize", "pagenumber", "perpage", "limit", "offset", "cursor", "after", "before",
            "pageable", "pagetoken");

    private static final Explanation UNBOUNDED = new Explanation(
            "Collection endpoint documents no pagination parameters.",
            "Accept a Pageable parameter annotated with @ParameterObject, or add page and size query parameters.");

    @Override
    public String id() {
        return "BQ-DES-001";
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
    protected List<ApiOperation> targets(OpenApiContext document) {
        return document.operations().stream().filter(CollectionGetIsPaginated::returnsACollection).toList();
    }

    @Override
    protected JsonPointer locate(ApiOperation operation) {
        return operation.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation operation, OpenApiContext document) {
        boolean paginated = operation.parameters().stream()
                .filter(ApiParameter::inQuery)
                .anyMatch(parameter -> PAGING_PARAMETERS.contains(normalised(parameter.name())));
        return paginated ? Optional.empty() : Optional.of(UNBOUNDED);
    }

    private static boolean returnsACollection(ApiOperation operation) {
        return operation.method() == HttpMethod.GET
                && !operation.path().endsWith(PARAMETER_END)
                && operation.responses().stream()
                        .filter(ApiResponse::isSuccess)
                        .flatMap(response -> response.content().stream())
                        .map(ApiMediaType::schema)
                        .flatMap(Optional::stream)
                        .anyMatch(ApiSchema::isArray);
    }

    private static String normalised(String name) {
        return name.replaceAll("[-_]", "").toLowerCase(Locale.ROOT);
    }
}
