package com.github.mekhails.intellijvoicerecognitionplugin.searcher

import com.github.mekhails.intellijvoicerecognitionplugin.configuration.ELEVEN_ACTION_STRING
import com.intellij.ide.BrowserUtil
import com.intellij.ide.actions.searcheverywhere.ActionSearchEverywhereContributor
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project

class ActionSearcher(project: Project?, editor: Editor?) {
    private val actionSearchEverywhereContributor = invokeAndWaitIfNeeded {
        ActionSearchEverywhereContributor(project, editor?.component, editor)
    }

    fun searchAction(pattern: String, progressIndicator: ProgressIndicator): (() -> Unit)? {
        val bundledAction = findInBundledActions(pattern)
        if (bundledAction != null)
            return bundledAction

        val matchedAction = actionSearchEverywhereContributor.search(pattern, progressIndicator)
            .maxByOrNull { it.matchingDegree } ?: return null

        return { actionSearchEverywhereContributor.processSelectedItem(matchedAction, 0, pattern) }
    }

    private fun findInBundledActions(pattern: String): (() -> Unit)? {
        if (pattern == ELEVEN_ACTION_STRING)
            return { BrowserUtil.browse("https://www.youtube.com/watch?v=NMS2VnDveP8") }

        return null
    }
}
