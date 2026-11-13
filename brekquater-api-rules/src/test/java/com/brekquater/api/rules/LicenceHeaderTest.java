// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LicenceHeaderTest {

    private static final List<String> HEADER =
            List.of("// SPDX-License-Identifier: Apache-2.0", "// Copyright Brekquater");

    // Surefire runs each module's tests from that module's directory, so the repository root is one level up.
    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void everyJavaSourceFileInTheRepositoryIsFound() {
        assertThat(javaSourceFiles()).isNotEmpty();
    }

    @Test
    void everyJavaSourceFileStartsWithTheLicenceHeader() {
        assertThat(javaSourceFiles().stream().filter(file -> !startsWithHeader(file)))
                .as("Java source files that do not start with %s", HEADER)
                .isEmpty();
    }

    private static List<Path> javaSourceFiles() {
        try (Stream<Path> files = Files.walk(REPOSITORY_ROOT)) {
            return files.filter(LicenceHeaderTest::isJavaSourceFile).sorted().toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static boolean isJavaSourceFile(Path file) {
        Path relative = REPOSITORY_ROOT.relativize(file);
        return relative.toString().endsWith(".java")
                && relative.getNameCount() > 2
                && relative.getName(1).toString().equals("src");
    }

    private static boolean startsWithHeader(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.limit(HEADER.size()).toList().equals(HEADER);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
