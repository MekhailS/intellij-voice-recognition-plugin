package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.github.mekhails.intellijvoicerecognitionplugin.searcher.ActionSearcher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.runBackgroundableTask

class CallActionThroughSearcher : AnAction() {
    override fun update(e: AnActionEvent) {
        if (e.project == null)
            e.presentation.isEnabledAndVisible = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        runBackgroundableTask("Rename file", e.project, true) { progressIndicator ->
            val operationSearcher = ActionSearcher(e.project, e.dataContext.getData(CommonDataKeys.EDITOR))
            operationSearcher.searchAction("Commit", progressIndicator)?.invoke()
        }
    }
}