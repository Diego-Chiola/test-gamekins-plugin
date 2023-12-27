package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.util.Constants
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JacocoUtil
import org.gamekins.util.JsoupUtil
import org.gamekins.util.CheckstyleUtil
import org.jsoup.nodes.Document
import java.io.File
import kotlin.random.Random

class CheckStyleChallenge(data: Challenge.ChallengeGenerationData,
                          private val errorsList: ArrayList<CheckstyleUtil.CheckstyleErrorData>
)
    : Challenge {

    private val details = data.selectedFile!!
    private val created = System.currentTimeMillis()
    private var chosenError: CheckstyleUtil.CheckstyleErrorData
    private val errorLineContent: String
    private var solved: Long = 0

    init {
        chosenError = errorsList.random()
        errorLineContent = CheckstyleUtil.getLineContent(details.file, chosenError.line.toInt())!!
    }
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is CheckStyleChallenge) return false
        return (this.chosenError.rule == other.chosenError.rule
                && this.errorLineContent == other.errorLineContent)
    }

    override fun getParameters(): Parameters {
        return details.parameters
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getName(): String {
        return "Checkstyle"
    }

    override fun getScore(): Int {
        return 1
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun isSolvable(parameters: Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (details.parameters.branch != parameters.branch) {
            listener.logger.println("\nisSolvable if branch return TRUE")
            return true
        }
        if (!details.update(parameters).filesExists()) {
            listener.logger.println("\nisSolvable if file exist return FALSE")
            return false
        }

        for (i in errorsList)
            listener.logger.println("isSolvable errorList: ${i.rule} ")
        listener.logger.println("\nisSolvable chosenError: $chosenError\n")
        listener.logger.println("isSolvable errorLineContent: $errorLineContent\n")

        val checkstyleHTMLFile = JacocoUtil.calculateCurrentFilePath(parameters.workspace,
            details.checkstyleHTMLFile, details.parameters.remote)
        val sourceFilePath = JacocoUtil.calculateCurrentFilePath(parameters.workspace,
            details.file, details.parameters.remote)
        val newSourceFile = File(sourceFilePath.remote)
        listener.logger.println("isSolvable newSourceFile absolutePath: ${newSourceFile.absolutePath}")

        val document: Document = try {
            if (!checkstyleHTMLFile.exists()) {
                listener.logger.println("[Gamekins] checkstyle source file "
                        + checkstyleHTMLFile.remote + Constants.EXISTS + checkstyleHTMLFile.exists())
                return true
            }
            JsoupUtil.generateDocument(checkstyleHTMLFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }

        val newErrorsChosenRuleTypeList = updatedErrorList(document, sourceFilePath, listener)
        if (newErrorsChosenRuleTypeList == null){
            listener.logger.println("isSolved newErrorsChosenRuleTypeList is null FALSE")
            return false
        }
        if (newErrorsChosenRuleTypeList.isEmpty()){
            listener.logger.println("isSolved newErrorsChosenRuleTypeList is empty TRUE")
            return true
        }
        for (a in newErrorsChosenRuleTypeList)
            listener.logger.println("isSolvable newErrorsChosenRuleTypeList Rule and Line: ${a.rule} ${a.line}\n")


        for (error in newErrorsChosenRuleTypeList){
            if (CheckstyleUtil.getLineContent(newSourceFile, error.line.toInt()) == errorLineContent) {
                listener.logger.println("isSolvable found same error, not solved yet, update error")
                chosenError = error
            }
        }
        return true
    }

    override fun isSolved(parameters: Parameters, run: Run<*, *>, listener: TaskListener): Boolean {

        for (i in errorsList)
            listener.logger.println("isSolved errorList: ${i.rule} ")
        listener.logger.println("\nisSolved chosenError: $chosenError\n")
        listener.logger.println("isSolved errorLineContent: $errorLineContent\n")

        val checkstyleHTMLFile = JacocoUtil.calculateCurrentFilePath(parameters.workspace,
            details.checkstyleHTMLFile, details.parameters.remote)
        val sourceFilePath = JacocoUtil.calculateCurrentFilePath(parameters.workspace,
            details.file, details.parameters.remote)
        val newSourceFile = File(sourceFilePath.remote)
        listener.logger.println("isSolved newSourceFile absolutePath: ${newSourceFile.absolutePath}")

        if (!checkstyleHTMLFile.exists() || !sourceFilePath.exists()) return false

        val document: Document = try {
            JsoupUtil.generateDocument(checkstyleHTMLFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }

        val newErrorsChosenRuleTypeList = updatedErrorList(document, sourceFilePath, listener)
        if (newErrorsChosenRuleTypeList == null){
            listener.logger.println("isSolved newErrorsChosenRuleTypeList is null FALSE")
            return false
        }
        if (newErrorsChosenRuleTypeList.isEmpty()){
            listener.logger.println("isSolved newErrorsChosenRuleTypeList is empty TRUE")
            return true
        }
        for (a in newErrorsChosenRuleTypeList)
            listener.logger.println("isSolved newErrorsChosenRuleTypeList Rule and Line: ${a.rule} ${a.line}\n")

        for (error in newErrorsChosenRuleTypeList){
            if (CheckstyleUtil.getLineContent(newSourceFile, error.line.toInt()) == errorLineContent) {
                listener.logger.println("isSolved found same error, not solved yet")
                return false
            }
        }

        solved = System.currentTimeMillis()
        return true
    }

    override fun printToXML(reason: String, indentation: String): String? {
        var print = (indentation + "<" + this::class.simpleName + " created=\"" + created + "\" solved=\"" + solved
                + "\" class=\"" + details.fileName + "\" category=\"" + chosenError.category + "\" rule=\"" +
                chosenError.rule + "\" message=\"" + chosenError.message + "\" line=\"" + chosenError.line)
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    override fun toString(): String {
        return ("Adjust your code in class to conform to style rules. " +
                "In class <b>${details.fileName}</b> in package <b>${details.packageName}</b> " +
                "for category <b>${chosenError.category}</b> a violation " +
                "of the rule <b>${chosenError.rule}</b> at line <b>${chosenError.line}</b> has been found " +
                "with the following <b>message</b>: ${chosenError.message}")
    }

    override fun hashCode(): Int {
        var result = details.hashCode()
        result = 31 * result + errorLineContent.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        return result
    }

    private fun updatedErrorList(document: Document, sourceFilePath: FilePath, listener: TaskListener)
        : ArrayList<CheckstyleUtil.CheckstyleErrorData>? {
        val newErrorsList = CheckstyleUtil.getFileStyleErrors(document, sourceFilePath, listener) ?: return null

        if (newErrorsList.isEmpty()) return  newErrorsList

        val newErrorsChosenRuleTypeList = ArrayList<CheckstyleUtil.CheckstyleErrorData>()
        for (error in newErrorsList)
            if (error.rule == errorsList[0].rule) newErrorsChosenRuleTypeList.add(error)

        return  newErrorsChosenRuleTypeList
    }

}
