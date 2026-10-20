package io.zakkyhidayat.quran.data

import androidx.compose.ui.text.SpanStyle
import io.zakkyhidayat.quran.ui.HtmlBlock
import io.zakkyhidayat.quran.ui.htmlTextLength
import io.zakkyhidayat.quran.ui.limitBlocks
import io.zakkyhidayat.quran.ui.parseHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirTextTest {
    private fun blocks(html: String) = parseHtml(html, SpanStyle()) { }

    @Test
    fun `teks polos dipecah per baris menjadi paragraf`() {
        val html = normalizeTafsirHtml("Baris satu.\n\n  Baris dua.  \n")
        assertEquals("<p>Baris satu.</p><p>Baris dua.</p>", html)
        assertEquals(2, blocks(html).size)
    }

    @Test
    fun `teks yang sudah berblok tidak diubah`() {
        val raw = "<p>Satu</p>\n<h3>Judul</h3>\n<p>Dua</p>"
        assertEquals(raw, normalizeTafsirHtml(raw))
        assertEquals("<ol><li>a</li></ol>", normalizeTafsirHtml("<ol><li>a</li></ol>"))
    }

    @Test
    fun `br menjadi baris baru di dalam paragraf`() {
        val paragraph = blocks("<p>a<br/>b</p>").single() as HtmlBlock.Paragraph
        assertEquals("a\nb", paragraph.text.text)
    }

    @Test
    fun `blok dibatasi menurut jumlah huruf dan blok yang melewati batas dipotong`() {
        val html = "<p>${"a".repeat(100)}</p><p>${"b".repeat(100)}</p><p>${"c".repeat(100)}</p>"
        val all = blocks(html)
        assertEquals(300, htmlTextLength(html))
        assertEquals(1, limitBlocks(all, 50).size)
        assertEquals(2, limitBlocks(all, 150).size)
        assertEquals(3, limitBlocks(all, 1_000).size)
        assertEquals(3, limitBlocks(all, Int.MAX_VALUE).size)
        // Blok kedua dipotong pada sisa jatah (50 huruf) dan diberi elipsis.
        val second = limitBlocks(all, 150)[1] as HtmlBlock.Paragraph
        assertEquals("b".repeat(50) + "…", second.text.text)
    }

    @Test
    fun `satu paragraf raksasa dipotong di batas kata`() {
        val huge = "kata ".repeat(40_000) // 200 ribu huruf dalam satu paragraf
        val shown = limitBlocks(blocks("<p>$huge</p>"), 3_000)
        assertEquals(1, shown.size)
        val text = (shown.single() as HtmlBlock.Paragraph).text.text
        assertTrue(text.length <= 3_001)
        assertTrue(text.endsWith("kata…"))
    }

    @Test
    fun `label rentang kelompok ayat`() {
        val one = TafsirEntry("p", 2, 5, 2, 5, "")
        assertFalse(one.isGroup)
        val group = TafsirEntry("p", 2, 1, 2, 5, "")
        assertTrue(group.isGroup)
        assertEquals("2:1-5", group.rangeLabel())
        assertEquals("2:285-3:2", TafsirEntry("p", 2, 285, 3, 2, "").rangeLabel())
    }

    @Test
    fun `kunci ayat surah kali seribu ditambah ayat`() {
        assertEquals(2255, tafsirKey(2, 255))
        assertEquals(114006, tafsirKey(114, 6))
    }
}
