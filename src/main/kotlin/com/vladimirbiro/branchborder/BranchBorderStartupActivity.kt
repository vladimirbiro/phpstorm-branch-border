package com.vladimirbiro.branchborder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class BranchBorderStartupActivity : ProjectActivity {

    private val log = Logger.getInstance(BranchBorderStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        log.info("BranchBorderStartupActivity executing for project: ${project.name}")

        // Delay slightly to ensure window is ready
        kotlinx.coroutines.delay(500)

        BranchBorderService.getInstance(project).initialize()
    }
}
