package com.github.mekhails.intellijvoicerecognitionplugin.services

import com.intellij.openapi.project.Project
import com.github.mekhails.intellijvoicerecognitionplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
