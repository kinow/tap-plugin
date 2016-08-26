package org.tap4j.plugin.issue21456;

import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.tap4j.plugin.TapPublisher;
import org.tap4j.plugin.TapTestResultAction;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;

public class TestIssue21456 {

    public @Rule JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testDurationMs() throws IOException, InterruptedException, ExecutionException {
        final FreeStyleProject project = jenkins.createFreeStyleProject("tap-bug-21456");

        final String tap = "1..2\n" +
                "ok 1 - Input file opened\n" +
                "not ok 2 - First line of the input valid\n" +
                "  ---\n" +
                "    duration_ms: 100660.00\n" +
                "  ...\n";

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher arg1,
                    BuildListener arg2) throws InterruptedException, IOException {
                build.getWorkspace().child("result.tap").write(tap,"UTF-8");
                return true;
            }
        });

        final String command = "exit 255";

        project.getBuildersList().add(new Shell(command));

        final TapPublisher publisher = new TapPublisher(
                "result.tap", // testResults
                true, // failIfNoResults
                true, // failedTestsMarkBuildAsFailure
                true, // outputTapToConsole
                true, // enableSubtests
                true, // discardOldReports
                true, // todoIsFailure
                true, // includeCommentDiagnostics
                true, // validateNumberOfTests
                false, // planRequired
                true, // verbose
                false, // showOnlyFailures
                false, // stripSingleParents
                false, // flattenTapResult
                true); // skipIfBuildNotOk <-- that's the one we are testing here
        project.getPublishersList().add(publisher);
        project.save();
        FreeStyleBuild build = (FreeStyleBuild) project.scheduleBuild2(0).get();

        TapTestResultAction action = build.getAction(TapTestResultAction.class);

        assertNull("Not supposed to have a TAP action. Should have skipped a failed build!", action);
    }

}
