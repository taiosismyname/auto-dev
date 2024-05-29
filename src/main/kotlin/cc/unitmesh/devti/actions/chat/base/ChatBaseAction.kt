package cc.unitmesh.devti.actions.chat.base

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.gui.sendToChatPanel
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.temporary.getElementToAction
import com.intellij.util.DocumentUtil

abstract class ChatBaseAction : AnAction() {
    companion object {
        private val logger = logger<ChatBaseAction>()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    open fun chatCompletedPostAction(event: AnActionEvent, panel: ChatCodingPanel): ((response: String) -> Unit)? = null

    abstract fun getActionType(): ChatActionType

    override fun actionPerformed(event: AnActionEvent) = executeAction(event)

    open fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val document = event.getData(CommonDataKeys.EDITOR)?.document

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE)

        var prompt = buildSelectionText(editor, caretModel)

        val lineEndOffset = document?.getLineEndOffset(document.getLineNumber(caretModel?.offset ?: 0)) ?: 0

        // if selectedText is empty, then we use the cursor position to get the text
        if (prompt.isEmpty()) {
            prompt = document?.text?.substring(0, lineEndOffset) ?: ""
        }

        val suffixText = document?.text?.substring(lineEndOffset) ?: ""

        val prompter = ContextPrompter.prompter(file?.language?.displayName ?: "")

        logger.info("use prompter: ${prompter.javaClass}")

        val element = getElementToAction(project, editor) ?: return

        prompt += addAdditionPrompt(project, editor, element)
        prompter.initContext(getActionType(), prompt, file, project, caretModel?.offset ?: 0, element)

        sendToChatPanel(project, getActionType()) { panel: ChatCodingPanel, service ->
            val chatContext = ChatContext(
                chatCompletedPostAction(event, panel),
                prompt,
                suffixText
            )

            service.handlePromptAndResponse(panel, prompter, chatContext, newChatContext = true)
        }
    }

    fun commentPrefix(element: PsiElement): String {
        return LanguageCommenters.INSTANCE.forLanguage(element.language)?.lineCommentPrefix ?: "//"
    }

    private fun buildSelectionText(editor: Editor, caretModel: CaretModel?): @NlsSafe String {
        if (caretModel?.currentCaret?.selectedText.isNullOrEmpty()) {
            return ""
        }

//        val selection = editor.selectionModel
//        val document = editor.document
//        val firstLine: Int = document.getLineNumber(selection.selectionStart)
//        val lastLine: Int = document.getLineNumber(selection.selectionEnd)
//
//        val commentCodeWithLine = buildString {
//            for (i in firstLine..lastLine) {
//                val range: TextRange = DocumentUtil.getLineTextRange(document, i)
//                append("$i ${document.getText(range)}\n")
//            }
//        }
//
//        return commentCodeWithLine

        return caretModel?.currentCaret?.selectedText ?: ""
    }

    /**
     * After chat completion, we can provide some suggestions to the user.
     * For example, In issue: [#129](https://github.com/unit-mesh/auto-dev/issues/129), If our user doesn't provide any
     * refactor intention, we can provide some suggestions to the user.
     *
     * @param project The current project.
     * @param editor The editor that is currently in use.
     * @param element The PsiElement that is being completed.
     * @return A string representing the completion suggestion, or `null` if no suggestion is available.
     */
    open fun chatCompletionSuggestion(project: Project, editor: Editor, element: PsiElement): String? {
        return null
    }

    /**
     * Add additional prompt to the chat context.
     * Sample case:
     *
     * - Refactor: will collection code smell for the element
     *
     */
    open fun addAdditionPrompt(project: Project, editor: Editor, element: PsiElement): String = ""

    fun selectElement(elementToExplain: PsiElement, editor: Editor) {
        val startOffset = elementToExplain.textRange.startOffset
        val endOffset = elementToExplain.textRange.endOffset

        editor.selectionModel.setSelection(startOffset, endOffset)
    }
}