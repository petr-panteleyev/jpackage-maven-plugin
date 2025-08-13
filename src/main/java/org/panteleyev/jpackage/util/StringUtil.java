// Copyright © 2020-2025 Petr Panteleyev <petr@panteleyev.org>
// SPDX-License-Identifier: BSD-2-Clause

package org.panteleyev.jpackage.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isDigit;
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

    public static int parseVersion(String versionString) {
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

    private static final Pattern APPVER = Pattern.compile(
            "^[^0-9]*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:\\.(\\d+))?"
    );

    /**
     * Sets the application version passed to the {@code jpackage} {@code --app-version} option.
     * <p>
     * According to the JDK {@code jpackage} specification, the version string must match:
     * <pre>
     *     [0-9]+(\.[0-9]+){0,3}
     * </pre>
     * That is:
     * <ul>
     *   <li>Consists of 1 to 4 numeric parts separated by dots.</li>
     *   <li>Each component must be a non-negative integer within the range {@code 0} to {@code 65535}.</li>
     *   <li>Qualifiers such as {@code -SNAPSHOT}, {@code -beta}, or other non-numeric characters are not allowed.</li>
     * </ul>
     * <p>
     * On Windows (MSI), the format is based on the
     * <a href="https://learn.microsoft.com/en-us/windows/win32/msi/productversion">Windows Installer ProductVersion</a>
     * rules, where {@code Major.Minor.Build} are the primary fields and an optional 4th field is supported
     * starting with JDK 16.
     *
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html#app-version">
     *      JDK jpackage --app-version</a>
     *
     * @param version the version string to normalize
     * @return a normalized version string in the format {@code Major.Minor.Build} or {@code Major.Minor.Build.Patch}
     *         where {@code Major}, {@code Minor}, {@code Build}, and {@code Patch} are non-negative integers.
     * @throws IllegalArgumentException if the input is invalid, empty, or cannot be parsed into numeric components.
     */
    public static String normalizeVersion(String version) throws IllegalArgumentException {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version string is null or empty");
        }

        Matcher m = APPVER.matcher(version.trim());
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        StringBuilder normalizeVersion = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            String g = m.group(i);
            if (g == null) break;
            normalizeVersion.append(Integer.parseInt(g));
            if (i < 4 && m.group(i + 1) != null) {
                normalizeVersion.append('.');
            }
        }
        return normalizeVersion.toString();
    }

    public static String sanitizeForWindows(String version) {
        String[] p = version.split("\\.", -1);
        if (p.length >= 4) {
            version = String.join(".", p[0], p[1], p[2]);
            p = version.split("\\.");
        }
        int major = Integer.parseInt(p[0]);
        int minor = p.length > 1 ? Integer.parseInt(p[1]) : 0;
        int build = p.length > 2 ? Integer.parseInt(p[2]) : 0;
        if (major < 0 || major > 255) throw new IllegalArgumentException("Major out of range 0..255");
        if (minor < 0 || minor > 255) throw new IllegalArgumentException("Minor out of range 0..255");
        if (build < 0 || build > 65535) throw new IllegalArgumentException("Build out of range 0..65535");
        return String.format("%d.%d.%d", major, minor, build);
    }
}
