package org.tap4j.plugin.removeyamlifcorrupted;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.tap4j.plugin.TapPublisher;
import org.tap4j.plugin.TapResult;
import org.tap4j.plugin.TapTestResultAction;

/**
 * Tests for remove corrupted yaml configuration option.
 *
 * @author Jakub Podlesak
 */
public class TestRemoveYamlIfCorrupted {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testYamlStripped() throws IOException, InterruptedException, ExecutionException {

        final String tap = "1..1\n"
                + "not ok 1 '0' should not be in window anymore\n"
                + "  ---\n"
                + "    type:    AssertionError\n"
                + "    message: '0' should not be in window anymore\n"
                + "    code:    ~\n"
                + "    errno:   ~\n"
                + "    file:    /workdir/npm-module/test/window/frame.js\n"
                + "    line:    416\n"
                + "    column:  10\n"
                + "    stack:\n"
                + "      - |\n"
                + "        Object.remove frame (/workdir/npm-module/test/window/frame.js:416:10)\n"
                + "      - |\n"
                + "        Object.<anonymous> (/workdir/npm-module/node_modules/nodeunit/lib/core.js:236:16)\n"
                + "      - |\n"
                + "        Object.<anonymous> (/workdir/npm-module/node_modules/nodeunit/lib/core.js:236:16)\n"
                + "      - |\n"
                + "        /workdir/npm-module/node_modules/nodeunit/lib/core.js:236:16\n"
                + "      - |\n"
                + "        Object.runTest (/workdir/npm-module/node_modules/nodeunit/lib/core.js:70:9)\n"
                + "      - |\n"
                + "        /workdir/npm-module/node_modules/nodeunit/lib/core.js:118:25\n"
                + "      - |\n"
                + "        /workdir/npm-module/node_modules/nodeunit/deps/async.js:513:13\n"
                + "      - |\n"
                + "        iterate (/workdir/npm-module/node_modules/nodeunit/deps/async.js:123:13)\n"
                + "      - |\n"
                + "        /workdir/npm-module/node_modules/nodeunit/deps/async.js:134:25\n"
                + "      - |\n"
                + "        /workdir/npm-module/node_modules/nodeunit/deps/async.js:515:17\n"
                + "      - |\n"
                + "        Immediate.<anonymous> (/workdir/npm-module/node_modules/nodeunit/lib/types.js:146:17)\n"
                + "      - |\n"
                + "        runCallback (timers.js:693:18)\n"
                + "      - |\n"
                + "        tryOnImmediate (timers.js:664:5)\n"
                + "      - |\n"
                + "        process.processImmediate (timers.js:646:5)\n"
                + "    wanted:  true\n"
                + "    found:   false\n"
                + "  ...";

        _test("do-not-remove-corrupted-yaml", false, tap, 0);
        _test("remove-corrupted-yaml", true, tap, 1);
    }

    private void _test(String projectName, boolean removeYamlIfCorrupted, final String tap, int expectedTotal) throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, projectName);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher arg1,
                    BuildListener arg2) throws InterruptedException, IOException {
                build.getWorkspace().child("result.tap").write(tap, "UTF-8");
                return true;
            }
        });

        TapPublisher publisher = new TapPublisher(
                "result.tap", // test results
                true, // failIfNoResults
                true, // failedTestsMarkBuildAsFailure
                false, // outputTapToConsole
                true, // enableSubtests
                true, // discardOldReports
                true, // todoIsFailure
                true, // includeCommentDiagnostics
                true, // validateNumberOfTests
                true, // planRequired
                false, // verbose
                true, // showOnlyFailures
                false, // stripSingleParents
                true, // flattenTapResult
                removeYamlIfCorrupted,
                false); //skipIfBuildNotOk

        project.getPublishersList().add(publisher);
        project.save();
        FreeStyleBuild build = (FreeStyleBuild) project.scheduleBuild2(0).get();

        TapTestResultAction action = build.getAction(TapTestResultAction.class);
        TapResult testResult = action.getTapResult();

        assertEquals(expectedTotal, testResult.getTotal());
    }
}
