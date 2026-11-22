// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.ApiOAuthFlow;
import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.DocumentSummary;
import com.brekquater.api.rules.HttpMethod;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.ApiParameter;
import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.ApiSecurityScheme;
import com.brekquater.api.rules.ApiServer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Test
    void readsTheMediaTypesOfARequestBody() {
        ApiOperation operation = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders/{id}:
                    patch:
                      requestBody: {content: {application/merge-patch+json: {schema: {type: object}}, application/json: {}}}
                      responses: {'200': {description: OK}}
                """).operations().get(0);

        assertThat(operation.requestContent())
                .extracting(ApiMediaType::name, mediaType -> mediaType.pointer().value())
                .containsExactly(
                        tuple("application/json", "/paths/~1orders~1{id}/patch/requestBody/content/application~1json"),
                        tuple(
                                "application/merge-patch+json",
                                "/paths/~1orders~1{id}/patch/requestBody/content/application~1merge-patch+json"));
    }

    @Test
    void listsEverySchemaWithItsLocationAndPatternWithoutFollowingReferences() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders/{id}:
                    parameters:
                      - {name: id, in: path, required: true, schema: {type: string, pattern: '^[0-9]+$'}}
                    get:
                      responses:
                        '200': {description: OK, content: {application/json: {schema: {$ref: '#/components/schemas/Order'}}}}
                components:
                  schemas:
                    Order:
                      type: object
                      properties:
                        lines: {type: array, items: {$ref: '#/components/schemas/Line'}}
                      additionalProperties: {type: string}
                    Line:
                      allOf: [{type: object}]
                """);

        assertThat(context.schemas())
                .extracting(schema -> schema.pointer().value(), schema -> schema.pattern().orElse(""))
                .containsExactly(
                        tuple("/components/schemas/Line", ""),
                        tuple("/components/schemas/Line/allOf/0", ""),
                        tuple("/components/schemas/Order", ""),
                        tuple("/components/schemas/Order/properties/lines", ""),
                        tuple("/components/schemas/Order/properties/lines/items", ""),
                        tuple("/components/schemas/Order/additionalProperties", ""),
                        tuple("/paths/~1orders~1{id}/parameters/0/schema", "^[0-9]+$"),
                        tuple("/paths/~1orders~1{id}/get/responses/200/content/application~1json/schema", ""));
    }

    @Test
    void namesAPropertyOnlyWhenTheSchemaSitsUnderProperties() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths: {}
                components:
                  schemas:
                    properties:
                      type: object
                      properties:
                        items: {type: array, items: {type: string}}
                """);

        assertThat(context.schemas())
                .extracting(schema -> schema.propertyName().orElse("-"))
                .containsExactly("-", "items", "-");
    }

    @Test
    void listsTheFlowsOfOAuthSchemesOnly() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths: {}
                components:
                  securitySchemes:
                    bearer: {type: http, scheme: bearer}
                    login:
                      type: oauth2
                      flows:
                        password: {tokenUrl: 'https://auth.example.com/token', scopes: {}}
                        authorizationCode: {authorizationUrl: 'https://auth.example.com/authorize', tokenUrl: 'https://auth.example.com/token', scopes: {}}
                """);

        assertThat(context.oauthFlows())
                .extracting(ApiOAuthFlow::type, flow -> flow.pointer().value())
                .containsExactly(
                        tuple("authorizationCode", "/components/securitySchemes/login/flows/authorizationCode"),
                        tuple("password", "/components/securitySchemes/login/flows/password"));
    }

    @Test
    void readsSecuritySchemesTheirFlowsAndTheScopesEachRequirementAsksFor() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                security: [{login: [orders:read]}, {key: []}]
                paths: {}
                components:
                  securitySchemes:
                    key: {type: apiKey, in: query, name: api_key}
                    login:
                      type: oauth2
                      flows:
                        clientCredentials: {tokenUrl: 'https://sso.example.com/token', refreshUrl: 'https://sso.example.com/refresh', scopes: {orders:read: Read orders}}
                    basic: {type: http, scheme: Basic}
                """);

        assertThat(context.securitySchemes())
                .extracting(ApiSecurityScheme::name, ApiSecurityScheme::type, ApiSecurityScheme::httpScheme, ApiSecurityScheme::apiKeyLocation)
                .containsExactly(
                        tuple("basic", "http", Optional.of("Basic"), Optional.empty()),
                        tuple("key", "apiKey", Optional.empty(), Optional.of("query")),
                        tuple("login", "oauth2", Optional.empty(), Optional.empty()));
        assertThat(context.oauthFlows()).singleElement().satisfies(flow -> {
            assertThat(flow.scheme()).isEqualTo("login");
            assertThat(flow.scopes()).containsExactly("orders:read");
            assertThat(flow.urls()).containsExactly("https://sso.example.com/token", "https://sso.example.com/refresh");
        });
        assertThat(context.rootSecurity())
                .extracting(ApiSecurityRequirement::scopes)
                .containsExactly(Map.of("login", List.of("orders:read")), Map.of("key", List.of()));
    }

    @Test
    void readsTheServersInDocumentOrder() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                servers:
                  - {url: 'http://localhost:8080', description: Generated server url}
                  - {url: 'https://api.example.com'}
                paths: {}
                """);

        assertThat(context.servers())
                .extracting(ApiServer::url, ApiServer::generatedBySpringdoc, server -> server.pointer().value())
                .containsExactly(
                        tuple("http://localhost:8080", true, "/servers/0"),
                        tuple("https://api.example.com", false, "/servers/1"));
    }

    @Test
    void recordsWhereASchemaRefersAndWhetherItIsWriteOnly() {
        OpenApiContext context = contextOf("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths: {}
                components:
                  schemas:
                    Account:
                      type: object
                      properties:
                        owner: {$ref: '#/components/schemas/Person'}
                        password: {type: string, writeOnly: true}
                    Person: {type: object}
                """);

        assertThat(context.schemas())
                .extracting(schema -> schema.pointer().value(), ApiSchemaNode::referencedSchemaName, ApiSchemaNode::writeOnly)
                .containsExactly(
                        tuple("/components/schemas/Account", Optional.empty(), false),
                        tuple("/components/schemas/Account/properties/owner", Optional.of("Person"), false),
                        tuple("/components/schemas/Account/properties/password", Optional.empty(), true),
                        tuple("/components/schemas/Person", Optional.empty(), false));
    }

    private static OpenApiContext contextOf(String yaml) {
        return new SwaggerOpenApiContext(OpenApiFixture.parse(yaml));
    }
}
