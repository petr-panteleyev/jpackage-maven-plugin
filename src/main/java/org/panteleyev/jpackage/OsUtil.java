/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.jpackage;

public interface OsUtil {
    String OS = System.getProperty("os.name").toLowerCase();

    static boolean isWindows() {
        return OS.contains("win");
    }

    static boolean isMac() {
        return OS.contains("mac");
    }
}
