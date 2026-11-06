package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ai.OfficeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The words inside a Word, Excel or PowerPoint file.
 *
 * Each format is built here as the zip of XML it really is, so the test exercises
 * the reading rather than a stub of it. A .docx was refused outright before this,
 * which is a poor answer to somebody who has just attached their homework.
 */
class OfficeTextTest {

    /** Builds a zip from path → contents, the way every Office file is stored. */
    private fun zip(entries: Map<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { out ->
            for ((path, content) in entries) {
                out.putNextEntry(ZipEntry(path))
                out.write(content.toByteArray(Charsets.UTF_8))
                out.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun docx(vararg paragraphs: String): ByteArray {
        val body = paragraphs.joinToString("") { p ->
            "<w:p><w:r><w:t xml:space=\"preserve\">$p</w:t></w:r></w:p>"
        }
        return zip(
            mapOf(
                "word/document.xml" to
                    "<w:document><w:body>$body</w:body></w:document>",
                "[Content_Types].xml" to "<Types/>",
            ),
        )
    }

    // ---- Word ----

    @Test fun aWordDocumentIsReadParagraphByParagraph() {
        val text = OfficeText.extract(docx("Simple interest is P R T / 100", "Where R is per year"), "notes.docx")
        assertEquals("Simple interest is P R T / 100\nWhere R is per year", text)
    }

    @Test fun aParagraphSplitAcrossRunsIsOneLine() {
        // Word splits a sentence across runs whenever the formatting changes,
        // and the words are still one sentence
        val body = "<w:p><w:r><w:t>Hello </w:t></w:r><w:r><w:t>world</w:t></w:r></w:p>"
        val bytes = zip(mapOf("word/document.xml" to "<w:document><w:body>$body</w:body></w:document>"))
        assertEquals("Hello world", OfficeText.extract(bytes, "a.docx"))
    }

    @Test fun aTableTagIsNotMistakenForATextTag() {
        // <w:tbl> starts with the same three characters as <w:t> and must not be
        // read as a text run
        val body = "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>cell</w:t></w:r></w:p></w:tc></w:tr></w:tbl>"
        val bytes = zip(mapOf("word/document.xml" to "<w:document><w:body>$body</w:body></w:document>"))
        val text = OfficeText.extract(bytes, "a.docx")
        assertEquals("cell", text)
    }

    @Test fun anEscapedCharacterComesBackAsItself() {
        val text = OfficeText.extract(docx("a &lt; b &amp;&amp; c &gt; d"), "a.docx")
        assertEquals("a < b && c > d", text)
    }

    // ---- PowerPoint ----

    @Test fun slidesAreReadInOrderNotAlphabetically() {
        val bytes = zip(
            mapOf(
                "ppt/slides/slide10.xml" to "<p:sld><a:t>tenth</a:t></p:sld>",
                "ppt/slides/slide2.xml" to "<p:sld><a:t>second</a:t></p:sld>",
                "ppt/slides/slide1.xml" to "<p:sld><a:t>first</a:t></p:sld>",
            ),
        )
        val text = OfficeText.extract(bytes, "deck.pptx")!!
        val first = text.indexOf("first")
        val second = text.indexOf("second")
        val tenth = text.indexOf("tenth")
        assertTrue("slide 1 should come before slide 2", first in 0 until second)
        assertTrue("slide 2 should come before slide 10", second in 0 until tenth)
    }

    // ---- Excel ----

    @Test fun aCellPointsIntoTheSharedStringsTable() {
        val bytes = zip(
            mapOf(
                "xl/sharedStrings.xml" to
                    "<sst><si><t>Principal</t></si><si><t>Interest</t></si></sst>",
                "xl/worksheets/sheet1.xml" to
                    "<worksheet><sheetData>" +
                    "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c></row>" +
                    "<row r=\"2\"><c r=\"A2\"><v>50000</v></c><c r=\"B2\"><v>4000</v></c></row>" +
                    "</sheetData></worksheet>",
            ),
        )
        val text = OfficeText.extract(bytes, "book.xlsx")!!
        assertTrue("the header row is missing: $text", text.contains("A1: Principal"))
        assertTrue("the second header is missing: $text", text.contains("B1: Interest"))
        assertTrue("the number row is missing: $text", text.contains("A2: 50000"))
    }

    @Test fun theSheetsAreNamedSoTheRowsMakeSense() {
        val bytes = zip(
            mapOf(
                "xl/worksheets/sheet1.xml" to "<worksheet><sheetData><row><c r=\"A1\"><v>1</v></c></row></sheetData></worksheet>",
            ),
        )
        assertTrue(OfficeText.extract(bytes, "b.xlsx")!!.startsWith("Sheet 1:"))
    }

    // ---- what it refuses ----

    @Test fun somethingThatIsNotAZipIsNotGuessedAt() {
        // null rather than an empty string, so the caller can tell "no words in
        // it" from "not the file it claims to be"
        assertNull(OfficeText.extract("not a zip at all".toByteArray(), "a.docx"))
    }

    @Test fun aZipWithNoDocumentInItGivesNothing() {
        val bytes = zip(mapOf("readme.txt" to "hello"))
        assertNull(OfficeText.extract(bytes, "a.docx"))
    }

    @Test fun onlyTheThreeFormatsAreClaimed() {
        assertTrue(OfficeText.handles("notes.docx"))
        assertTrue(OfficeText.handles("BOOK.XLSX"))
        assertTrue(OfficeText.handles("deck.pptx"))
        assertTrue(!OfficeText.handles("photo.jpg"))
        assertTrue(!OfficeText.handles("paper.pdf"))
        assertTrue(!OfficeText.handles("notes.docx.exe"))
    }
}
