/* 
 * The MIT License
 * 
 * Copyright (c) 2010 Bruno P. Kinoshita <http://www.kinoshita.eti.br>
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

import hudson.AbortException;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction.Data;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tap4j.model.TestSet;
import org.tap4j.parser.ParserException;
import org.tap4j.parser.Tap13YamlParser;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapPublisher extends TestDataPublisher
{
	private final String testResults;

	private final Tap13YamlParser parser = new Tap13YamlParser();

	@DataBoundConstructor
	public TapPublisher(String testResults)
	{
		this.testResults = testResults;
	}

	public static class DescriptorImpl extends Descriptor<TestDataPublisher>
	{

		public DescriptorImpl()
		{
			super(TapPublisher.class);
			load();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName()
		{
			return "Publish TAP Results";
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * hudson.tasks.junit.TestDataPublisher#getTestData(hudson.model.AbstractBuild
	 * , hudson.Launcher, hudson.model.BuildListener,
	 * hudson.tasks.junit.TestResult)
	 */
	@Override
	public Data getTestData( AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener, TestResult testResult ) throws IOException,
			InterruptedException
	{
		File baseDir = null;
		String[] fileNames = null;

		try
		{
			baseDir = new File(build.getWorkspace().getRemote());
			final FileSet fs = Util.createFileSet(baseDir, testResults);
			final DirectoryScanner ds = fs.getDirectoryScanner();

			fileNames = ds.getIncludedFiles();
		} catch (Exception e)
		{
			e.printStackTrace(listener.fatalError(e.getMessage()));
			throw new AbortException(e.getMessage());
		}

		for (String fileName : fileNames)
		{
			try
			{
				TestSet testSet = parser.parseFile(new File(baseDir, fileName));
			} catch (ParserException pe)
			{
				pe.printStackTrace(listener.getLogger());
			}
		}

		return null;
	}

}
