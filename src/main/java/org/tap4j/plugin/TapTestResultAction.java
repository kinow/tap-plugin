/*
 * The MIT License
 *
 * Copyright (c) 2012 Bruno P. Kinoshita
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tap4j.plugin;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Job;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.Messages;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.export.Exported;
import org.tap4j.plugin.model.TapStreamResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class TapTestResultAction
        implements StaplerProxy, SimpleBuildStep.LastBuildAction, HealthReportingAction, RunAction2 {

    public transient Run<?,?> run;
    @Deprecated
    public transient AbstractBuild<?,?> owner;

    private TapResult tapResult;


    /**
     * @param r
     * @param tapResult
     */
    @Deprecated
    protected TapTestResultAction(AbstractBuild<?, ?> owner, TapResult tapResult) {
        this((Run) owner, tapResult);
    }

    /**
     * @param r
     * @param tapResult
     */
    protected TapTestResultAction(Run r, TapResult tapResult) {
        setRunAndOwner(r);

        this.tapResult = tapResult;
    }

    /**
     * @return the tapResult
     */
    public TapResult getTapResult() {
        return tapResult;
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getFailCount()
     */
    @Exported(visibility = 2)
    public int getFailCount() {
        return tapResult.getFailed();
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getTotalCount()
     */
    @Exported(visibility = 2)
    public int getTotalCount() {
        return tapResult.getTotal();
    }
    
    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getSkipCount()
     */
    @Exported(visibility = 2)
    public int getSkipCount() {
        return tapResult.getSkipped();
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getFailedTests()
     */
    public List<CaseResult> getFailedTests() {
        return Collections.emptyList();
    }
    
    /* 
     * (non-Javadoc)
     * @see org.kohsuke.stapler.StaplerProxy#getTarget()
     */
    public Object getTarget() {
        return getResult();
    }
    
    public TapStreamResult getResult() {
        return new TapStreamResult(owner, tapResult);
    }

    /* (non-Javadoc)
     * @see hudson.tasks.test.AbstractTestResultAction#getUrlName()
     */
    @Override
    @Exported(visibility = 2)
    public String getUrlName() {
        return "tapTestReport";
    }


    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    /* (non-Javadoc)
         * @see hudson.tasks.test.AbstractTestResultAction#getDisplayName()
         */
    @Override
    public String getDisplayName() {
        return "TAP Test Results";
    }
    
    /*
     * (non-Javadoc)
     * @see jenkins.tasks.SimpleBuildStep#getProjectActions()
     */
    @Override
    public Collection<? extends Action> getProjectActions() {
        Job<?,?> job = run.getParent();
        if (!Util.filter(job.getActions(), TapProjectAction.class).isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.singleton(new TapProjectAction(job));
    }

    @Override
    public HealthReport getBuildHealth() {
        final double scaleFactor = 1.0;
        final int totalCount = getTotalCount();
        final int failCount = getFailCount();
        int score = (totalCount == 0)
                ? 100
                : (int) (100.0 * Math.max(0.0, Math.min(1.0, 1.0 - (scaleFactor * failCount) / totalCount)));
        Localizable description, displayName = Messages._AbstractTestResultAction_getDisplayName();
        if (totalCount == 0) {
            description = Messages._AbstractTestResultAction_zeroTestDescription(displayName);
        } else {
            description = Messages._AbstractTestResultAction_TestsDescription(displayName, failCount, totalCount);
        }
        return new HealthReport(score, description);
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        setRunAndOwner(r);
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        setRunAndOwner(r);
    }

    private void setRunAndOwner(Run<?, ?> r) {
        this.run = r;
        this.owner = r instanceof AbstractBuild ? (AbstractBuild<?,?>) r : null;
    }

    void mergeResult(TapResult additionalResult) {
        TapStreamResult original = getResult();
        original.merge(additionalResult);
        setFromTapStreamResult(original);
    }

    private void setFromTapStreamResult(TapStreamResult result) {
        this.tapResult = result.getTapResult();
    }
}