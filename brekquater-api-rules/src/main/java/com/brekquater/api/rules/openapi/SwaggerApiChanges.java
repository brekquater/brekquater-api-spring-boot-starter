// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.ApiChange.Kind;
import com.brekquater.api.rules.ApiChanges;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compares the OpenAPI document of the last release with the current one and lists the changes that can break a
 * client. Operations are matched by method and path, with path variable names ignored, so renaming {@code {id}}
 * to {@code {orderId}} or changing an operationId is not a change.
 */
public final class SwaggerApiChanges {

    private static final JsonPointer PATHS = JsonPointer.root().append("paths");
    private static final String PATH = "path";
    private static final String HEADER = "header";
    private static final String SUCCESS = "2";
    private static final String ANY_SUCCESS = "2XX";
    private static final String JSON = "json";
    private static final String WILDCARD = "/*";
    private static final String SUNSET_EXTENSION = "x-sunset";

    private final Components oldComponents;
    private final Components newComponents;
    private final SchemaChanges schemas;

    private SwaggerApiChanges(OpenAPI baseline, OpenAPI current) {
        this.oldComponents = Components.of(baseline);
        this.newComponents = Components.of(current);
        this.schemas = new SchemaChanges(new LocalRefs(baseline), new LocalRefs(current));
    }

    /** Returns how {@code current} differs from {@code baseline}. */
    public static ApiChanges between(OpenAPI baseline, OpenAPI current) {
        SwaggerApiChanges comparison = new SwaggerApiChanges(baseline, current);
        Map<String, OperationSource> now = OperationIndex.sources(current)
                .collect(Collectors.toMap(SwaggerApiChanges::key, Function.identity(), (first, second) -> first));
        List<OperationSource> before = OperationIndex.sources(baseline).toList();
        before.forEach(old -> Optional.ofNullable(now.get(key(old)))
                .ifPresentOrElse(found -> comparison.compare(old, found), () -> comparison.removed(old)));
        return new ApiChanges(comparison.schemas.changes(), before.size(), version(baseline), version(current));
    }

    private void removed(OperationSource old) {
        if (!Boolean.TRUE.equals(old.operation().getDeprecated())) {
            schemas.add(Kind.OPERATION_REMOVED, pointer(old), old.method().name() + " " + old.path(), "");
        }
    }

    private void compare(OperationSource old, OperationSource now) {
        JsonPointer pointer = pointer(now);
        parameters(old, now, pointer);
        requestBody(old.operation().getRequestBody(), now.operation().getRequestBody(), pointer.append("requestBody"));
        responses(old.operation(), now.operation(), pointer.append("responses"));
        sunset(old.operation(), now.operation(), pointer);
    }

    private void parameters(OperationSource old, OperationSource now, JsonPointer pointer) {
        Map<String, Parameter> before = parametersOf(old, oldComponents, pointer).values().stream()
                .collect(Collectors.toMap(SwaggerApiChanges::parameterKey, Function.identity(), (first, second) -> first));
        parametersOf(now, newComponents, pointer).forEach((location, parameter) -> {
            if (PATH.equals(parameter.getIn())) {
                return;
            }
            Optional<Parameter> previous = Optional.ofNullable(before.get(parameterKey(parameter)));
            String subject = parameter.getIn() + " parameter " + parameter.getName();
            if (Boolean.TRUE.equals(parameter.getRequired()) && !previous.map(Parameter::getRequired).orElse(false)) {
                schemas.add(Kind.PARAMETER_BECAME_REQUIRED, location, subject, "");
            }
            previous.ifPresent(earlier ->
                    schemas.compareRequest(earlier.getSchema(), parameter.getSchema(), location.append("schema"), subject));
        });
    }

    // Operation parameters override path item parameters with the same name and location, as in OpenAPI.
    private static Map<JsonPointer, Parameter> parametersOf(OperationSource source, Components components, JsonPointer op) {
        Map<String, Map.Entry<JsonPointer, Parameter>> byKey = new LinkedHashMap<>();
        JsonPointer item = PATHS.append(source.path()).append("parameters");
        collect(source.pathItemParameters(), item, components, byKey);
        collect(Optional.ofNullable(source.operation().getParameters()).orElse(List.of()), op.append("parameters"),
                components, byKey);
        Map<JsonPointer, Parameter> parameters = new LinkedHashMap<>();
        byKey.values().forEach(entry -> parameters.put(entry.getKey(), entry.getValue()));
        return parameters;
    }

    private static void collect(List<Parameter> declared, JsonPointer owner, Components components,
            Map<String, Map.Entry<JsonPointer, Parameter>> byKey) {
        for (int index = 0; index < declared.size(); index++) {
            Parameter parameter = declared.get(index) == null ? null : components.resolve(declared.get(index));
            if (parameter != null && parameter.getName() != null && parameter.getIn() != null) {
                byKey.put(parameterKey(parameter), Map.entry(owner.append(String.valueOf(index)), parameter));
            }
        }
    }

