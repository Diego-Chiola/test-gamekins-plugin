package org.gamekins.util

import hudson.FilePath
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import kotlin.jvm.Throws

object JsoupUtil {
    /**
     * Generates the Jsoup document of a HTML [file].
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun generateDocument(file: FilePath): Document {
        return Jsoup.parse(file.readToString())
    }
}