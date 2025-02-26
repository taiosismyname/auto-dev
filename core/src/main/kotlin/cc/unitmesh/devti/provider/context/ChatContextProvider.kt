package cc.unitmesh.devti.provider.context

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface ChatContextProvider {
    @RequiresReadLock
    fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean

    @RequiresBackgroundThread
    fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem>

    companion object {
        val EP_NAME = ExtensionPointName<ChatContextProvider>("cc.unitmesh.chatContextProvider")

        fun collectChatContextList(
            project: Project,
            chatCreationContext: ChatCreationContext,
        ): List<ChatContextItem> {
            val elements = mutableListOf<ChatContextItem>()

            val chatContextProviders = EP_NAME.extensionList
            for (provider in chatContextProviders) {
                try {
                    val applicable = provider.isApplicable(project, chatCreationContext)
                    if (applicable) {
                        elements.addAll(provider.collect(project, chatCreationContext))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            elements.addAll(chatCreationContext.extraItems)
            return elements.distinctBy { it.text }
        }
    }
}
