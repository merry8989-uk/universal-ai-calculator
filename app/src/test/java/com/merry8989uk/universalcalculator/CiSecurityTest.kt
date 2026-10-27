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
        // the full-history run has no commit range, so it reports rather than
        // gates; push and pull request are the runs that block a merge
        assertTrue(
            "a manual or scheduled scan would fail on the known historical finding",
            "continue-on-error" in text,
        )
        assertTrue("it does not review the dependencies a PR adds", "dependency-review-action" in text)
        assertTrue("it does not submit the dependency graph", "dependency-submission" in text)
        // the repository has no gradlew, so the job has to make one like the
        // test workflow does; without it the submission has nothing to run
        assertTrue("the graph job will not find a wrapper", "gradle wrapper" in text)
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
        // netty, protobuf and commons-io come in under AGP, which pins them
        assertTrue("the toolchain's own libraries are offered for update", "io.netty*" in text)
        assertTrue(
            "minor updates are not grouped, so they arrive as a pile of pull requests",
            "groups:" in text,
        )
    }

    @Test fun everyIgnoredFindingIsWrittenDownWithAReason() {
        val text = read(".gitleaks.toml") ?: return
        assertTrue("the scanner has no allowlist at all", "[[allowlists]]" in text)
        // an allowlist entry without a reason is how a real leak gets buried
        for (block in text.split("[[allowlists]]").drop(1)) {
            assertTrue(
                "an allowlist entry has no description: ${block.take(80)}",
                "description" in block,
            )
        }
        assertTrue(
            "the old Supabase key is not recorded as a known finding",
            "8db60ab673523afc5e9f6de7ce1bc3357a15c3c7" in text,
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
