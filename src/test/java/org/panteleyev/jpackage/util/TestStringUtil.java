// Copyright © 2020-2025 Petr Panteleyev <petr@panteleyev.org>
// SPDX-License-Identifier: BSD-2-Clause

package org.panteleyev.jpackage.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
