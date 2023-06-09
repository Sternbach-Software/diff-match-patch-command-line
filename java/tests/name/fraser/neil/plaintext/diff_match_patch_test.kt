/*
 * Diff Match and Patch -- Test harness
 * Copyright 2018 The diff-match-patch Authors.
 * https://github.com/google/diff-match-patch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Compile from diff-match-patch/java with:
 * javac -d classes src/name/fraser/neil/plaintext/diff_match_patch.java tests/name/fraser/neil/plaintext/diff_match_patch_test.java
 * Execute with:
 * java -classpath classes name/fraser/neil/plaintext/diff_match_patch_test
 */
package name.fraser.neil.plaintext

import name.fraser.neil.plaintext.diff_match_patch
import name.fraser.neil.plaintext.diff_match_patch_test
import name.fraser.neil.plaintext.diff_match_patch.LinesToCharsResult
import java.lang.StringBuilder
import java.util.LinkedList
import java.lang.IllegalArgumentException
import java.util.HashMap
import java.util.Arrays
import kotlin.Throws
import java.io.IOException
import kotlin.jvm.JvmStatic
import java.io.File
import java.lang.Error
import java.util.ArrayList

object diff_match_patch_test {
    private var dmp: diff_match_patch? = null
    private val DELETE = diff_match_patch.Operation.DELETE
    private val EQUAL = diff_match_patch.Operation.EQUAL
    private val INSERT = diff_match_patch.Operation.INSERT

    //  DIFF TEST FUNCTIONS
    fun testDiffCommonPrefix() {
        // Detect any common prefix.
        assertEquals("diff_commonPrefix: Null case.", 0, dmp!!.diff_commonPrefix("abc", "xyz"))
        assertEquals("diff_commonPrefix: Non-null case.", 4, dmp!!.diff_commonPrefix("1234abcdef", "1234xyz"))
        assertEquals("diff_commonPrefix: Whole case.", 4, dmp!!.diff_commonPrefix("1234", "1234xyz"))
    }

    fun testDiffCommonSuffix() {
        // Detect any common suffix.
        assertEquals("diff_commonSuffix: Null case.", 0, dmp!!.diff_commonSuffix("abc", "xyz"))
        assertEquals("diff_commonSuffix: Non-null case.", 4, dmp!!.diff_commonSuffix("abcdef1234", "xyz1234"))
        assertEquals("diff_commonSuffix: Whole case.", 4, dmp!!.diff_commonSuffix("1234", "xyz1234"))
    }

    fun testDiffCommonOverlap() {
        // Detect any suffix/prefix overlap.
        assertEquals("diff_commonOverlap: Null case.", 0, dmp!!.diff_commonOverlap("", "abcd"))
        assertEquals("diff_commonOverlap: Whole case.", 3, dmp!!.diff_commonOverlap("abc", "abcd"))
        assertEquals("diff_commonOverlap: No overlap.", 0, dmp!!.diff_commonOverlap("123456", "abcd"))
        assertEquals("diff_commonOverlap: Overlap.", 3, dmp!!.diff_commonOverlap("123456xxx", "xxxabcd"))

        // Some overly clever languages (C#) may treat ligatures as equal to their
        // component letters.  E.g. U+FB01 == 'fi'
        assertEquals("diff_commonOverlap: Unicode.", 0, dmp!!.diff_commonOverlap("fi", "\ufb01i"))
    }

