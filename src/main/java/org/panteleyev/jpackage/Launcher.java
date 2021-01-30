/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
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
