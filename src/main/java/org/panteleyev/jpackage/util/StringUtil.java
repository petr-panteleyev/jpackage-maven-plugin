/*
 Copyright © 2020-2025 Petr Panteleyev
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage.util;

import java.util.regex.Matcher;

import static org.panteleyev.jpackage.util.OsUtil.isWindows;

public final class StringUtil {
    private StringUtil() {
    }

    private static final String REPLACER = Matcher.quoteReplacement(isWindows() ? "\\\\\\\"" : "\\\"");
    private static final String SPACE_WRAPPER = isWindows() ? "\\\"" : "\"";

    public static String escape(String arg) {
        arg = arg.replaceAll("\"", REPLACER);
        if (arg.contains(" ")) {
            arg = SPACE_WRAPPER + arg + SPACE_WRAPPER;
        }

        return arg;
    }

    public static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
