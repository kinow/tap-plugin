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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.tap4j.plugin.model.TestSetMap;

/**
 * Publishes TAP results in Jenkins builds.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class TapPublisher 
extends Notifier
{
	private final String testResults;
	
	private TapRemoteCallable remoteCallable;

	@DataBoundConstructor
	public TapPublisher( String testResults )
	{
		this.testResults = testResults;
	}
	
	/**
	 * @return the testResults
	 */
	public String getTestResults()
	{
		return testResults;
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.BuildStepCompatibilityLayer#getProjectAction(hudson.model.AbstractProject)
	 */
	@Override
	public Action getProjectAction( AbstractProject<?, ?> project )
	{
		return new TapProjectAction( project );
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform( AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener ) throws InterruptedException, IOException
	{
		
		TapResult tapResult = null;
		TapBuildAction buildAction = null;
		
		remoteCallable = new TapRemoteCallable(testResults, listener);
		
		List<TestSetMap> testSets = build.getWorkspace().act( remoteCallable );
		
		if ( remoteCallable.hasParserErrors() )
		{
			build.setResult( Result.UNSTABLE );
		}
		else
		{
			build.setResult( Result.SUCCESS );
		}
		
		tapResult = new TapResult(build, testSets);
		buildAction = new TapBuildAction( build, tapResult );
		build.addAction( buildAction );
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService()
	{
		return BuildStepMonitor.BUILD;
	}
	
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher>
	{
		public DescriptorImpl()
		{
			super( TapPublisher.class );
			load();
		}
		
		@Override
		public String getDisplayName()
		{
			return "Publish TAP Results";
		}

		/* (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@Override
		public boolean isApplicable( @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType )
		{
			return true;
		}
		
	}

}
