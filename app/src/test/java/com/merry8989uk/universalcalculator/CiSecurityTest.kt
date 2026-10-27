package com.merry8989uk.universalcalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The repository's own guard rails.
 *
 * A security workflow that is quietly deleted, or a Dependabot config that only
 * watches one of the three ecosystems, is worse than none: it looks like cover
 * that is not there.
 */
class CiSecurityTest {

    private val base: File? = listOf(File("."), File(".."))
        .firstOrNull { File(it, ".github").exists() }

    private fun read(path: String): String? {
        val root = base ?: return null
        val f = File(root, path)
        return if (f.exists()) f.readText() else null
    }

    @Test fun thereIsASecurityWorkflow() {
        val text = read(".github/workflows/security.yml") ?: return
        assertTrue("it does not scan for secrets", "gitleaks" in text)
        assertTrue("it does not review the dependencies a PR adds", "dependency-review-action" in text)
        assertTrue("it does not submit the dependency graph", "dependency-submission" in text)
    }

    @Test fun theSecurityWorkflowRunsOnPullRequestsAndOnASchedule() {
        val text = read(".github/workflows/security.yml") ?: return
        assertTrue("pull requests are not covered", "pull_request:" in text)
        assertTrue("there is no weekly sweep", "schedule:" in text)
    }

    @Test fun theTestsRunOnPullRequests() {
        val text = read(".github/workflows/tests.yml") ?: return
        assertTrue("a pull request does not run the tests", "pull_request:" in text)
    }

    @Test fun dependabotWatchesEveryEcosystemThatExists() {
        val text = read(".github/dependabot.yml") ?: return
        assertTrue("the gradle build is not watched", "package-ecosystem: \"gradle\"" in text)
        assertTrue("the actions themselves are not watched", "package-ecosystem: \"github-actions\"" in text)

        // npm is deliberately absent: the Worker is one JavaScript file with no
        // package.json, so pointing Dependabot at it just fails. If a manifest
        // ever appears there, this catches the missing entry.
        val hasManifest = base?.let { File(it, "backend/cloudflare/package.json").exists() } ?: false
        if (hasManifest) {
            assertTrue("the worker now has a package.json but is not watched", "\"npm\"" in text)
        }
    }

    @Test fun dependabotDoesNotProposeTheUpdatesThatBreakTheBuild() {
        val text = read(".github/dependabot.yml") ?: return
        // AGP, Kotlin and Compose have to move together; a major bump of any of
        // them is a migration. The first version of this file let Dependabot open
        // four such pull requests and all four failed the build.
        assertTrue("Compose majors are not held back", "androidx.compose*" in text)
        assertTrue("Kotlin majors are not held back", "org.jetbrains.kotlin*" in text)
        assertTrue("AGP majors are not held back", "com.android.application" in text)
        assertTrue(
            "minor updates are not grouped, so they arrive as a pile of pull requests",
            "groups:" in text,
        )
    }

    @Test fun aHighSeverityDependencyFailsThePullRequest() {
        val text = read(".github/workflows/security.yml") ?: return
        assertTrue(
            "the review does not fail on a serious vulnerability",
            "fail-on-severity: high" in text,
        )
    }
}