    fun testDiffHalfmatch() {
        // Detect a halfmatch.
        dmp!!.Diff_Timeout = 1f
        assertNull("diff_halfMatch: No match #1.", dmp!!.diff_halfMatch("1234567890", "abcdef"))
        assertNull("diff_halfMatch: No match #2.", dmp!!.diff_halfMatch("12345", "23"))
        assertArrayEquals(
            "diff_halfMatch: Single Match #1.",
            arrayOf<String>("12", "90", "a", "z", "345678"),
            dmp!!.diff_halfMatch("1234567890", "a345678z")
        )
        assertArrayEquals(
            "diff_halfMatch: Single Match #2.",
            arrayOf<String>("a", "z", "12", "90", "345678"),
            dmp!!.diff_halfMatch("a345678z", "1234567890")
        )
        assertArrayEquals(
            "diff_halfMatch: Single Match #3.",
            arrayOf<String>("abc", "z", "1234", "0", "56789"),
            dmp!!.diff_halfMatch("abc56789z", "1234567890")
        )
        assertArrayEquals(
            "diff_halfMatch: Single Match #4.",
            arrayOf<String>("a", "xyz", "1", "7890", "23456"),
            dmp!!.diff_halfMatch("a23456xyz", "1234567890")
        )
        assertArrayEquals(
            "diff_halfMatch: Multiple Matches #1.",
            arrayOf<String>("12123", "123121", "a", "z", "1234123451234"),
            dmp!!.diff_halfMatch("121231234123451234123121", "a1234123451234z")
        )
        assertArrayEquals(
            "diff_halfMatch: Multiple Matches #2.",
            arrayOf<String>("", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-="),
            dmp!!.diff_halfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-=")
        )
        assertArrayEquals(
            "diff_halfMatch: Multiple Matches #3.",
            arrayOf<String>("-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y"),
            dmp!!.diff_halfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy")
        )

        // Optimal diff would be -q+x=H-i+e=lloHe+Hu=llo-Hew+y not -qHillo+x=HelloHe-w+Hulloy
        assertArrayEquals(
            "diff_halfMatch: Non-optimal halfmatch.",
            arrayOf<String>("qHillo", "w", "x", "Hulloy", "HelloHe"),
            dmp!!.diff_halfMatch("qHilloHelloHew", "xHelloHeHulloy")
        )
        dmp!!.Diff_Timeout = 0f
        assertNull("diff_halfMatch: Optimal no halfmatch.", dmp!!.diff_halfMatch("qHilloHelloHew", "xHelloHeHulloy"))
    }

    fun testDiffLinesToChars() {
        // Convert lines down to characters.
        val tmpVector = ArrayList<String>()
        tmpVector.add("")
        tmpVector.add("alpha\n")
        tmpVector.add("beta\n")
        assertLinesToCharsResultEquals(
            "diff_linesToChars: Shared lines.",
            LinesToCharsResult("\u0001\u0002\u0001", "\u0002\u0001\u0002", tmpVector),
            dmp!!.diff_linesToChars("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n")
        )
        tmpVector.clear()
        tmpVector.add("")
        tmpVector.add("alpha\r\n")
        tmpVector.add("beta\r\n")
        tmpVector.add("\r\n")
        assertLinesToCharsResultEquals(
            "diff_linesToChars: Empty string and blank lines.",
            LinesToCharsResult("", "\u0001\u0002\u0003\u0003", tmpVector),
            dmp!!.diff_linesToChars("", "alpha\r\nbeta\r\n\r\n\r\n")
        )
        tmpVector.clear()
        tmpVector.add("")
        tmpVector.add("a")
        tmpVector.add("b")
        assertLinesToCharsResultEquals(
            "diff_linesToChars: No linebreaks.",
            LinesToCharsResult("\u0001", "\u0002", tmpVector),
            dmp!!.diff_linesToChars("a", "b")
        )

        // More than 256 to reveal any 8-bit limitations.
        val n = 300
        tmpVector.clear()
        val lineList = StringBuilder()
        val charList = StringBuilder()
        for (i in 1 until n + 1) {
            tmpVector.add(
                """
    $i
    
    """.trimIndent()
            )
            lineList.append(
                """
    $i
    
    """.trimIndent()
            )
            charList.append(i.toChar().toString())
        }
        assertEquals("Test initialization fail #1.", n, tmpVector.size)
        val lines = lineList.toString()
        val chars = charList.toString()
        assertEquals("Test initialization fail #2.", n, chars.length)
        tmpVector.add(0, "")
        assertLinesToCharsResultEquals(
            "diff_linesToChars: More than 256.",
            LinesToCharsResult(chars, "", tmpVector),
            dmp!!.diff_linesToChars(lines, "")
        )
    }

    fun testDiffCharsToLines() {
        // First check that Diff equality works.
        assertTrue(
            "diff_charsToLines: Equality #1.",
            diff_match_patch.Diff(EQUAL, "a") == diff_match_patch.Diff(EQUAL, "a")
        )
        assertEquals(
            "diff_charsToLines: Equality #2.",
            diff_match_patch.Diff(EQUAL, "a"),
            diff_match_patch.Diff(EQUAL, "a")
        )

        // Convert chars up to lines.
        var diffs = diffList(
            diff_match_patch.Diff(EQUAL, "\u0001\u0002\u0001"),
            diff_match_patch.Diff(INSERT, "\u0002\u0001\u0002")
        )
        val tmpVector = ArrayList<String>()
        tmpVector.add("")
        tmpVector.add("alpha\n")
        tmpVector.add("beta\n")
        dmp!!.diff_charsToLines(diffs, tmpVector)
        assertEquals(
            "diff_charsToLines: Shared lines.", diffList(
                diff_match_patch.Diff(EQUAL, "alpha\nbeta\nalpha\n"), diff_match_patch.Diff(
                    INSERT, "beta\nalpha\nbeta\n"
                )
            ), diffs
        )

        // More than 256 to reveal any 8-bit limitations.
        val n = 300
        tmpVector.clear()
        var lineList = StringBuilder()
        val charList = StringBuilder()
        for (i in 1 until n + 1) {
            tmpVector.add(
                """
    $i
    
    """.trimIndent()
            )
            lineList.append(
                """
    $i
    
    """.trimIndent()
            )
            charList.append(i.toChar().toString())
        }
        assertEquals("Test initialization fail #3.", n, tmpVector.size)
        val lines = lineList.toString()
        var chars = charList.toString()
        assertEquals("Test initialization fail #4.", n, chars.length)
        tmpVector.add(0, "")
        diffs = diffList(diff_match_patch.Diff(DELETE, chars))
        dmp!!.diff_charsToLines(diffs, tmpVector)
        assertEquals("diff_charsToLines: More than 256.", diffList(diff_match_patch.Diff(DELETE, lines)), diffs)

        // More than 65536 to verify any 16-bit limitation.
        lineList = StringBuilder()
        for (i in 0..65999) {
            lineList.append(
                """
    $i
    
    """.trimIndent()
            )
        }
        chars = lineList.toString()
        val results = dmp!!.diff_linesToChars(chars, "")
        diffs = diffList(diff_match_patch.Diff(INSERT, results.chars1))
        dmp!!.diff_charsToLines(diffs, results.lineArray)
        assertEquals("diff_charsToLines: More than 65536.", chars, diffs.first.text)
    }

    fun testDiffCleanupMerge() {
        // Cleanup a messy diff.
        var diffs = diffList()
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals("diff_cleanupMerge: Null case.", diffList(), diffs)
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(DELETE, "b"), diff_match_patch.Diff(
                INSERT, "c"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: No change case.", diffList(
                diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(
                    DELETE, "b"
                ), diff_match_patch.Diff(INSERT, "c")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(EQUAL, "b"), diff_match_patch.Diff(
                EQUAL, "c"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals("diff_cleanupMerge: Merge equalities.", diffList(diff_match_patch.Diff(EQUAL, "abc")), diffs)
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(DELETE, "b"), diff_match_patch.Diff(
                DELETE, "c"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals("diff_cleanupMerge: Merge deletions.", diffList(diff_match_patch.Diff(DELETE, "abc")), diffs)
        diffs = diffList(
            diff_match_patch.Diff(INSERT, "a"), diff_match_patch.Diff(INSERT, "b"), diff_match_patch.Diff(
                INSERT, "c"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals("diff_cleanupMerge: Merge insertions.", diffList(diff_match_patch.Diff(INSERT, "abc")), diffs)
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(INSERT, "b"), diff_match_patch.Diff(
                DELETE, "c"
            ), diff_match_patch.Diff(INSERT, "d"), diff_match_patch.Diff(EQUAL, "e"), diff_match_patch.Diff(
                EQUAL, "f"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Merge interweave.", diffList(
                diff_match_patch.Diff(DELETE, "ac"), diff_match_patch.Diff(
                    INSERT, "bd"
                ), diff_match_patch.Diff(EQUAL, "ef")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(INSERT, "abc"), diff_match_patch.Diff(
                DELETE, "dc"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Prefix and suffix detection.", diffList(
                diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(
                    DELETE, "d"
                ), diff_match_patch.Diff(INSERT, "b"), diff_match_patch.Diff(EQUAL, "c")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "x"), diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(
                INSERT, "abc"
            ), diff_match_patch.Diff(DELETE, "dc"), diff_match_patch.Diff(EQUAL, "y")
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Prefix and suffix detection with equalities.", diffList(
                diff_match_patch.Diff(
                    EQUAL, "xa"
                ), diff_match_patch.Diff(DELETE, "d"), diff_match_patch.Diff(INSERT, "b"), diff_match_patch.Diff(
                    EQUAL, "cy"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(INSERT, "ba"), diff_match_patch.Diff(
                EQUAL, "c"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Slide edit left.", diffList(
                diff_match_patch.Diff(INSERT, "ab"), diff_match_patch.Diff(
                    EQUAL, "ac"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "c"), diff_match_patch.Diff(INSERT, "ab"), diff_match_patch.Diff(
                EQUAL, "a"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Slide edit right.", diffList(
                diff_match_patch.Diff(EQUAL, "ca"), diff_match_patch.Diff(
                    INSERT, "ba"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(DELETE, "b"), diff_match_patch.Diff(
                EQUAL, "c"
            ), diff_match_patch.Diff(DELETE, "ac"), diff_match_patch.Diff(EQUAL, "x")
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Slide edit left recursive.", diffList(
                diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(
                    EQUAL, "acx"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "x"), diff_match_patch.Diff(DELETE, "ca"), diff_match_patch.Diff(
                EQUAL, "c"
            ), diff_match_patch.Diff(DELETE, "b"), diff_match_patch.Diff(EQUAL, "a")
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Slide edit right recursive.", diffList(
                diff_match_patch.Diff(EQUAL, "xca"), diff_match_patch.Diff(
                    DELETE, "cba"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "b"), diff_match_patch.Diff(INSERT, "ab"), diff_match_patch.Diff(
                EQUAL, "c"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Empty merge.", diffList(
                diff_match_patch.Diff(INSERT, "a"), diff_match_patch.Diff(
                    EQUAL, "bc"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, ""), diff_match_patch.Diff(INSERT, "a"), diff_match_patch.Diff(
                EQUAL, "b"
            )
        )
        dmp!!.diff_cleanupMerge(diffs)
        assertEquals(
            "diff_cleanupMerge: Empty equality.", diffList(
                diff_match_patch.Diff(INSERT, "a"), diff_match_patch.Diff(
                    EQUAL, "b"
                )
            ), diffs
        )
    }

    fun testDiffCleanupSemanticLossless() {
        // Slide diffs to match logical boundaries.
        var diffs = diffList()
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals("diff_cleanupSemanticLossless: Null case.", diffList(), diffs)
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "AAA\r\n\r\nBBB"),
            diff_match_patch.Diff(INSERT, "\r\nDDD\r\n\r\nBBB"),
            diff_match_patch.Diff(
                EQUAL, "\r\nEEE"
            )
        )
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals(
            "diff_cleanupSemanticLossless: Blank lines.", diffList(
                diff_match_patch.Diff(EQUAL, "AAA\r\n\r\n"), diff_match_patch.Diff(
                    INSERT, "BBB\r\nDDD\r\n\r\n"
                ), diff_match_patch.Diff(EQUAL, "BBB\r\nEEE")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "AAA\r\nBBB"),
            diff_match_patch.Diff(INSERT, " DDD\r\nBBB"),
            diff_match_patch.Diff(
                EQUAL, " EEE"
            )
        )
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals(
            "diff_cleanupSemanticLossless: Line boundaries.", diffList(
                diff_match_patch.Diff(EQUAL, "AAA\r\n"), diff_match_patch.Diff(
                    INSERT, "BBB DDD\r\n"
                ), diff_match_patch.Diff(EQUAL, "BBB EEE")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "The c"), diff_match_patch.Diff(INSERT, "ow and the c"), diff_match_patch.Diff(
                EQUAL, "at."
            )
        )
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals(
            "diff_cleanupSemanticLossless: Word boundaries.", diffList(
                diff_match_patch.Diff(EQUAL, "The "), diff_match_patch.Diff(
                    INSERT, "cow and the "
                ), diff_match_patch.Diff(EQUAL, "cat.")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "The-c"), diff_match_patch.Diff(INSERT, "ow-and-the-c"), diff_match_patch.Diff(
                EQUAL, "at."
            )
        )
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals(
            "diff_cleanupSemanticLossless: Alphanumeric boundaries.", diffList(
                diff_match_patch.Diff(EQUAL, "The-"), diff_match_patch.Diff(
                    INSERT, "cow-and-the-"
                ), diff_match_patch.Diff(EQUAL, "cat.")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(
                EQUAL, "ax"
            )
        )
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals(
            "diff_cleanupSemanticLossless: Hitting the start.", diffList(
                diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(
                    EQUAL, "aax"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "xa"), diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(
                EQUAL, "a"
            )
        )
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals(
            "diff_cleanupSemanticLossless: Hitting the end.", diffList(
                diff_match_patch.Diff(EQUAL, "xaa"), diff_match_patch.Diff(
                    DELETE, "a"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "The xxx. The "),
            diff_match_patch.Diff(INSERT, "zzz. The "),
            diff_match_patch.Diff(
                EQUAL, "yyy."
            )
        )
        dmp!!.diff_cleanupSemanticLossless(diffs)
        assertEquals(
            "diff_cleanupSemanticLossless: Sentence boundaries.", diffList(
                diff_match_patch.Diff(EQUAL, "The xxx."), diff_match_patch.Diff(
                    INSERT, " The zzz."
                ), diff_match_patch.Diff(EQUAL, " The yyy.")
            ), diffs
        )
    }

    fun testDiffCleanupSemantic() {
        // Cleanup semantically trivial equalities.
        var diffs = diffList()
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals("diff_cleanupSemantic: Null case.", diffList(), diffs)
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(INSERT, "cd"), diff_match_patch.Diff(
                EQUAL, "12"
            ), diff_match_patch.Diff(DELETE, "e")
        )
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: No elimination #1.", diffList(
                diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(
                    INSERT, "cd"
                ), diff_match_patch.Diff(EQUAL, "12"), diff_match_patch.Diff(DELETE, "e")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(INSERT, "ABC"), diff_match_patch.Diff(
                EQUAL, "1234"
            ), diff_match_patch.Diff(DELETE, "wxyz")
        )
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: No elimination #2.", diffList(
                diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(
                    INSERT, "ABC"
                ), diff_match_patch.Diff(EQUAL, "1234"), diff_match_patch.Diff(DELETE, "wxyz")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(EQUAL, "b"), diff_match_patch.Diff(
                DELETE, "c"
            )
        )
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: Simple elimination.", diffList(
                diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(
                    INSERT, "b"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(EQUAL, "cd"), diff_match_patch.Diff(
                DELETE, "e"
            ), diff_match_patch.Diff(EQUAL, "f"), diff_match_patch.Diff(INSERT, "g")
        )
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: Backpass elimination.", diffList(
                diff_match_patch.Diff(DELETE, "abcdef"), diff_match_patch.Diff(
                    INSERT, "cdfg"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(INSERT, "1"), diff_match_patch.Diff(EQUAL, "A"), diff_match_patch.Diff(
                DELETE, "B"
            ), diff_match_patch.Diff(INSERT, "2"), diff_match_patch.Diff(EQUAL, "_"), diff_match_patch.Diff(
                INSERT, "1"
            ), diff_match_patch.Diff(EQUAL, "A"), diff_match_patch.Diff(DELETE, "B"), diff_match_patch.Diff(
                INSERT, "2"
            )
        )
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: Multiple elimination.", diffList(
                diff_match_patch.Diff(DELETE, "AB_AB"), diff_match_patch.Diff(
                    INSERT, "1A2_1A2"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "The c"), diff_match_patch.Diff(DELETE, "ow and the c"), diff_match_patch.Diff(
                EQUAL, "at."
            )
        )
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: Word boundaries.", diffList(
                diff_match_patch.Diff(EQUAL, "The "), diff_match_patch.Diff(
                    DELETE, "cow and the "
                ), diff_match_patch.Diff(EQUAL, "cat.")
            ), diffs
        )
        diffs = diffList(diff_match_patch.Diff(DELETE, "abcxx"), diff_match_patch.Diff(INSERT, "xxdef"))
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: No overlap elimination.", diffList(
                diff_match_patch.Diff(DELETE, "abcxx"), diff_match_patch.Diff(
                    INSERT, "xxdef"
                )
            ), diffs
        )
        diffs = diffList(diff_match_patch.Diff(DELETE, "abcxxx"), diff_match_patch.Diff(INSERT, "xxxdef"))
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: Overlap elimination.", diffList(
                diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(
                    EQUAL, "xxx"
                ), diff_match_patch.Diff(INSERT, "def")
            ), diffs
        )
        diffs = diffList(diff_match_patch.Diff(DELETE, "xxxabc"), diff_match_patch.Diff(INSERT, "defxxx"))
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: Reverse overlap elimination.", diffList(
                diff_match_patch.Diff(INSERT, "def"), diff_match_patch.Diff(
                    EQUAL, "xxx"
                ), diff_match_patch.Diff(DELETE, "abc")
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "abcd1212"),
            diff_match_patch.Diff(INSERT, "1212efghi"),
            diff_match_patch.Diff(
                EQUAL, "----"
            ),
            diff_match_patch.Diff(DELETE, "A3"),
            diff_match_patch.Diff(INSERT, "3BC")
        )
        dmp!!.diff_cleanupSemantic(diffs)
        assertEquals(
            "diff_cleanupSemantic: Two overlap eliminations.", diffList(
                diff_match_patch.Diff(DELETE, "abcd"), diff_match_patch.Diff(
                    EQUAL, "1212"
                ), diff_match_patch.Diff(INSERT, "efghi"), diff_match_patch.Diff(EQUAL, "----"), diff_match_patch.Diff(
                    DELETE, "A"
                ), diff_match_patch.Diff(EQUAL, "3"), diff_match_patch.Diff(INSERT, "BC")
            ), diffs
        )
    }

