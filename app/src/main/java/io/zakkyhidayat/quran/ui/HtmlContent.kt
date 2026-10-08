package io.zakkyhidayat.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

sealed interface HtmlBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : HtmlBlock
    data class Paragraph(val text: AnnotatedString) : HtmlBlock
    data class Item(val marker: String, val text: AnnotatedString) : HtmlBlock
}

private val TOKEN = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>|([^<]+)")
private val HREF = Regex("href=\"([^\"]*)\"")

private fun unescape(text: String): String = text
    .replace("&nbsp;", " ").replace("&amp;", "&").replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"")

// Parser untuk subset HTML info surah: p, h1-h3, strong/b, em/i, a, ol/ul, li.
fun parseHtml(html: String, linkStyle: SpanStyle, onLink: (String) -> Unit): List<HtmlBlock> {
    val blocks = mutableListOf<HtmlBlock>()
    var builder: AnnotatedString.Builder? = null
    var kind: (AnnotatedString) -> HtmlBlock = { HtmlBlock.Paragraph(it) }
    var listCounter = 0
    var ordered = true

    fun flush() {
        val b = builder ?: return
        val text = b.toAnnotatedString()
        if (text.text.isNotBlank()) blocks += kind(text)
        builder = null
    }

    fun open(make: (AnnotatedString) -> HtmlBlock) {
        flush()
        kind = make
        builder = AnnotatedString.Builder()
    }

    for (m in TOKEN.findAll(html)) {
        val text = m.groupValues[4]
        if (text.isNotEmpty()) {
            val collapsed = unescape(text).replace(Regex("\\s+"), " ")
            if (builder == null) open { HtmlBlock.Paragraph(it) }
            // Hindari spasi di awal blok.
            builder!!.append(if (builder!!.length == 0) collapsed.trimStart() else collapsed)
            continue
        }
        val closing = m.groupValues[1] == "/"
        val tag = m.groupValues[2].lowercase()
        val attrs = m.groupValues[3]
        when (tag) {
            "p" -> if (closing) flush() else open { HtmlBlock.Paragraph(it) }
            "h1", "h2", "h3" -> if (closing) flush() else {
                val level = tag.last().digitToInt()
                open { HtmlBlock.Heading(level, it) }
            }
            "ol", "ul" -> if (!closing) { flush(); listCounter = 0; ordered = tag == "ol" }
            "li" -> if (closing) flush() else {
                listCounter++
                val marker = if (ordered) "$listCounter." else "•"
                open { HtmlBlock.Item(marker, it) }
            }
            "strong", "b" -> builder?.let { if (closing) it.pop() else it.pushStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            "em", "i" -> builder?.let { if (closing) it.pop() else it.pushStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            "a" -> builder?.let {
                if (closing) {
                    it.pop()
                } else {
                    val href = HREF.find(attrs)?.groupValues?.get(1).orEmpty()
                    it.pushLink(LinkAnnotation.Clickable(href, TextLinkStyles(linkStyle)) { onLink(href) })
                }
            }
        }
    }
    flush()
    return blocks
}

@Composable
fun HtmlContent(html: String, modifier: Modifier = Modifier, onLink: (String) -> Unit) {
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val blocks = remember(html, linkStyle) { parseHtml(html, linkStyle, onLink) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block) {
                is HtmlBlock.Heading -> Text(
                    block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                is HtmlBlock.Paragraph -> {
                    val body = MaterialTheme.typography.bodyLarge
                    Text(block.text, style = body.copy(lineHeight = body.fontSize * 1.6f))
                }
                is HtmlBlock.Item -> {
                    val body = MaterialTheme.typography.bodyLarge
                    androidx.compose.foundation.layout.Row(Modifier.padding(start = 8.dp)) {
                        Text(block.marker, style = body, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                        Text(block.text, style = body.copy(lineHeight = body.fontSize * 1.6f), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Tautan ayat bentuk "/2/67-73", "/2/255" atau "/2" -> (surah, ayat pertama).
fun parseAyahLink(href: String): Pair<Int, Int>? {
    val m = Regex("^/(\\d{1,3})(?:/(\\d{1,3}))?").find(href) ?: return null
    val surah = m.groupValues[1].toInt()
    if (surah !in 1..114) return null
    return surah to (m.groupValues[2].ifEmpty { "1" }.toInt())
}
