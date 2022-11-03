/*
 Copyright © 2021 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage;

import org.apache.maven.plugin.MojoFailureException;
import java.io.File;

public class Launcher {
    private String name;
    private File file;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void validate() throws MojoFailureException {
        if (name == null || name.isEmpty() || file == null) {
            throw new MojoFailureException("Launcher parameters cannot be null or empty");
        }

        if (!file.exists()) {
            throw new MojoFailureException("Launcher file " + file.getAbsolutePath() + " does not exist");
        }
    }
}
