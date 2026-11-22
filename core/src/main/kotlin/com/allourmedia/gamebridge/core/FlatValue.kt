package com.allourmedia.gamebridge.core

/** A value in a flat JSON object: only strings and integers exist in a Bridge Shortcut. */
internal sealed interface FlatValue {
    data class Text(
        val value: String,
    ) : FlatValue

    /** A JSON integer kept as its literal (`-?(0|[1-9][0-9]*)`) so that range checks never truncate. */
    data class Integer(
        val literal: String,
    ) : FlatValue {
        /** The value as an Int, or null when it does not fit. */
        fun toIntOrNull(): Int? = literal.toIntOrNull()
    }
}

/**
 * Reads one JSON object whose values are strings or integers, or returns null. Strict on purpose: no nesting, no
 * booleans, nulls, fractions or exponents, no duplicate keys, no trailing commas, nothing after the object. Input is
 * untrusted Shortcut text and is already size-capped, so there is no recursion and no partial result.
 */
internal fun readFlatJsonObject(text: String): Map<String, FlatValue>? = FlatJsonReader(text).readObject()

private class FlatJsonReader(
    private val text: String,
) {
    private var pos = 0

    fun readObject(): Map<String, FlatValue>? {
        skipWhitespace()
        if (!consume('{')) return null
        val result = LinkedHashMap<String, FlatValue>()
        skipWhitespace()
        if (consume('}')) return null // An empty object is never a valid Bridge Shortcut.
        while (true) {
            skipWhitespace()
            val key = readString() ?: return null
            skipWhitespace()
            if (!consume(':')) return null
            skipWhitespace()
            val value = readValue() ?: return null
            if (result.put(key, value) != null) return null
            skipWhitespace()
            if (consume(',')) continue
            if (consume('}')) break
            return null
        }
        skipWhitespace()
        return if (pos == text.length) result else null
    }

    private fun readValue(): FlatValue? =
        when (peek()) {
            '"' -> readString()?.let { FlatValue.Text(it) }
            '-', in '0'..'9' -> readInteger()
            else -> null
        }

    private fun readInteger(): FlatValue.Integer? {
        val start = pos
        consume('-')
        when (peek()) {
            '0' -> pos++
            in '1'..'9' -> while (peek() in '0'..'9') pos++
            else -> return null
        }
        // A fraction, exponent or more digits after a leading zero is not an integer literal.
        if (peek() == '.' || peek() == 'e' || peek() == 'E' || peek() in '0'..'9') return null
        return FlatValue.Integer(text.substring(start, pos))
    }

    private fun readString(): String? {
        if (!consume('"')) return null
        val out = StringBuilder()
        while (pos < text.length) {
            val c = text[pos++]
            when {
                c == '"' -> return out.toString()
                c < ' ' -> return null
                c != '\\' -> out.append(c)
                else -> out.append(readEscape() ?: return null)
            }
        }
        return null
    }

    private fun readEscape(): Char? {
        if (pos >= text.length) return null
        return when (text[pos++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> readUnicodeEscape()
            else -> null
        }
    }

    private fun readUnicodeEscape(): Char? {
        if (pos + 4 > text.length) return null
        val hex = text.substring(pos, pos + 4)
        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        pos += 4
        return hex.toInt(16).toChar()
    }

    private fun skipWhitespace() {
        while (peek() == ' ' || peek() == '\t' || peek() == '\n' || peek() == '\r') pos++
    }

    private fun peek(): Char? = text.getOrNull(pos)

    private fun consume(c: Char): Boolean = (peek() == c).also { if (it) pos++ }
}
