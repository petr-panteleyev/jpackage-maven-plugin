/*
 Copyright © 2020-2022 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jpackage;

import java.util.regex.Matcher;
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
        return s != null && s.trim().length() > 0;
    }

    static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
}
