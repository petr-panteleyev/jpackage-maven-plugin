// Copyright © 2020-2025 Petr Panteleyev <petr@panteleyev.org>
// SPDX-License-Identifier: BSD-2-Clause

package org.panteleyev.jpackage.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.panteleyev.jpackage.util.OsUtil.isWindows;
import static org.panteleyev.jpackage.util.StringUtil.escape;

public class TestStringUtil {

    private static List<Arguments> dataProvider() {
        if (isWindows()) {
            return Arrays.asList(
                    Arguments.of("", ""),
                    Arguments.of("123", "123"),
                    Arguments.of("-DAppOption=text string", "\\\"-DAppOption=text string\\\""),
                    Arguments.of("-XX:OnError=\"userdump.exe %p\"",
                            "\\\"-XX:OnError=\\\\\\\"userdump.exe %p\\\\\\\"\\\"")

            );
        } else {
            return Arrays.asList(
                    Arguments.of("", ""),
                    Arguments.of("123", "123"),
                    Arguments.of("-DAppOption=text string", "\"-DAppOption=text string\""),
                    Arguments.of("-XX:OnError=\"userdump.exe %p\"",
                            "\"-XX:OnError=\\\"userdump.exe %p\\\"\"")
            );
        }
    }

    private static List<Arguments> testParseVersionArguments() {
        return Arrays.asList(
                Arguments.of(null, 0),
                Arguments.of("", 0),
                Arguments.of("  ", 0),
                Arguments.of("22", 22),
                Arguments.of("22.0.1", 22),
                Arguments.of("23-ea", 23),
                Arguments.of("23ea", 23),
                Arguments.of("  23 ", 23),
                Arguments.of("a22", 0)
        );
    }

    private static List<Arguments> validNormalizeCases() {
        return Arrays.asList(
                // 1) 1–4 numeric parts (strip leading zeros)
                Arguments.of("1", "1"),
                Arguments.of("01", "1"),
                Arguments.of("1.2", "1.2"),
                Arguments.of("1.02", "1.2"),
                Arguments.of("1.2.3", "1.2.3"),
                Arguments.of("1.2.03", "1.2.3"),
                Arguments.of("1.2.3.4", "1.2.3.4"),
                // More than 4 parts → keep only the first 4
                Arguments.of("1.2.3.4.5", "1.2.3.4"),

                // 2) Text prefixes (extract numeric sequence only)
                Arguments.of("v1", "1"),
                Arguments.of("release-1.2.3", "1.2.3"),
                Arguments.of("build_2024.07.01", "2024.7.1"),

                // 3) Remove pre-release / build metadata
                Arguments.of("0.1.0-SNAPSHOT", "0.1.0"),
                Arguments.of("1.2.3-SNAPSHOT", "1.2.3"),
                Arguments.of("1.2.3-rc1", "1.2.3"),
                Arguments.of("1.2.3-rc.1", "1.2.3"),
                Arguments.of("1.2.3+build.5", "1.2.3"),
                Arguments.of("1.2.3-rc1+build.5", "1.2.3"),
                Arguments.of("v1.2.3-rc.1+meta", "1.2.3"),

                // 4) Allow large numbers (e.g., dates) and preserve a format
                Arguments.of("2024", "2024"),
                Arguments.of("2024.07", "2024.7"),
                Arguments.of("2024.07.01", "2024.7.1"),
                Arguments.of("1.2.3.20240813", "1.2.3.20240813")
        );
    }

    private static List<Arguments> invalidNormalizeCases() {
        return Arrays.asList(
                Arguments.of((Object) null),
                Arguments.of(""),
                Arguments.of("   "),
                Arguments.of("foo"),
                Arguments.of("v")
        );
    }


    static List<Arguments> validSanitizeForWindowsCases() {
        return  Arrays.asList(
                // 1–3 parts → pad to 3 parts
                Arguments.of("0", "0.0.0"),
                Arguments.of("0.0", "0.0.0"),
                Arguments.of("0.0.0", "0.0.0"),
                Arguments.of("1", "1.0.0"),
                Arguments.of("1.2", "1.2.0"),
                Arguments.of("1.2.3", "1.2.3"),

                // 4th component → drop
                Arguments.of("1.2.3.4", "1.2.3"),

                // Leading zeros → strip
                Arguments.of("01.002.00003", "1.2.3"),
                Arguments.of("10.0.00000", "10.0.0"),

                // Edge of Windows ranges
                Arguments.of("255.255.65535", "255.255.65535"),
                Arguments.of("10.0.65535", "10.0.65535")
        );
        // Note: sanitizeForWindows assumes numeric-only input already.
        // Non-numeric input is tested below as "format errors".
    }

    static List<Arguments> invalidRangeSanitizeForWindowsCases() {
        return Arrays.asList(
                Arguments.of("256.0.0"),     // major > 255
                Arguments.of("1.256.0"),     // minor > 255
                Arguments.of("1.2.65536"),   // build > 65535
                Arguments.of("999.1.1")      // major > 255
        );
    }

    static List<Arguments> invalidFormatCases() {
        return Arrays.asList(
                Arguments.of(""),          // empty → split OK, parse fails
                Arguments.of(" "),         // whitespace
                Arguments.of("a.b.c"),     // non-numeric tokens
                Arguments.of("1..2"),      // missing token in the middle
                Arguments.of("1.2.")       // trailing dot → empty token
        );
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testEscape(String arg, String expected) {
        assertEquals(expected, escape(arg));
    }

    @ParameterizedTest
    @MethodSource("testParseVersionArguments")
    public void testParseVersion(String versionString, int expected) {
        assertEquals(expected, StringUtil.parseVersion(versionString));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @MethodSource("validNormalizeCases")
    void testNormalize(String input, String expected) {
        assertEquals(expected, StringUtil.normalizeVersion(input));
    }

    @ParameterizedTest(name = "[{index}] format error: \"{0}\"")
    @MethodSource("invalidNormalizeCases")
    void testNormalize_format_error(String input) {
        assertThrows(IllegalArgumentException.class, () -> StringUtil.normalizeVersion(input));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @MethodSource("validSanitizeForWindowsCases")
    void sanitize_valid_inputs(String input, String expected) {
        assertEquals(expected, StringUtil.sanitizeForWindows(input));
    }

    @ParameterizedTest(name = "[{index}] range error: \"{0}\"")
    @MethodSource("invalidRangeSanitizeForWindowsCases")
    void sanitize_out_of_range_throws(String input) {
        assertThrows(IllegalArgumentException.class, () -> StringUtil.sanitizeForWindows(input));
    }

    @ParameterizedTest(name = "[{index}] format error: \"{0}\"")
    @MethodSource("invalidFormatCases")
    void sanitize_format_error_throws(String input) {
        assertThrows(NumberFormatException.class, () -> StringUtil.sanitizeForWindows(input));
    }

    @ParameterizedTest
    @NullSource
    void sanitize_null_throws( String input ) {
        assertThrows(NullPointerException.class, () -> StringUtil.sanitizeForWindows(input));
    }

    @Test
    void sanitize_trims_fourth_component_only() {
        assertEquals("1.2.3", StringUtil.sanitizeForWindows("1.2.3.99999"));
    }
}
