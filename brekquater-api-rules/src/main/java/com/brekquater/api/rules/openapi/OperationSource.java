// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.HttpMethod;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;

/**
 * Where an operation was found: its path, its method, and the parameters its path item shares with every
 * operation of that path.
 */
record OperationSource(String path, HttpMethod method, Operation operation, List<Parameter> pathItemParameters) {

    OperationSource {
        pathItemParameters = List.copyOf(pathItemParameters);
    }
}
