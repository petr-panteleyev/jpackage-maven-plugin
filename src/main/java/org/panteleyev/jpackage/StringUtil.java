/*
 Copyright © 2020-2024 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage;

import java.util.regex.Matcher;

import static java.lang.Character.isDigit;
import static org.panteleyev.jpackage.OsUtil.isWindows;

final class StringUtil {
    private StringUtil() {
    }

    private static final String REPLACER = Matcher.quoteReplacement(isWindows() ? "\\\\\\\"" : "\\\"");
    private static final String SPACE_WRAPPER = isWindows() ? "\\\"" : "\"";

    static String escape(String arg) {
        arg = arg.replaceAll("\"", REPLACER);
        if (arg.contains(" ")) {
            arg = SPACE_WRAPPER + arg + SPACE_WRAPPER;
        }

        return arg;
    }

    static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    static int parseVersion(String versionString) {
        if (versionString == null) {
            return 0;
        }

        versionString = versionString.trim();
        if (versionString.isEmpty()) {
            return 0;
        }

        int index = 0;
        while (index < versionString.length() && isDigit(versionString.charAt(index))) {
            index++;
        }
        try {
            return Integer.parseInt(versionString.substring(0, index));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
