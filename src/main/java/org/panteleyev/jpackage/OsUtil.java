/*
 Copyright © 2020-2022 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage;

final class OsUtil {
    private OsUtil() {
    }

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