    fun testDiffCleanupEfficiency() {
        // Cleanup operationally trivial equalities.
        dmp!!.Diff_EditCost = 4
        var diffs = diffList()
        dmp!!.diff_cleanupEfficiency(diffs)
        assertEquals("diff_cleanupEfficiency: Null case.", diffList(), diffs)
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(INSERT, "12"), diff_match_patch.Diff(
                EQUAL, "wxyz"
            ), diff_match_patch.Diff(DELETE, "cd"), diff_match_patch.Diff(INSERT, "34")
        )
        dmp!!.diff_cleanupEfficiency(diffs)
        assertEquals(
            "diff_cleanupEfficiency: No elimination.", diffList(
                diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(
                    INSERT, "12"
                ), diff_match_patch.Diff(EQUAL, "wxyz"), diff_match_patch.Diff(DELETE, "cd"), diff_match_patch.Diff(
                    INSERT, "34"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(INSERT, "12"), diff_match_patch.Diff(
                EQUAL, "xyz"
            ), diff_match_patch.Diff(DELETE, "cd"), diff_match_patch.Diff(INSERT, "34")
        )
        dmp!!.diff_cleanupEfficiency(diffs)
        assertEquals(
            "diff_cleanupEfficiency: Four-edit elimination.", diffList(
                diff_match_patch.Diff(DELETE, "abxyzcd"), diff_match_patch.Diff(
                    INSERT, "12xyz34"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(INSERT, "12"), diff_match_patch.Diff(EQUAL, "x"), diff_match_patch.Diff(
                DELETE, "cd"
            ), diff_match_patch.Diff(INSERT, "34")
        )
        dmp!!.diff_cleanupEfficiency(diffs)
        assertEquals(
            "diff_cleanupEfficiency: Three-edit elimination.", diffList(
                diff_match_patch.Diff(DELETE, "xcd"), diff_match_patch.Diff(
                    INSERT, "12x34"
                )
            ), diffs
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(INSERT, "12"), diff_match_patch.Diff(
                EQUAL, "xy"
            ), diff_match_patch.Diff(INSERT, "34"), diff_match_patch.Diff(EQUAL, "z"), diff_match_patch.Diff(
                DELETE, "cd"
            ), diff_match_patch.Diff(INSERT, "56")
        )
        dmp!!.diff_cleanupEfficiency(diffs)
        assertEquals(
            "diff_cleanupEfficiency: Backpass elimination.", diffList(
                diff_match_patch.Diff(DELETE, "abxyzcd"), diff_match_patch.Diff(
                    INSERT, "12xy34z56"
                )
            ), diffs
        )
        dmp!!.Diff_EditCost = 5
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "ab"), diff_match_patch.Diff(INSERT, "12"), diff_match_patch.Diff(
                EQUAL, "wxyz"
            ), diff_match_patch.Diff(DELETE, "cd"), diff_match_patch.Diff(INSERT, "34")
        )
        dmp!!.diff_cleanupEfficiency(diffs)
        assertEquals(
            "diff_cleanupEfficiency: High cost elimination.", diffList(
                diff_match_patch.Diff(DELETE, "abwxyzcd"), diff_match_patch.Diff(
                    INSERT, "12wxyz34"
                )
            ), diffs
        )
        dmp!!.Diff_EditCost = 4
    }

    fun testDiffPrettyHtml() {
        // Pretty print.
        val diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a\n"), diff_match_patch.Diff(DELETE, "<B>b</B>"), diff_match_patch.Diff(
                INSERT, "c&d"
            )
        )
        assertEquals(
            "diff_prettyHtml:",
            "<span>a&para;<br></span><del style=\"background:#ffe6e6;\">&lt;B&gt;b&lt;/B&gt;</del><ins style=\"background:#e6ffe6;\">c&amp;d</ins>",
            dmp!!.diff_prettyHtml(diffs)
        )
    }

    fun testDiffText() {
        // Compute the source and destination texts.
        val diffs = diffList(
            diff_match_patch.Diff(EQUAL, "jump"), diff_match_patch.Diff(DELETE, "s"), diff_match_patch.Diff(
                INSERT, "ed"
            ), diff_match_patch.Diff(EQUAL, " over "), diff_match_patch.Diff(DELETE, "the"), diff_match_patch.Diff(
                INSERT, "a"
            ), diff_match_patch.Diff(EQUAL, " lazy")
        )
        assertEquals("diff_text1:", "jumps over the lazy", dmp!!.diff_text1(diffs))
        assertEquals("diff_text2:", "jumped over a lazy", dmp!!.diff_text2(diffs))
    }

    fun testDiffDelta() {
        // Convert a diff into delta string.
        var diffs = diffList(
            diff_match_patch.Diff(EQUAL, "jump"), diff_match_patch.Diff(DELETE, "s"), diff_match_patch.Diff(
                INSERT, "ed"
            ), diff_match_patch.Diff(EQUAL, " over "), diff_match_patch.Diff(DELETE, "the"), diff_match_patch.Diff(
                INSERT, "a"
            ), diff_match_patch.Diff(EQUAL, " lazy"), diff_match_patch.Diff(INSERT, "old dog")
        )
        var text1 = dmp!!.diff_text1(diffs)
        assertEquals("diff_text1: Base text.", "jumps over the lazy", text1)
        var delta = dmp!!.diff_toDelta(diffs)
        assertEquals("diff_toDelta:", "=4\t-1\t+ed\t=6\t-3\t+a\t=5\t+old dog", delta)

        // Convert delta string into a diff.
        assertEquals("diff_fromDelta: Normal.", diffs, dmp!!.diff_fromDelta(text1, delta))

        // Generates error (19 < 20).
        try {
            dmp!!.diff_fromDelta(text1 + "x", delta)
            fail("diff_fromDelta: Too long.")
        } catch (ex: IllegalArgumentException) {
            // Exception expected.
        }

        // Generates error (19 > 18).
        try {
            dmp!!.diff_fromDelta(text1.substring(1), delta)
            fail("diff_fromDelta: Too short.")
        } catch (ex: IllegalArgumentException) {
            // Exception expected.
        }

        // Generates error (%c3%xy invalid Unicode).
        try {
            dmp!!.diff_fromDelta("", "+%c3%xy")
            fail("diff_fromDelta: Invalid character.")
        } catch (ex: IllegalArgumentException) {
            // Exception expected.
        }

        // Test deltas with special characters.
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "\u0680 \u0000 \t %"),
            diff_match_patch.Diff(DELETE, "\u0681 \u0001 \n ^"),
            diff_match_patch.Diff(
                INSERT, "\u0682 \u0002 \\ |"
            )
        )
        text1 = dmp!!.diff_text1(diffs)
        assertEquals("diff_text1: Unicode text.", "\u0680 \u0000 \t %\u0681 \u0001 \n ^", text1)
        delta = dmp!!.diff_toDelta(diffs)
        assertEquals("diff_toDelta: Unicode.", "=7\t-7\t+%DA%82 %02 %5C %7C", delta)
        assertEquals("diff_fromDelta: Unicode.", diffs, dmp!!.diff_fromDelta(text1, delta))

        // Verify pool of unchanged characters.
        diffs = diffList(diff_match_patch.Diff(INSERT, "A-Z a-z 0-9 - _ . ! ~ * ' ( ) ; / ? : @ & = + $ , # "))
        val text2 = dmp!!.diff_text2(diffs)
        assertEquals(
            "diff_text2: Unchanged characters.",
            "A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ",
            text2
        )
        delta = dmp!!.diff_toDelta(diffs)
        assertEquals(
            "diff_toDelta: Unchanged characters.",
            "+A-Z a-z 0-9 - _ . ! ~ * \' ( ) ; / ? : @ & = + $ , # ",
            delta
        )

        // Convert delta string into a diff.
        assertEquals("diff_fromDelta: Unchanged characters.", diffs, dmp!!.diff_fromDelta("", delta))

        // 160 kb string.
        var a = "abcdefghij"
        for (i in 0..13) {
            a += a
        }
        diffs = diffList(diff_match_patch.Diff(INSERT, a))
        delta = dmp!!.diff_toDelta(diffs)
        assertEquals("diff_toDelta: 160kb string.", "+$a", delta)

        // Convert delta string into a diff.
        assertEquals("diff_fromDelta: 160kb string.", diffs, dmp!!.diff_fromDelta("", delta))
    }

    fun testDiffXIndex() {
        // Translate a location in text1 to text2.
        var diffs = diffList(
            diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(INSERT, "1234"), diff_match_patch.Diff(
                EQUAL, "xyz"
            )
        )
        assertEquals("diff_xIndex: Translation on equality.", 5, dmp!!.diff_xIndex(diffs, 2))
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(DELETE, "1234"), diff_match_patch.Diff(
                EQUAL, "xyz"
            )
        )
        assertEquals("diff_xIndex: Translation on deletion.", 1, dmp!!.diff_xIndex(diffs, 3))
    }

    fun testDiffLevenshtein() {
        var diffs = diffList(
            diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(INSERT, "1234"), diff_match_patch.Diff(
                EQUAL, "xyz"
            )
        )
        assertEquals("diff_levenshtein: Levenshtein with trailing equality.", 4, dmp!!.diff_levenshtein(diffs))
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "xyz"), diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(
                INSERT, "1234"
            )
        )
        assertEquals("diff_levenshtein: Levenshtein with leading equality.", 4, dmp!!.diff_levenshtein(diffs))
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "abc"), diff_match_patch.Diff(EQUAL, "xyz"), diff_match_patch.Diff(
                INSERT, "1234"
            )
        )
        assertEquals("diff_levenshtein: Levenshtein with middle equality.", 7, dmp!!.diff_levenshtein(diffs))
    }

    fun testDiffBisect() {
        // Normal.
        val a = "cat"
        val b = "map"
        // Since the resulting diff hasn't been normalized, it would be ok if
        // the insertion and deletion pairs are swapped.
        // If the order changes, tweak this test as required.
        var diffs = diffList(
            diff_match_patch.Diff(DELETE, "c"), diff_match_patch.Diff(INSERT, "m"), diff_match_patch.Diff(
                EQUAL, "a"
            ), diff_match_patch.Diff(DELETE, "t"), diff_match_patch.Diff(INSERT, "p")
        )
        assertEquals("diff_bisect: Normal.", diffs, dmp!!.diff_bisect(a, b, Long.MAX_VALUE))

        // Timeout.
        diffs = diffList(diff_match_patch.Diff(DELETE, "cat"), diff_match_patch.Diff(INSERT, "map"))
        assertEquals("diff_bisect: Timeout.", diffs, dmp!!.diff_bisect(a, b, 0))
    }

    fun testDiffMain() {
        // Perform a trivial diff.
        var diffs = diffList()
        assertEquals("diff_main: Null case.", diffs, dmp!!.diff_main("", "", false))
        diffs = diffList(diff_match_patch.Diff(EQUAL, "abc"))
        assertEquals("diff_main: Equality.", diffs, dmp!!.diff_main("abc", "abc", false))
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "ab"), diff_match_patch.Diff(INSERT, "123"), diff_match_patch.Diff(
                EQUAL, "c"
            )
        )
        assertEquals("diff_main: Simple insertion.", diffs, dmp!!.diff_main("abc", "ab123c", false))
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(DELETE, "123"), diff_match_patch.Diff(
                EQUAL, "bc"
            )
        )
        assertEquals("diff_main: Simple deletion.", diffs, dmp!!.diff_main("a123bc", "abc", false))
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(INSERT, "123"), diff_match_patch.Diff(
                EQUAL, "b"
            ), diff_match_patch.Diff(INSERT, "456"), diff_match_patch.Diff(EQUAL, "c")
        )
        assertEquals("diff_main: Two insertions.", diffs, dmp!!.diff_main("abc", "a123b456c", false))
        diffs = diffList(
            diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(DELETE, "123"), diff_match_patch.Diff(
                EQUAL, "b"
            ), diff_match_patch.Diff(DELETE, "456"), diff_match_patch.Diff(EQUAL, "c")
        )
        assertEquals("diff_main: Two deletions.", diffs, dmp!!.diff_main("a123b456c", "abc", false))

        // Perform a real diff.
        // Switch off the timeout.
        dmp!!.Diff_Timeout = 0f
        diffs = diffList(diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(INSERT, "b"))
        assertEquals("diff_main: Simple case #1.", diffs, dmp!!.diff_main("a", "b", false))
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "Apple"), diff_match_patch.Diff(INSERT, "Banana"), diff_match_patch.Diff(
                EQUAL, "s are a"
            ), diff_match_patch.Diff(INSERT, "lso"), diff_match_patch.Diff(EQUAL, " fruit.")
        )
        assertEquals(
            "diff_main: Simple case #2.",
            diffs,
            dmp!!.diff_main("Apples are a fruit.", "Bananas are also fruit.", false)
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "a"), diff_match_patch.Diff(INSERT, "\u0680"), diff_match_patch.Diff(
                EQUAL, "x"
            ), diff_match_patch.Diff(DELETE, "\t"), diff_match_patch.Diff(INSERT, "\u0000")
        )
        assertEquals("diff_main: Simple case #3.", diffs, dmp!!.diff_main("ax\t", "\u0680x\u0000", false))
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "1"), diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(
                DELETE, "y"
            ), diff_match_patch.Diff(EQUAL, "b"), diff_match_patch.Diff(DELETE, "2"), diff_match_patch.Diff(
                INSERT, "xab"
            )
        )
        assertEquals("diff_main: Overlap #1.", diffs, dmp!!.diff_main("1ayb2", "abxab", false))
        diffs = diffList(
            diff_match_patch.Diff(INSERT, "xaxcx"), diff_match_patch.Diff(EQUAL, "abc"), diff_match_patch.Diff(
                DELETE, "y"
            )
        )
        assertEquals("diff_main: Overlap #2.", diffs, dmp!!.diff_main("abcy", "xaxcxabc", false))
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "ABCD"),
            diff_match_patch.Diff(EQUAL, "a"),
            diff_match_patch.Diff(
                DELETE, "="
            ),
            diff_match_patch.Diff(INSERT, "-"),
            diff_match_patch.Diff(EQUAL, "bcd"),
            diff_match_patch.Diff(
                DELETE, "="
            ),
            diff_match_patch.Diff(INSERT, "-"),
            diff_match_patch.Diff(EQUAL, "efghijklmnopqrs"),
            diff_match_patch.Diff(
                DELETE, "EFGHIJKLMNOefg"
            )
        )
        assertEquals(
            "diff_main: Overlap #3.",
            diffs,
            dmp!!.diff_main("ABCDa=bcd=efghijklmnopqrsEFGHIJKLMNOefg", "a-bcd-efghijklmnopqrs", false)
        )
        diffs = diffList(
            diff_match_patch.Diff(INSERT, " "), diff_match_patch.Diff(EQUAL, "a"), diff_match_patch.Diff(
                INSERT, "nd"
            ), diff_match_patch.Diff(EQUAL, " [[Pennsylvania]]"), diff_match_patch.Diff(DELETE, " and [[New")
        )
        assertEquals(
            "diff_main: Large equality.",
            diffs,
            dmp!!.diff_main("a [[Pennsylvania]] and [[New", " and [[Pennsylvania]]", false)
        )
        dmp!!.Diff_Timeout = 0.1f // 100ms
        var a: String? =
            "`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.\n"
        var b: String? =
            "I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.\n"
        // Increase the text lengths by 1024 times to ensure a timeout.
        for (i in 0..9) {
            a += a
            b += b
        }
        val startTime = System.currentTimeMillis()
        dmp!!.diff_main(a, b)
        val endTime = System.currentTimeMillis()
        // Test that we took at least the timeout period.
        assertTrue("diff_main: Timeout min.", dmp!!.Diff_Timeout * 1000 <= endTime - startTime)
        // Test that we didn't take forever (be forgiving).
        // Theoretically this test could fail very occasionally if the
        // OS task swaps or locks up for a second at the wrong moment.
        assertTrue("diff_main: Timeout max.", dmp!!.Diff_Timeout * 1000 * 2 > endTime - startTime)
        dmp!!.Diff_Timeout = 0f

        // Test the linemode speedup.
        // Must be long to pass the 100 char cutoff.
        a =
            "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n"
        b =
            "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n"
        assertEquals("diff_main: Simple line-mode.", dmp!!.diff_main(a, b, true), dmp!!.diff_main(a, b, false))
        a =
            "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
        b =
            "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij"
        assertEquals("diff_main: Single line-mode.", dmp!!.diff_main(a, b, true), dmp!!.diff_main(a, b, false))
        a =
            "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n"
        b =
            "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n"
        val texts_linemode = diff_rebuildtexts(
            dmp!!.diff_main(a, b, true)
        )
        val texts_textmode = diff_rebuildtexts(
            dmp!!.diff_main(a, b, false)
        )
        assertArrayEquals("diff_main: Overlap line-mode.", texts_textmode, texts_linemode)

        // Test null inputs.
        try {
            dmp!!.diff_main(null, null)
            fail("diff_main: Null inputs.")
        } catch (ex: IllegalArgumentException) {
            // Error expected.
        }
    }

    //  MATCH TEST FUNCTIONS
    fun testMatchAlphabet() {
        // Initialise the bitmasks for Bitap.
        var bitmask: MutableMap<Char?, Int?>
        bitmask = HashMap()
        bitmask['a'] = 4
        bitmask['b'] = 2
        bitmask['c'] = 1
        assertEquals("match_alphabet: Unique.", bitmask, dmp!!.match_alphabet("abc"))
        bitmask = HashMap()
        bitmask['a'] = 37
        bitmask['b'] = 18
        bitmask['c'] = 8
        assertEquals("match_alphabet: Duplicates.", bitmask, dmp!!.match_alphabet("abcaba"))
    }

    fun testMatchBitap() {
        // Bitap algorithm.
        dmp!!.Match_Distance = 100
        dmp!!.Match_Threshold = 0.5f
        assertEquals("match_bitap: Exact match #1.", 5, dmp!!.match_bitap("abcdefghijk", "fgh", 5))
        assertEquals("match_bitap: Exact match #2.", 5, dmp!!.match_bitap("abcdefghijk", "fgh", 0))
        assertEquals("match_bitap: Fuzzy match #1.", 4, dmp!!.match_bitap("abcdefghijk", "efxhi", 0))
        assertEquals("match_bitap: Fuzzy match #2.", 2, dmp!!.match_bitap("abcdefghijk", "cdefxyhijk", 5))
        assertEquals("match_bitap: Fuzzy match #3.", -1, dmp!!.match_bitap("abcdefghijk", "bxy", 1))
        assertEquals("match_bitap: Overflow.", 2, dmp!!.match_bitap("123456789xx0", "3456789x0", 2))
        assertEquals("match_bitap: Before start match.", 0, dmp!!.match_bitap("abcdef", "xxabc", 4))
        assertEquals("match_bitap: Beyond end match.", 3, dmp!!.match_bitap("abcdef", "defyy", 4))
        assertEquals("match_bitap: Oversized pattern.", 0, dmp!!.match_bitap("abcdef", "xabcdefy", 0))
        dmp!!.Match_Threshold = 0.4f
        assertEquals("match_bitap: Threshold #1.", 4, dmp!!.match_bitap("abcdefghijk", "efxyhi", 1))
        dmp!!.Match_Threshold = 0.3f
        assertEquals("match_bitap: Threshold #2.", -1, dmp!!.match_bitap("abcdefghijk", "efxyhi", 1))
        dmp!!.Match_Threshold = 0.0f
        assertEquals("match_bitap: Threshold #3.", 1, dmp!!.match_bitap("abcdefghijk", "bcdef", 1))
        dmp!!.Match_Threshold = 0.5f
        assertEquals("match_bitap: Multiple select #1.", 0, dmp!!.match_bitap("abcdexyzabcde", "abccde", 3))
        assertEquals("match_bitap: Multiple select #2.", 8, dmp!!.match_bitap("abcdexyzabcde", "abccde", 5))
        dmp!!.Match_Distance = 10 // Strict location.
        assertEquals(
            "match_bitap: Distance test #1.",
            -1,
            dmp!!.match_bitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24)
        )
        assertEquals(
            "match_bitap: Distance test #2.",
            0,
            dmp!!.match_bitap("abcdefghijklmnopqrstuvwxyz", "abcdxxefg", 1)
        )
        dmp!!.Match_Distance = 1000 // Loose location.
        assertEquals(
            "match_bitap: Distance test #3.",
            0,
            dmp!!.match_bitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24)
        )
    }

    fun testMatchMain() {
        // Full match.
        assertEquals("match_main: Equality.", 0, dmp!!.match_main("abcdef", "abcdef", 1000))
        assertEquals("match_main: Null text.", -1, dmp!!.match_main("", "abcdef", 1))
        assertEquals("match_main: Null pattern.", 3, dmp!!.match_main("abcdef", "", 3))
        assertEquals("match_main: Exact match.", 3, dmp!!.match_main("abcdef", "de", 3))
        assertEquals("match_main: Beyond end match.", 3, dmp!!.match_main("abcdef", "defy", 4))
        assertEquals("match_main: Oversized pattern.", 0, dmp!!.match_main("abcdef", "abcdefy", 0))
        dmp!!.Match_Threshold = 0.7f
        assertEquals(
            "match_main: Complex match.",
            4,
            dmp!!.match_main("I am the very model of a modern major general.", " that berry ", 5)
        )
        dmp!!.Match_Threshold = 0.5f

        // Test null inputs.
        try {
            dmp!!.match_main(null, null, 0)
            fail("match_main: Null inputs.")
        } catch (ex: IllegalArgumentException) {
            // Error expected.
        }
    }

    //  PATCH TEST FUNCTIONS
    fun testPatchObj() {
        // Patch Object.
        val p = diff_match_patch.Patch()
        p.start1 = 20
        p.start2 = 21
        p.length1 = 18
        p.length2 = 17
        p.diffs = diffList(
            diff_match_patch.Diff(EQUAL, "jump"), diff_match_patch.Diff(DELETE, "s"), diff_match_patch.Diff(
                INSERT, "ed"
            ), diff_match_patch.Diff(EQUAL, " over "), diff_match_patch.Diff(DELETE, "the"), diff_match_patch.Diff(
                INSERT, "a"
            ), diff_match_patch.Diff(EQUAL, "\nlaz")
        )
        val strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n"
        assertEquals("Patch: toString.", strp, p.toString())
    }

    fun testPatchFromText() {
        assertTrue("patch_fromText: #0.", dmp!!.patch_fromText("").isEmpty())
        val strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n %0Alaz\n"
        assertEquals("patch_fromText: #1.", strp, dmp!!.patch_fromText(strp)[0].toString())
        assertEquals(
            "patch_fromText: #2.",
            "@@ -1 +1 @@\n-a\n+b\n",
            dmp!!.patch_fromText("@@ -1 +1 @@\n-a\n+b\n")[0].toString()
        )
        assertEquals(
            "patch_fromText: #3.",
            "@@ -1,3 +0,0 @@\n-abc\n",
            dmp!!.patch_fromText("@@ -1,3 +0,0 @@\n-abc\n")[0].toString()
        )
        assertEquals(
            "patch_fromText: #4.",
            "@@ -0,0 +1,3 @@\n+abc\n",
            dmp!!.patch_fromText("@@ -0,0 +1,3 @@\n+abc\n")[0].toString()
        )

        // Generates error.
        try {
            dmp!!.patch_fromText("Bad\nPatch\n")
            fail("patch_fromText: #5.")
        } catch (ex: IllegalArgumentException) {
            // Exception expected.
        }
    }

    fun testPatchToText() {
        var strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n"
        var patches: List<diff_match_patch.Patch?>?
        patches = dmp!!.patch_fromText(strp)
        assertEquals("patch_toText: Single.", strp, dmp!!.patch_toText(patches))
        strp = "@@ -1,9 +1,9 @@\n-f\n+F\n oo+fooba\n@@ -7,9 +7,9 @@\n obar\n-,\n+.\n  tes\n"
        patches = dmp!!.patch_fromText(strp)
        assertEquals("patch_toText: Dual.", strp, dmp!!.patch_toText(patches))
    }

    fun testPatchAddContext() {
        dmp!!.Patch_Margin = 4
        var p: diff_match_patch.Patch
        p = dmp!!.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n")[0]
        dmp!!.patch_addContext(p, "The quick brown fox jumps over the lazy dog.")
        assertEquals(
            "patch_addContext: Simple case.",
            "@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n",
            p.toString()
        )
        p = dmp!!.patch_fromText("@@ -21,4 +21,10 @@\n-jump\n+somersault\n")[0]
        dmp!!.patch_addContext(p, "The quick brown fox jumps.")
        assertEquals(
            "patch_addContext: Not enough trailing context.",
            "@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n",
            p.toString()
        )
        p = dmp!!.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n")[0]
        dmp!!.patch_addContext(p, "The quick brown fox jumps.")
        assertEquals(
            "patch_addContext: Not enough leading context.",
            "@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n",
            p.toString()
        )
        p = dmp!!.patch_fromText("@@ -3 +3,2 @@\n-e\n+at\n")[0]
        dmp!!.patch_addContext(p, "The quick brown fox jumps.  The quick brown fox crashes.")
        assertEquals(
            "patch_addContext: Ambiguity.",
            "@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n",
            p.toString()
        )
    }

    fun testPatchMake() {
        var patches: LinkedList<diff_match_patch.Patch?>?
        patches = dmp!!.patch_make("", "")
        assertEquals("patch_make: Null case.", "", dmp!!.patch_toText(patches))
        var text1 = "The quick brown fox jumps over the lazy dog."
        var text2 = "That quick brown fox jumped over a lazy dog."
        var expectedPatch =
            "@@ -1,8 +1,7 @@\n Th\n-at\n+e\n  qui\n@@ -21,17 +21,18 @@\n jump\n-ed\n+s\n  over \n-a\n+the\n  laz\n"
        // The second patch must be "-21,17 +21,18", not "-22,17 +21,18" due to rolling context.
        patches = dmp!!.patch_make(text2, text1)
        assertEquals("patch_make: Text2+Text1 inputs.", expectedPatch, dmp!!.patch_toText(patches))
        expectedPatch =
            "@@ -1,11 +1,12 @@\n Th\n-e\n+at\n  quick b\n@@ -22,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n"
        patches = dmp!!.patch_make(text1, text2)
        assertEquals("patch_make: Text1+Text2 inputs.", expectedPatch, dmp!!.patch_toText(patches))
        var diffs = dmp!!.diff_main(text1, text2, false)
        patches = dmp!!.patch_make(diffs)
        assertEquals("patch_make: Diff input.", expectedPatch, dmp!!.patch_toText(patches))
        patches = dmp!!.patch_make(text1, diffs)
        assertEquals("patch_make: Text1+Diff inputs.", expectedPatch, dmp!!.patch_toText(patches))
        patches = dmp!!.patch_make(text1, text2, diffs)
        assertEquals("patch_make: Text1+Text2+Diff inputs (deprecated).", expectedPatch, dmp!!.patch_toText(patches))
        patches = dmp!!.patch_make("`1234567890-=[]\\;',./", "~!@#$%^&*()_+{}|:\"<>?")
        assertEquals(
            "patch_toText: Character encoding.",
            "@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n",
            dmp!!.patch_toText(patches)
        )
        diffs = diffList(
            diff_match_patch.Diff(DELETE, "`1234567890-=[]\\;',./"),
            diff_match_patch.Diff(INSERT, "~!@#$%^&*()_+{}|:\"<>?")
        )
        assertEquals(
            "patch_fromText: Character decoding.",
            diffs,
            dmp!!.patch_fromText("@@ -1,21 +1,21 @@\n-%601234567890-=%5B%5D%5C;',./\n+~!@#$%25%5E&*()_+%7B%7D%7C:%22%3C%3E?\n")[0].diffs
        )
        text1 = ""
        for (x in 0..99) {
            text1 += "abcdef"
        }
        text2 = text1 + "123"
        expectedPatch = "@@ -573,28 +573,31 @@\n cdefabcdefabcdefabcdefabcdef\n+123\n"
        patches = dmp!!.patch_make(text1, text2)
        assertEquals("patch_make: Long string with repeats.", expectedPatch, dmp!!.patch_toText(patches))

        // Test null inputs.
        try {
            dmp!!.patch_make(null)
            fail("patch_make: Null inputs.")
        } catch (ex: IllegalArgumentException) {
            // Error expected.
        }
    }

    fun testPatchSplitMax() {
        // Assumes that Match_MaxBits is 32.
        var patches: LinkedList<diff_match_patch.Patch?>?
        patches = dmp!!.patch_make(
            "abcdefghijklmnopqrstuvwxyz01234567890",
            "XabXcdXefXghXijXklXmnXopXqrXstXuvXwxXyzX01X23X45X67X89X0"
        )
        dmp!!.patch_splitMax(patches)
        assertEquals(
            "patch_splitMax: #1.",
            "@@ -1,32 +1,46 @@\n+X\n ab\n+X\n cd\n+X\n ef\n+X\n gh\n+X\n ij\n+X\n kl\n+X\n mn\n+X\n op\n+X\n qr\n+X\n st\n+X\n uv\n+X\n wx\n+X\n yz\n+X\n 012345\n@@ -25,13 +39,18 @@\n zX01\n+X\n 23\n+X\n 45\n+X\n 67\n+X\n 89\n+X\n 0\n",
            dmp!!.patch_toText(patches)
        )
        patches = dmp!!.patch_make(
            "abcdef1234567890123456789012345678901234567890123456789012345678901234567890uvwxyz",
            "abcdefuvwxyz"
        )
        val oldToText = dmp!!.patch_toText(patches)
        dmp!!.patch_splitMax(patches)
        assertEquals("patch_splitMax: #2.", oldToText, dmp!!.patch_toText(patches))
        patches = dmp!!.patch_make("1234567890123456789012345678901234567890123456789012345678901234567890", "abc")
        dmp!!.patch_splitMax(patches)
        assertEquals(
            "patch_splitMax: #3.",
            "@@ -1,32 +1,4 @@\n-1234567890123456789012345678\n 9012\n@@ -29,32 +1,4 @@\n-9012345678901234567890123456\n 7890\n@@ -57,14 +1,3 @@\n-78901234567890\n+abc\n",
            dmp!!.patch_toText(patches)
        )
        patches = dmp!!.patch_make(
            "abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1 abcdefghij , h : 0 , t : 1",
            "abcdefghij , h : 1 , t : 1 abcdefghij , h : 1 , t : 1 abcdefghij , h : 0 , t : 1"
        )
        dmp!!.patch_splitMax(patches)
        assertEquals(
            "patch_splitMax: #4.",
            "@@ -2,32 +2,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n@@ -29,32 +29,32 @@\n bcdefghij , h : \n-0\n+1\n  , t : 1 abcdef\n",
            dmp!!.patch_toText(patches)
        )
    }

    fun testPatchAddPadding() {
        var patches: LinkedList<diff_match_patch.Patch?>?
        patches = dmp!!.patch_make("", "test")
        assertEquals("patch_addPadding: Both edges full.", "@@ -0,0 +1,4 @@\n+test\n", dmp!!.patch_toText(patches))
        dmp!!.patch_addPadding(patches)
        assertEquals(
            "patch_addPadding: Both edges full.",
            "@@ -1,8 +1,12 @@\n %01%02%03%04\n+test\n %01%02%03%04\n",
            dmp!!.patch_toText(patches)
        )
        patches = dmp!!.patch_make("XY", "XtestY")
        assertEquals(
            "patch_addPadding: Both edges partial.",
            "@@ -1,2 +1,6 @@\n X\n+test\n Y\n",
            dmp!!.patch_toText(patches)
        )
        dmp!!.patch_addPadding(patches)
        assertEquals(
            "patch_addPadding: Both edges partial.",
            "@@ -2,8 +2,12 @@\n %02%03%04X\n+test\n Y%01%02%03\n",
            dmp!!.patch_toText(patches)
        )
        patches = dmp!!.patch_make("XXXXYYYY", "XXXXtestYYYY")
        assertEquals(
            "patch_addPadding: Both edges none.",
            "@@ -1,8 +1,12 @@\n XXXX\n+test\n YYYY\n",
            dmp!!.patch_toText(patches)
        )
        dmp!!.patch_addPadding(patches)
        assertEquals(
            "patch_addPadding: Both edges none.",
            "@@ -5,8 +5,12 @@\n XXXX\n+test\n YYYY\n",
            dmp!!.patch_toText(patches)
        )
    }

    fun testPatchApply() {
        dmp!!.Match_Distance = 1000
        dmp!!.Match_Threshold = 0.5f
        dmp!!.Patch_DeleteThreshold = 0.5f
        var patches: LinkedList<diff_match_patch.Patch?>?
        patches = dmp!!.patch_make("", "")
        var results = dmp!!.patch_apply(patches, "Hello world.")
        var boolArray = results[1] as BooleanArray
        var resultStr = results[0].toString() + "\t" + boolArray.size
        assertEquals("patch_apply: Null case.", "Hello world.\t0", resultStr)
        patches = dmp!!.patch_make(
            "The quick brown fox jumps over the lazy dog.",
            "That quick brown fox jumped over a lazy dog."
        )
        results = dmp!!.patch_apply(patches, "The quick brown fox jumps over the lazy dog.")
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0] + "\t" + boolArray[1]
        assertEquals("patch_apply: Exact match.", "That quick brown fox jumped over a lazy dog.\ttrue\ttrue", resultStr)
        results = dmp!!.patch_apply(patches, "The quick red rabbit jumps over the tired tiger.")
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0] + "\t" + boolArray[1]
        assertEquals(
            "patch_apply: Partial match.",
            "That quick red rabbit jumped over a tired tiger.\ttrue\ttrue",
            resultStr
        )
        results = dmp!!.patch_apply(patches, "I am the very model of a modern major general.")
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0] + "\t" + boolArray[1]
        assertEquals(
            "patch_apply: Failed match.",
            "I am the very model of a modern major general.\tfalse\tfalse",
            resultStr
        )
        patches = dmp!!.patch_make("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy")
        results = dmp!!.patch_apply(
            patches,
            "x123456789012345678901234567890-----++++++++++-----123456789012345678901234567890y"
        )
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0] + "\t" + boolArray[1]
        assertEquals("patch_apply: Big delete, small change.", "xabcy\ttrue\ttrue", resultStr)
        patches = dmp!!.patch_make("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy")
        results = dmp!!.patch_apply(
            patches,
            "x12345678901234567890---------------++++++++++---------------12345678901234567890y"
        )
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0] + "\t" + boolArray[1]
        assertEquals(
            "patch_apply: Big delete, big change 1.",
            "xabc12345678901234567890---------------++++++++++---------------12345678901234567890y\tfalse\ttrue",
            resultStr
        )
        dmp!!.Patch_DeleteThreshold = 0.6f
        patches = dmp!!.patch_make("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy")
        results = dmp!!.patch_apply(
            patches,
            "x12345678901234567890---------------++++++++++---------------12345678901234567890y"
        )
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0] + "\t" + boolArray[1]
        assertEquals("patch_apply: Big delete, big change 2.", "xabcy\ttrue\ttrue", resultStr)
        dmp!!.Patch_DeleteThreshold = 0.5f

        // Compensate for failed patch.
        dmp!!.Match_Threshold = 0.0f
        dmp!!.Match_Distance = 0
        patches = dmp!!.patch_make(
            "abcdefghijklmnopqrstuvwxyz--------------------1234567890",
            "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890"
        )
        results = dmp!!.patch_apply(patches, "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890")
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0] + "\t" + boolArray[1]
        assertEquals(
            "patch_apply: Compensate for failed patch.",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890\tfalse\ttrue",
            resultStr
        )
        dmp!!.Match_Threshold = 0.5f
        dmp!!.Match_Distance = 1000
        patches = dmp!!.patch_make("", "test")
        var patchStr = dmp!!.patch_toText(patches)
        dmp!!.patch_apply(patches, "")
        assertEquals("patch_apply: No side effects.", patchStr, dmp!!.patch_toText(patches))
        patches = dmp!!.patch_make("The quick brown fox jumps over the lazy dog.", "Woof")
        patchStr = dmp!!.patch_toText(patches)
        dmp!!.patch_apply(patches, "The quick brown fox jumps over the lazy dog.")
        assertEquals("patch_apply: No side effects with major delete.", patchStr, dmp!!.patch_toText(patches))
        patches = dmp!!.patch_make("", "test")
        results = dmp!!.patch_apply(patches, "")
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0]
        assertEquals("patch_apply: Edge exact match.", "test\ttrue", resultStr)
        patches = dmp!!.patch_make("XY", "XtestY")
        results = dmp!!.patch_apply(patches, "XY")
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0]
        assertEquals("patch_apply: Near edge exact match.", "XtestY\ttrue", resultStr)
        patches = dmp!!.patch_make("y", "y123")
        results = dmp!!.patch_apply(patches, "x")
        boolArray = results[1] as BooleanArray
        resultStr = results[0].toString() + "\t" + boolArray[0]
        assertEquals("patch_apply: Edge partial match.", "x123\ttrue", resultStr)
    }

    private fun assertEquals(error_msg: String, a: Any, b: Any) {
        if (a.toString() != b.toString()) {
            throw Error(
                """assertEquals fail:
 Expected: $a
 Actual: $b
$error_msg"""
            )
        }
    }

    private fun assertTrue(error_msg: String, a: Boolean) {
        if (!a) {
            throw Error("assertTrue fail: $error_msg")
        }
    }

    private fun assertNull(error_msg: String, n: Any?) {
        if (n != null) {
            throw Error("assertNull fail: $error_msg")
        }
    }

    private fun fail(error_msg: String) {
        throw Error("Fail: $error_msg")
    }

    private fun assertArrayEquals(error_msg: String, a: Array<String>, b: Array<String>) {
        val list_a = listOf(*a)
        val list_b = listOf(*b)
        assertEquals(error_msg, list_a, list_b)
    }

    private fun assertLinesToCharsResultEquals(
        error_msg: String,
        a: LinesToCharsResult, b: LinesToCharsResult
    ) {
        assertEquals(error_msg, a.chars1, b.chars1)
        assertEquals(error_msg, a.chars2, b.chars2)
        assertEquals(error_msg, a.lineArray, b.lineArray)
    }

    // Construct the two texts which made up the diff originally.
    private fun diff_rebuildtexts(diffs: LinkedList<diff_match_patch.Diff>): Array<String> {
        val text = arrayOf("", "")
        for (myDiff in diffs) {
            if (myDiff.operation != diff_match_patch.Operation.INSERT) {
                text[0] += myDiff.text
            }
            if (myDiff.operation != diff_match_patch.Operation.DELETE) {
                text[1] += myDiff.text
            }
        }
        return text
    }

    // Private function for quickly building lists of diffs.
    private fun diffList(vararg diffs: diff_match_patch.Diff): LinkedList<diff_match_patch.Diff> {
        return LinkedList(Arrays.asList(*diffs))
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        dmp = diff_match_patch()
        testDiffCommonPrefix()
        testDiffCommonSuffix()
        testDiffCommonOverlap()
        testDiffHalfmatch()
        testDiffLinesToChars()
        testDiffCharsToLines()
        testDiffCleanupMerge()
        testDiffCleanupSemanticLossless()
        testDiffCleanupSemantic()
        testDiffCleanupEfficiency()
        testDiffPrettyHtml()
        testDiffText()
        testDiffDelta()
        testDiffXIndex()
        testDiffLevenshtein()
        testDiffBisect()
        testDiffMain()
        testMatchAlphabet()
        testMatchBitap()
        testMatchMain()
        testPatchObj()
        testPatchFromText()
        testPatchToText()
        testPatchAddContext()
        testPatchMake()
        testPatchSplitMax()
        testPatchAddPadding()
        testPatchApply()
        val dmp = diff_match_patch()
        //      LinkedList<diff_match_patch.Diff> diff = dmp.diff_main("Hello World.", "Goodbye World.");
        val bytes1 =
            File("path1").readText()
        val bytes2 =
            File("path2").readText()
        val diff = dmp.diff_main(bytes1, bytes2)
        // Result: [(-1, "Hell"), (1, "G"), (0, "o"), (1, "odbye"), (0, " World.")]
//        dmp.diff_cleanupSemantic(diff)
//        diff.removeAll { it.operation.equals(diff_match_patch.Operation.EQUAL) }
        // Result: [(-1, "Hello"), (1, "Goodbye"), (0, " World.")]
        diff.forEach { println("Operation: ${it.operation}\n${it.text.take(50)}") }
//        println(diff)
        println("All tests passed.")
    }
}