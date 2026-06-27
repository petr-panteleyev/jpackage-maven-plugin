/*
 Copyright © 2021 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage;

import org.apache.maven.plugin.MojoFailureException;
import java.io.File;

/**
 * Defines launcher parameters used with {@code --add-launcher}.
 */
public class Launcher {
    private String name;
    private File file;

    String getName() {
        return name;
    }

    /**
     * Sets name of the launcher.
     * @param name launcher name
     */
    public void setName(String name) {
        this.name = name;
    }

    File getFile() {
        return file;
    }

    /**
     * Sets path to a properties file that contains a list of key, value pairs.
     * @param file properties file
     */
    public void setFile(File file) {
        this.file = file;
    }

    void validate() throws MojoFailureException {
        if (name == null || name.isEmpty() || file == null) {
            throw new MojoFailureException("Launcher parameters cannot be null or empty");
        }

        if (!file.exists()) {
            throw new MojoFailureException("Launcher file " + file.getAbsolutePath() + " does not exist");
        }
    }
}
