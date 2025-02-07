// Copyright © 2025 Petr Panteleyev <petr@panteleyev.org>
// SPDX-License-Identifier: BSD-2-Clause

package org.panteleyev.jpackage.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class DirectoryUtil {

    public static boolean isNestedDirectory(Path parent, Path child) {
        Path absoluteParent = parent.toAbsolutePath();
        Path absoluteChild = child.toAbsolutePath();
        return absoluteChild.startsWith(absoluteParent);
    }

    public static void removeDirectory(Path dir) {
        if (!dir.toFile().exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private DirectoryUtil() {
    }
}
