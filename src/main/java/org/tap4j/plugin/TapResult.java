/*
 * The MIT License
 *
 * Copyright (c) <2011> <Bruno P. Kinoshita>
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

import hudson.model.AbstractBuild;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.tap4j.model.Directive;
import org.tap4j.model.TestResult;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.DiagnosticUtil;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapResult 
implements Serializable
{

	private static final long serialVersionUID = 4343399327336076951L;

	private AbstractBuild<?, ?> build;
	private List<TestSetMap> testSets;
	private int failed = 0;
	private int passed = 0;
	private int skipped = 0;

	public TapResult(AbstractBuild<?, ?> build, List<TestSetMap> testSets)
	{
		this.build = build;
		this.testSets = testSets;
	}

	public void updateStats()
	{
		for (TestSetMap testSet : testSets)
		{
			TestSet realTestSet = testSet.getTestSet();
			List<TestResult> testResults = realTestSet.getTestResults();
			for (TestResult testResult : testResults)
			{
				if (isSkipped(testResult))
				{
					skipped += 1;
				} 
				else if (isFailure(testResult))
				{
					failed += 1;
				}
				else
				{
					passed += 1;
				}
			}
		}
	}

	public AbstractBuild<?, ?> getOwner()
	{
		return this.build;
	}

	public List<TestSetMap> getTestSets()
	{
		return this.testSets;
	}

	public boolean isEmptyTestSet()
	{
		return this.testSets.size() <= 0;
	}

	public int getFailed()
	{
		return this.failed;
	}

	public int getSkipped()
	{
		return this.skipped;
	}

	public int getPassed()
	{
		return this.passed;
	}

	private boolean isSkipped( TestResult testResult )
	{
		boolean r = false;
		Directive directive = testResult.getDirective();
		if (directive != null
				&& directive.getDirectiveValue() == DirectiveValues.SKIP)
		{
			r = true;
		}
		return r;
	}

	private boolean isFailure( TestResult testResult )
	{
		boolean r = false;
		Directive directive = testResult.getDirective();
		StatusValues status = testResult.getStatus();
		if (directive != null
				&& directive.getDirectiveValue() == DirectiveValues.SKIP)
		{
			r = true;
		} else if (status != null && status == StatusValues.NOT_OK)
		{
			r = true;
		}
		return r;
	}

	public String createDiagnosticTable( Map<String, Object> diagnostic )
	{
		return DiagnosticUtil.createDiagnosticTable(diagnostic);
	}

}
