/*
 Copyright © 2020-2025 Petr Panteleyev
 SPDX-License-Identifier: BSD-2-Clause
 */
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

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testEscape(String arg, String expected) {
        assertEquals(expected, escape(arg));
    }
}
