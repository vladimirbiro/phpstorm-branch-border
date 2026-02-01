package com.vladimirbiro.branchborder

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager

class GitBranchListener(
    private val project: Project,
    private val onBranchChanged: (String?) -> Unit
) : GitRepositoryChangeListener, Disposable {

    private val log = Logger.getInstance(GitBranchListener::class.java)
    private var lastBranch: String? = null
    private val connection = project.messageBus.connect(this)

    fun start() {
        connection.subscribe(GitRepository.GIT_REPO_CHANGE, this)

        // Initial check
        val currentBranch = getCurrentBranch()
        lastBranch = currentBranch
        onBranchChanged(currentBranch)
        log.info("GitBranchListener started, initial branch: $currentBranch")
    }

    override fun repositoryChanged(repository: GitRepository) {
        val currentBranch = getCurrentBranch()
        if (currentBranch != lastBranch) {
            log.info("Branch changed: $lastBranch -> $currentBranch")
            lastBranch = currentBranch
            onBranchChanged(currentBranch)
        }
    }

    fun getCurrentBranch(): String? {
        val repositoryManager = GitRepositoryManager.getInstance(project)
        val repositories = repositoryManager.repositories

        if (repositories.isEmpty()) {
            return null
        }

        // Use the first repository (primary)
        val repo = repositories.first()
        return repo.currentBranch?.name
    }

    override fun dispose() {
        // Connection is auto-disposed via parent disposable
    }
}
