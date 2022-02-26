package com.github.mekhails.intellijvoicerecognitionplugin.services

import com.intellij.ide.actions.searcheverywhere.ActionSearchEverywhereContributor
import com.intellij.ide.util.gotoByName.GotoActionModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ActionSearcherService(private val project: Project) {
    private val actionSearchEverywhereContributor: ActionSearchEverywhereContributor by lazy {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        ActionSearchEverywhereContributor(project, null, editor)
    }

    fun searchAction(pattern: String, progressIndicator: ProgressIndicator): AnAction? {
        val matchedActionValue = actionSearchEverywhereContributor.search(pattern, progressIndicator, 1)
            .items.firstOrNull()?.value ?: return null

        if (matchedActionValue !is GotoActionModel.ActionWrapper) return null
        return matchedActionValue.action
    }
}