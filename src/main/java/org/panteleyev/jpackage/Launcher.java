/*
 Copyright © 2021-2025 Petr Panteleyev
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage;

import org.apache.maven.api.plugin.MojoException;

import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {
    private String name;
    private Path file;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getFile() {
        return file;
    }

    public void setFile(Path file) {
        this.file = file;
    }

    public void validate() {
        if (name == null || name.isEmpty() || file == null) {
            throw new MojoException("Launcher parameters cannot be null or empty");
        }

        if (!Files.exists(file)) {
            throw new MojoException("Launcher file " + file.toAbsolutePath() + " does not exist");
        }
    }
}
