// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.DocumentSummary;
import com.brekquater.api.rules.HttpMethod;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.ApiParameter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

class SwaggerOpenApiContextTest {

    @Test
    void indexesAnOperationForEachOfTheEightHttpMethods() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    trace: {responses: {'200': {description: OK}}}
                    patch: {responses: {'200': {description: OK}}}
                    head: {responses: {'200': {description: OK}}}
                    options: {responses: {'200': {description: OK}}}
                    delete: {responses: {'200': {description: OK}}}
                    post: {responses: {'200': {description: OK}}}
                    put: {responses: {'200': {description: OK}}}
                    get: {responses: {'200': {description: OK}}}
                """);

        assertThat(context.operations()).extracting(ApiOperation::method).containsExactly(HttpMethod.values());
    }

    @Test
    void ordersOperationsByPathWhateverTheOrderInTheDocument() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders/{id}:
                    get: {responses: {'200': {description: OK}}}
                  /customers:
                    get: {responses: {'200': {description: OK}}}
                  /orders:
                    post: {responses: {'201': {description: Created}}}
                    get: {responses: {'200': {description: OK}}}
                """);

        assertThat(context.operations())
                .extracting(operation -> operation.method() + " " + operation.path())
                .containsExactly("GET /customers", "GET /orders", "POST /orders", "GET /orders/{id}");
    }

    @Test
    void pointsAtAnOperationWithAnEscapedJsonPointer() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders/{id}:
                    get: {responses: {'200': {description: OK}}}
                """);

        assertThat(context.operations())
                .extracting(operation -> operation.pointer().value())
                .containsExactly("/paths/~1orders~1{id}/get");
    }

    @Test
    void exposesWhatAnOperationDeclares() {
        ApiOperation operation = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      summary: List orders
                      description: Returns every order.
                      x-brekquater-ignore: [BQ-DOC-001, BQ-ERR-001]
                      responses:
                        default: {description: Error}
                        '200': {description: OK}
                """).operations().get(0);

        assertThat(operation.operationId()).contains("listOrders");
        assertThat(operation.summary()).contains("List orders");
        assertThat(operation.description()).contains("Returns every order.");
        assertThat(operation.responses()).extracting(ApiResponse::code).containsExactly("200", "default");
        assertThat(operation.ignoredRuleIds()).containsExactly("BQ-DOC-001", "BQ-ERR-001");
    }

    @Test
    void reportsNothingForWhatAnOperationLeavesOut() {
        ApiOperation operation = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get: {}
                """).operations().get(0);

        assertThat(operation.operationId()).isEmpty();
        assertThat(operation.summary()).isEmpty();
        assertThat(operation.description()).isEmpty();
        assertThat(operation.responses()).isEmpty();
        assertThat(operation.ignoredRuleIds()).isEmpty();
    }

    @Test
    void ignoresAnOptOutThatIsNotAList() {
        ApiOperation operation = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      x-brekquater-ignore: BQ-DOC-001
                      responses: {'200': {description: OK}}
                """).operations().get(0);

        assertThat(operation.ignoredRuleIds()).isEmpty();
    }

    @Test
    void summarisesTheDocument() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get: {responses: {'200': {description: OK}}}
                """);

        assertThat(context.document()).isEqualTo(new DocumentSummary("3.1.0", "Orders API", "1.0", 1));
    }

    @Test
    void readsAnOpenApi30Document() {
        OpenApiContext context = contextOf("""
                openapi: 3.0.3
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get: {responses: {'200': {description: OK}}}
                """);

        assertThat(context.document().openApiVersion()).isEqualTo("3.0.3");
        assertThat(context.operations()).hasSize(1);
    }

    @Test
    void treatsADocumentWithoutPathsOrInfoAsEmpty() {
        OpenApiContext context = new SwaggerOpenApiContext(new OpenAPI().openapi(null));

        assertThat(context.operations()).isEmpty();
        assertThat(context.document()).isEqualTo(new DocumentSummary("", "", "", 0));
    }

    @Test
    void readsTheSingleTypeOfAnOpenApi30Schema() {
        ApiOperation operation = contextOf("""
                openapi: 3.0.3
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      responses:
                        '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                """).operations().get(0);

        assertThat(operation.responses().get(0).content().get(0).schema())
                .hasValueSatisfying(schema -> assertThat(schema.types()).containsExactly("array"));
    }

    @Test
    void readsTheSingleTypeOfASchemaBuiltInCode() {
        io.swagger.v3.oas.models.responses.ApiResponse ok = new io.swagger.v3.oas.models.responses.ApiResponse()
                .content(new Content().addMediaType("application/json", new MediaType().schema(new Schema<>().type("array"))));
        Operation operation = new Operation().responses(new ApiResponses().addApiResponse("200", ok));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/orders", new PathItem().get(operation)));

        ApiOperation adapted = new SwaggerOpenApiContext(openApi).operations().get(0);

        assertThat(adapted.responses().get(0).content().get(0).schema())
                .hasValueSatisfying(schema -> assertThat(schema.isArray()).isTrue());
    }

    @Test
    void skipsResponsesAndParametersThatCarryNothing() {
        Operation operation = new Operation()
                .addParametersItem(new Parameter().in("query"))
                .addParametersItem(new Parameter().name("page").in("query"))
                .responses(new ApiResponses().addApiResponse("200", null));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/orders", new PathItem().get(operation)));

        ApiOperation adapted = new SwaggerOpenApiContext(openApi).operations().get(0);

        assertThat(adapted.parameters()).extracting(ApiParameter::name).containsExactly("page");
        assertThat(adapted.responses()).isEmpty();
    }

    @Test
    void skipsAPathThatHasNoPathItem() {
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/orders", null));

        assertThat(new SwaggerOpenApiContext(openApi).operations()).isEmpty();
    }

    private static OpenApiContext contextOf(String yaml) {
        return new SwaggerOpenApiContext(OpenApiFixture.parse(yaml));
    }
}
