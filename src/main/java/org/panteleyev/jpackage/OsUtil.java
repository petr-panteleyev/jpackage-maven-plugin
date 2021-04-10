/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.jpackage;

abstract class OsUtil {
    private static final String OS = System.getProperty("os.name").toLowerCase();

    static boolean isWindows() {
        return OS.contains("win");
    }

    static boolean isMac() {
        return OS.contains("mac");
    }

    static boolean isLinux() {
        return OS.contains("linux");
    }
}
