// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Optional;

/** Base class for rules that judge every operation on its own, without looking at the rest of the document. */
public abstract class OperationRule extends OpenApiRule<ApiOperation> {

    @Override
    protected final List<ApiOperation> targets(OpenApiContext document) {
        return document.operations();
    }

    @Override
    protected final JsonPointer locate(ApiOperation operation) {
        return operation.pointer();
    }

    @Override
    protected final Optional<Explanation> check(ApiOperation operation, OpenApiContext document) {
        return check(operation);
    }

    /** Returns what is wrong with the operation, or empty when it passes. */
    protected abstract Optional<Explanation> check(ApiOperation operation);
}
