package net.fellbaum.jemoji.generator

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Writes [text] as UTF-8, but only if the file doesn't already have exactly this content.
 * Unchanged files are not touched, which avoids rewriting hundreds of MB of identical output and keeps
 * their modification time. Parent directories are created if needed.
 *
 * The size check is a cheap metadata lookup; the content is only read if the sizes match.
 */
fun File.writeTextIfChanged(text: String) {
    val bytes = text.toByteArray(Charsets.UTF_8)
    if (isFile && length() == bytes.size.toLong() && readBytes().contentEquals(bytes)) {
        WriteStats.skipped.incrementAndGet()
        return
    }
    parentFile?.mkdirs()
    writeBytes(bytes)
    WriteStats.written.incrementAndGet()
    WriteStats.writtenBytes.addAndGet(bytes.size.toLong())
}

/** Counts writes for the summary at the end of a run. Thread-safe, CLDR files are written in parallel. */
object WriteStats {
    val written = AtomicInteger()
    val skipped = AtomicInteger()
    val writtenBytes = AtomicLong()

    fun summary() =
        "Files written: ${written.get()} (${writtenBytes.get() / 1024 / 1024} MB), unchanged: ${skipped.get()}"
}
