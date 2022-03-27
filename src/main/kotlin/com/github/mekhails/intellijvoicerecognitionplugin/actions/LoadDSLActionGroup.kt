package com.github.mekhails.intellijvoicerecognitionplugin.actions

import com.github.mekhails.intellijvoicerecognitionplugin.configuration.ConfigurationService
import com.intellij.openapi.actionSystem.ActionGroupUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAware

class LoadDSLActionGroup : CollapsiblePopupActionGroup(), DumbAware {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val loadedDSLConfigurationFiles = ConfigurationService.getInstance().refreshAndGetConfigurationFiles()
        val openLoadedDSLActions = loadedDSLConfigurationFiles.map { OpenDSLAction(it) }

        return (openLoadedDSLActions + listOf<AnAction>(Separator.getInstance(), LoadDSLFromFileAction())).toTypedArray()
    }
}

open class CollapsiblePopupActionGroup : DefaultActionGroup() {
    @Volatile
    private var cachedIsPopup = true;

    override fun isPopup() = cachedIsPopup

    override fun update(e: AnActionEvent) {
        val size: Int = ActionGroupUtil.getActiveActions(this, e).take(2).size()
        e.presentation.isEnabledAndVisible = size > 0
        cachedIsPopup = size > 1
    }

    override fun disableIfNoVisibleChildren() = false
}