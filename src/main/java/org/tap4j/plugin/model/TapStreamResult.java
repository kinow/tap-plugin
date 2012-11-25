/*
 * The MIT License
 *
 * Copyright (c) <2012> <Bruno P. Kinoshita>
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
package org.tap4j.plugin.model;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.TapResult;
import org.tap4j.util.StatusValues;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class TapStreamResult extends TabulatedResult {

	private static final long serialVersionUID = 8337146933697574082L;
	private final AbstractBuild<?, ?> owner;
	private final List<TestResult> children = new ArrayList<TestResult>();
	private final TapResult tapResult;

	public TapStreamResult(AbstractBuild<?, ?> owner, TapResult tapResult) {
		this.owner = owner;
		this.tapResult = tapResult;
		for(TestSetMap tsm : tapResult.getTestSets()) {
			TestSet ts = tsm.getTestSet();
			for(org.tap4j.model.TestResult tr : ts.getTestResults()) {
				this.children.add(new TapTestResultResult(owner, tsm, tr, this.tapResult.getTodoIsFailure()));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see hudson.model.ModelObject#getDisplayName()
	 */
	public String getDisplayName() {
		return "TAP Stream results";
	}

	/* (non-Javadoc)
	 * @see hudson.tasks.test.TestObject#getOwner()
	 */
	@Override
	public AbstractBuild<?, ?> getOwner() {
		return owner;
	}

	/* (non-Javadoc)
	 * @see hudson.tasks.test.TestObject#getParent()
	 */
	@Override
	public TestObject getParent() {
		return null;
	}

	/* (non-Javadoc)
	 * @see hudson.tasks.test.TestObject#findCorrespondingResult(java.lang.String)
	 */
	@Override
	public TestResult findCorrespondingResult(String id) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.test.TabulatedResult#getChildren()
	 */
	@Override
	public Collection<? extends TestResult> getChildren() {
		return children;
	}

	/* (non-Javadoc)
	 * @see hudson.tasks.test.TabulatedResult#hasChildren()
	 */
	@Override
	public boolean hasChildren() {
		return children.size() > 0;
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.test.AbstractTestResultAction#getFailCount()
	 */
	@Override
	@Exported(visibility = 2)
	public int getFailCount() {
		return tapResult.getFailed();
	}

	/* (non-Javadoc)
	 * @see hudson.tasks.test.AbstractTestResultAction#getTotalCount()
	 */
	@Override
	@Exported(visibility = 2)
	public int getTotalCount() {
		return tapResult.getTotal();
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.test.AbstractTestResultAction#getSkipCount()
	 */
	@Override
	@Exported(visibility = 2)
	public int getSkipCount() {
		return tapResult.getSkipped();
	}

	/* (non-Javadoc)
	 * @see hudson.tasks.test.AbstractTestResultAction#getFailedTests()
	 */
	@Override
	public List<CaseResult> getFailedTests() {
		//throw new AssertionError("Not supposed to be called");
		return Collections.emptyList();
	}
	
	public List<TestResult> getFailedTests2() {
		List<TestResult> failedTests = new ArrayList<TestResult>();
		if(tapResult != null && tapResult.getTestSets().size() > 0) {
			for(TestSetMap tsm : tapResult.getTestSets()) {
				TestSet ts = tsm.getTestSet();
				for(org.tap4j.model.TestResult tr : ts.getTestResults()) {
					if(tr.getStatus() == StatusValues.NOT_OK) {
						failedTests.add(new TapTestResultResult(owner, tsm, tr, this.tapResult.getTodoIsFailure()));
					}
				}
			}
		}
		return failedTests;
	}
	
	@Override
    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
    	TapTestResultResult tr = getTapTestResultResult(name);
    	if (tr != null) {
            return tr;
    	} else {
            return super.getDynamic(name, req, rsp);
    	}
    }

	/**
	 * @param name
	 * @return
	 */
	private TapTestResultResult getTapTestResultResult(String name) {
		String fileName = name.substring(0, name.lastIndexOf("-"));
		String testNumber = name.substring(name.lastIndexOf("-")+1);
		for(TestSetMap tsm : tapResult.getTestSets()) {
			if(tsm.getFileName().equals(fileName)) {
				TestSet ts = tsm.getTestSet();
				org.tap4j.model.TestResult desired = ts.getTestResult(Integer.parseInt(testNumber));
				return new TapTestResultResult(owner, tsm, desired, this.tapResult.getTodoIsFailure());
			}
		}
		return null;
	}

}
