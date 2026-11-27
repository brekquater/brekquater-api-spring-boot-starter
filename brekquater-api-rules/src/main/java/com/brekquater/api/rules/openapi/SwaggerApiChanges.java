// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.ApiChange.Kind;
import com.brekquater.api.rules.ApiChanges;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.openapi.SchemaPair.Direction;
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
 * client. An operation is matched by method and exact path, or else by method and path with variable names
 * ignored, so renaming {@code {id}} to {@code {orderId}} or changing an operationId is not a change. An operation
 * that matches several current ones that way is left alone, because which one replaced it cannot be told.
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
        OperationMatcher matcher = new OperationMatcher(OperationIndex.sources(current).toList());
        List<OperationSource> before = OperationIndex.sources(baseline).toList();
        before.forEach(old -> comparison.matched(old, matcher.match(old)));
        return new ApiChanges(comparison.schemas.changes(), before.size(), version(baseline), version(current));
    }

    private void matched(OperationSource old, Match match) {
        if (match.gone()) {
            removed(old);
        }
        match.operation().ifPresent(now -> compare(old, now));
    }

    private void removed(OperationSource old) {
        if (!Boolean.TRUE.equals(old.operation().getDeprecated())) {
            JsonPointer pointer = pointer(old);
            schemas.add(new ApiChange(Kind.OPERATION_REMOVED, pointer, pointer, old.method().name() + " " + old.path(), ""));
        }
    }

    private void compare(OperationSource old, OperationSource now) {
        JsonPointer operation = pointer(now);
        parameters(old, now, operation);
        requestBody(old.operation().getRequestBody(), now.operation().getRequestBody(), operation);
        responses(old.operation(), now.operation(), operation);
        sunset(old.operation(), now.operation(), operation);
    }

    private void parameters(OperationSource old, OperationSource now, JsonPointer operation) {
        Map<String, Parameter> before = new LinkedHashMap<>();
        parametersOf(old, oldComponents).values().forEach(parameter -> before.putIfAbsent(parameterKey(parameter), parameter));
        parametersOf(now, newComponents).forEach((location, parameter) -> {
            if (!PATH.equals(parameter.getIn())) {
                parameter(Optional.ofNullable(before.get(parameterKey(parameter))), parameter,
                        new SchemaPair(null, parameter.getSchema(), location, subject(parameter), Direction.REQUEST, operation));
            }
        });
    }

    // The pair's pointer is the parameter's location; its schema is compared one level down.
    private void parameter(Optional<Parameter> previous, Parameter parameter, SchemaPair at) {
        if (Boolean.TRUE.equals(parameter.getRequired()) && !previous.map(Parameter::getRequired).orElse(false)) {
            schemas.add(new ApiChange(Kind.PARAMETER_BECAME_REQUIRED, at.operation(), at.pointer(), at.subject(), ""));
        }
        previous.ifPresent(earlier ->
                schemas.compare(at.child(earlier.getSchema(), parameter.getSchema(), at.pointer().append("schema"))));
    }

    private static String subject(Parameter parameter) {
        return parameter.getIn() + " parameter " + parameter.getName();
    }

    // Operation parameters override path item parameters with the same name and location, as in OpenAPI.
    private static Map<JsonPointer, Parameter> parametersOf(OperationSource source, Components components) {
        Map<String, Map.Entry<JsonPointer, Parameter>> byKey = new LinkedHashMap<>();
        byKey.putAll(indexed(source.pathItemParameters(), PATHS.append(source.path()).append("parameters"), components));
        byKey.putAll(indexed(Optional.ofNullable(source.operation().getParameters()).orElse(List.of()),
                pointer(source).append("parameters"), components));
        Map<JsonPointer, Parameter> parameters = new LinkedHashMap<>();
        byKey.values().forEach(entry -> parameters.put(entry.getKey(), entry.getValue()));
        return parameters;
    }

    private static Map<String, Map.Entry<JsonPointer, Parameter>> indexed(
            List<Parameter> declared, JsonPointer owner, Components components) {
        Map<String, Map.Entry<JsonPointer, Parameter>> byKey = new LinkedHashMap<>();
        for (int index = 0; index < declared.size(); index++) {
            Parameter parameter = declared.get(index) == null ? null : components.resolve(declared.get(index));
            if (parameter != null && parameter.getName() != null && parameter.getIn() != null) {
                byKey.put(parameterKey(parameter), Map.entry(owner.append(String.valueOf(index)), parameter));
            }
        }
        return byKey;
    }

    // Header names are case-insensitive in HTTP; other parameter names are not.
    private static String parameterKey(Parameter parameter) {
        String name = HEADER.equals(parameter.getIn()) ? parameter.getName().toLowerCase(Locale.ROOT) : parameter.getName();
        return parameter.getIn() + ":" + name;
    }

    private void requestBody(RequestBody old, RequestBody now, JsonPointer operation) {
        if (now == null) {
            return;
        }
        JsonPointer pointer = operation.append("requestBody");
        if (Boolean.TRUE.equals(now.getRequired()) && !(old != null && Boolean.TRUE.equals(old.getRequired()))) {
            schemas.add(new ApiChange(Kind.REQUEST_BODY_BECAME_REQUIRED, operation, pointer, "request body", ""));
        }
        if (old != null) {
            compareBodies(old.getContent(), now.getContent(),
                    new SchemaPair(null, null, pointer, "request body", Direction.REQUEST, operation));
        }
    }

    private void responses(Operation old, Operation now, JsonPointer operation) {
        Map<String, ApiResponse> after = responsesOf(now, newComponents);
        JsonPointer pointer = operation.append("responses");
        responsesOf(old, oldComponents).forEach((code, response) -> {
            if (!code.startsWith(SUCCESS) || code.equalsIgnoreCase(ANY_SUCCESS)) {
                return;
            }
            if (!after.containsKey(code) && !after.containsKey(ANY_SUCCESS)) {
                schemas.add(new ApiChange(Kind.SUCCESS_STATUS_REMOVED, operation, pointer, "status " + code, ""));
                return;
            }
            Optional.ofNullable(after.get(code)).ifPresent(current -> compareBodies(response.getContent(), current.getContent(),
                    new SchemaPair(null, null, pointer.append(code), "response body", Direction.RESPONSE, operation)));
        });
    }

    // The pair names where the bodies are and which way they travel; its schemas come from the two contents.
    private void compareBodies(Content old, Content now, SchemaPair bodies) {
        jsonBody(old).ifPresent(before -> jsonBody(now).ifPresent(after -> schemas.compare(bodies.child(
                before.getValue(), after.getValue(), bodies.pointer().append("content").append(after.getKey()).append("schema")))));
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

    private void sunset(Operation old, Operation now, JsonPointer operation) {
        Optional<LocalDate> before = sunsetOf(old);
        Optional<LocalDate> after = sunsetOf(now);
        if (before.isPresent() && after.isPresent() && after.get().isBefore(before.get())) {
            schemas.add(new ApiChange(Kind.SUNSET_MOVED_EARLIER, operation, operation, SUNSET_EXTENSION,
                    before.get() + " to " + after.get()));
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

    static JsonPointer pointer(OperationSource source) {
        return PATHS.append(source.path()).append(source.method().pathItemKey());
    }

    private static Optional<String> version(OpenAPI openApi) {
        return Optional.ofNullable(openApi.getInfo()).map(Info::getVersion).filter(Objects::nonNull);
    }

    /**
     * What a baseline operation became.
     *
     * @param operation the current operation it became, if exactly one fits
     * @param gone whether no current operation fits at all
     */
    private record Match(Optional<OperationSource> operation, boolean gone) {}

    /** Finds the current operation that a baseline operation became. */
    private static final class OperationMatcher {

        private final Map<String, OperationSource> exact;
        private final Map<String, List<OperationSource>> renamed;

        OperationMatcher(List<OperationSource> current) {
            this.exact = current.stream().collect(Collectors.toMap(OperationMatcher::exactKey, Function.identity(),
                    (first, second) -> first));
            this.renamed = current.stream().collect(Collectors.groupingBy(OperationMatcher::renamedKey));
        }

        Match match(OperationSource old) {
            Optional<OperationSource> same = Optional.ofNullable(exact.get(exactKey(old)));
            if (same.isPresent()) {
                return new Match(same, false);
            }
            List<OperationSource> candidates = renamed.getOrDefault(renamedKey(old), List.of());
            if (candidates.size() == 1) {
                return new Match(Optional.of(candidates.get(0)), false);
            }
            return new Match(Optional.empty(), candidates.isEmpty());
        }

        private static String exactKey(OperationSource source) {
            return source.method() + " " + source.path();
        }

        private static String renamedKey(OperationSource source) {
            return source.method() + " " + source.path().replaceAll("\\{[^}/]*}", "{}");
        }
    }
}
