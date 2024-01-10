package org.gamekins.util

import hudson.FilePath
import org.jsoup.nodes.Document
import java.io.File

object CheckstyleUtil {

    /**
     * generate an error list for the given source or test file [filepath] using the given checkstyle.html parsed
     * [document], the errors are a subset of the ones detected by sun_checks.xml predefined ruleset,
     * errors deemed relevant can be added or removed as desired. List of possible check can be found
     * here https://checkstyle.sourceforge.io/sun_style.html.
     */
    fun getFileStyleErrors(document: Document, filepath: FilePath)
    : ArrayList<CheckstyleErrorData>? {
        val anchorElements = document.select(buildTagName(filepath))

        if(anchorElements.isEmpty()) return null

        val anchorElement = anchorElements[1]
        var tableElements = anchorElement.nextElementSibling()!!.select("tr")
        tableElements = tableElements.next()
        val errorList = ArrayList<CheckstyleErrorData>()

        for (tableElement in tableElements) {
            val rowElements = tableElement.select("td")
            var error: CheckstyleErrorData? = null
            when (rowElements[2].text()) {
                "MissingJavadocMethod", "FinalLocalVariable", "InnerAssignment",
                "SimplifyBooleanExpression", "FinalClass", "HideUtilityClassConstructor",
                "InnerTypeLast", "OneTopLevelClass", "MutableException", "UnusedImports",
                "JavadocMissingWhitespaceAfterAsterisk", "MissingJavadocPackage",
                "JavadocContentLocation", "TrailingComment"-> {
                    error = CheckstyleErrorData(
                        rowElements[1].text(),
                        rowElements[2].text(),
                        rowElements[3].text(),
                        rowElements[4].text()
                    )
                }
            }
            if (error != null) errorList.add(error)
        }
        if (errorList.isEmpty()) return null
        return errorList
    }

    /**
     * Given an [errorsList] return all the errors with rule equals to [rule].
     * errorsList must not be null and contain at least one error with rule type equals to [rule].
     */
    fun getErrorsByRule(errorsList: ArrayList<CheckstyleErrorData>, rule: String)
    : ArrayList<CheckstyleErrorData> {
        val chosenRuleTypeErrorsList =  ArrayList<CheckstyleErrorData>()
        for (error in errorsList)
            if (error.rule == rule) chosenRuleTypeErrorsList.add(error)
        return chosenRuleTypeErrorsList
    }

    /**
     * Get content of a specific line given a [lineNumber] and a [file]
     */
    fun getLineContent(file: File, lineNumber: Int): String? {
        return file.useLines { lines ->
            lines.elementAtOrNull(lineNumber - 1) // Adjust index to 0-based
        }
    }

    /**
     * Removes the errors of a specific [rule] type from a [errorsList]
     */
    fun excludeRuleFromErrorsList(errorsList: ArrayList<CheckstyleErrorData>, rule: String)
    : ArrayList<CheckstyleErrorData> {
        val filteredErrorList = ArrayList<CheckstyleErrorData>()
        for (error in errorsList)
            if (error.rule != rule) filteredErrorList.add(error)
        return filteredErrorList
    }

    /**
     * Starting from a source or test file path [filepath] build the tag name associated to the file
     * table in checkstyle.html
     */
    private fun buildTagName(filepath: FilePath): String {
        var pathSplit = filepath.remote.split("/".toRegex())
        if (pathSplit.size == 1)
            pathSplit = filepath.remote.split("\\")
        var i = 0
        while(pathSplit[i] != "com") i++
        val subPath = pathSplit.subList(i, pathSplit.size).joinToString(".2F")
        return "a[name=$subPath]"
    }

    /**
     * The class representation of an error detected by checkstyle maven plugin.
     */
    class CheckstyleErrorData(
        var category: String = "",
        var rule: String = "",
        var message: String = "",
        var line: String = ""
    )
}