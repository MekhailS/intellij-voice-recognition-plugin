package com.github.mekhails.intellijvoicerecognitionplugin.searcher

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
        val matchedAction = actionSearchEverywhereContributor.search(pattern, progressIndicator)
            .maxByOrNull { it.matchingDegree } ?: return null

        return { actionSearchEverywhereContributor.processSelectedItem(matchedAction, 0, pattern) }
    }
}
