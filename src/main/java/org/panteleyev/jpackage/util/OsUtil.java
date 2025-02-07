// Copyright © 2020-2025 Petr Panteleyev <petr@panteleyev.org>
// SPDX-License-Identifier: BSD-2-Clause

package org.panteleyev.jpackage.util;

public final class OsUtil {
    private OsUtil() {
    }

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isLinux() {
        return OS.contains("linux");
    }
}