    // Header names are case-insensitive in HTTP; other parameter names are not.
    private static String parameterKey(Parameter parameter) {
        String name = HEADER.equals(parameter.getIn()) ? parameter.getName().toLowerCase(Locale.ROOT) : parameter.getName();
        return parameter.getIn() + ":" + name;
    }

    private void requestBody(RequestBody old, RequestBody now, JsonPointer pointer) {
        if (now == null) {
            return;
        }
        boolean wasRequired = old != null && Boolean.TRUE.equals(old.getRequired());
        if (Boolean.TRUE.equals(now.getRequired()) && !wasRequired) {
            schemas.add(Kind.REQUEST_BODY_BECAME_REQUIRED, pointer, "request body", "");
        }
        if (old != null) {
            jsonBody(old.getContent()).ifPresent(before -> jsonBody(now.getContent()).ifPresent(after -> schemas
                    .compareRequest(before.getValue(), after.getValue(), schemaPointer(pointer, after), "request body")));
        }
    }

    private void responses(Operation old, Operation now, JsonPointer pointer) {
        Map<String, ApiResponse> before = responsesOf(old, oldComponents);
        Map<String, ApiResponse> after = responsesOf(now, newComponents);
        before.forEach((code, response) -> {
            if (!code.startsWith(SUCCESS) || code.equalsIgnoreCase(ANY_SUCCESS)) {
                return;
            }
            if (!after.containsKey(code) && !after.containsKey(ANY_SUCCESS)) {
                schemas.add(Kind.SUCCESS_STATUS_REMOVED, pointer, "status " + code, "");
                return;
            }
            Optional.ofNullable(after.get(code)).ifPresent(current -> compareResponse(response, current, pointer.append(code)));
        });
    }

    private void compareResponse(ApiResponse old, ApiResponse now, JsonPointer pointer) {
        jsonBody(old.getContent()).ifPresent(before -> jsonBody(now.getContent()).ifPresent(after -> schemas
                .compareResponse(before.getValue(), after.getValue(), schemaPointer(pointer, after), "response body")));
    }

    private static Map<String, ApiResponse> responsesOf(Operation operation, Components components) {
        Map<String, ApiResponse> responses = new TreeMap<>();
        Optional.ofNullable(operation.getResponses()).ifPresent(declared -> declared.forEach((code, response) -> {
            if (response != null) {
                responses.put(code, components.resolve(response));
            }
        }));
        return responses;
    }

    // The schema a JSON client sends or reads: the first JSON media type by name, or else a wildcard such as */*,
    // which springdoc writes by default and Spring answers with JSON.
    private static Optional<Map.Entry<String, Schema<?>>> jsonBody(Content content) {
        Map<String, MediaType> mediaTypes = new TreeMap<>(Optional.<Map<String, MediaType>>ofNullable(content).orElse(Map.of()));
        return mediaTypes.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getSchema() != null)
                .filter(entry -> isJson(entry.getKey()) || entry.getKey().endsWith(WILDCARD))
                .sorted(Comparator.comparing(entry -> !isJson(entry.getKey())))
                .<Map.Entry<String, Schema<?>>>map(entry -> Map.entry(entry.getKey(), entry.getValue().getSchema()))
                .findFirst();
    }

    private static boolean isJson(String mediaType) {
        return mediaType.toLowerCase(Locale.ROOT).contains(JSON);
    }

    private static JsonPointer schemaPointer(JsonPointer owner, Map.Entry<String, Schema<?>> body) {
        return owner.append("content").append(body.getKey()).append("schema");
    }

    private void sunset(Operation old, Operation now, JsonPointer pointer) {
        Optional<LocalDate> before = sunsetOf(old);
        Optional<LocalDate> after = sunsetOf(now);
        if (before.isPresent() && after.isPresent() && after.get().isBefore(before.get())) {
            schemas.add(Kind.SUNSET_MOVED_EARLIER, pointer, SUNSET_EXTENSION, before.get() + " to " + after.get());
        }
    }

    private static Optional<LocalDate> sunsetOf(Operation operation) {
        Object value = Optional.ofNullable(operation.getExtensions()).orElse(Map.of()).get(SUNSET_EXTENSION);
        return Optional.ofNullable(value).map(String::valueOf).map(String::strip).flatMap(SwaggerApiChanges::date);
    }

    private static Optional<LocalDate> date(String text) {
        return Stream.<Function<String, LocalDate>>of(LocalDate::parse, raw -> OffsetDateTime.parse(raw).toLocalDate())
                .map(parser -> parse(parser, text))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<LocalDate> parse(Function<String, LocalDate> parser, String text) {
        try {
            return Optional.of(parser.apply(text));
        } catch (DateTimeParseException notThisFormat) {
            return Optional.empty();
        }
    }

    private static String key(OperationSource source) {
        return source.method() + " " + source.path().replaceAll("\\{[^}/]*}", "{}");
    }

    private static JsonPointer pointer(OperationSource source) {
        return PATHS.append(source.path()).append(source.method().pathItemKey());
    }

    private static Optional<String> version(OpenAPI openApi) {
        return Optional.ofNullable(openApi.getInfo()).map(Info::getVersion).filter(Objects::nonNull);
    }
}
