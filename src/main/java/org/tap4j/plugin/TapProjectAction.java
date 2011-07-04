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
import hudson.model.AbstractProject;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapProjectAction 
extends AbstractTapProjectAction
{

	private AbstractProject<?, ?> project;
	
	public TapProjectAction( AbstractProject<?, ?> project )
	{
		this.project = project;
	}
	
	public TapBuildAction getLastBuildAction()
	{
		TapBuildAction action = null;
		final AbstractBuild<?, ?> lastBuild = this.getLastBuildWithTap();
		
		if ( lastBuild != null )
		{
			action = lastBuild.getAction( TapBuildAction.class );
		}
		
		return action;
	}

	/**
	 * @return
	 */
	private AbstractBuild<?, ?> getLastBuildWithTap()
	{
		AbstractBuild<?, ?> lastBuild = this.project.getLastBuild();
		while ( lastBuild != null && lastBuild.getAction( TapBuildAction.class) == null )
		{
			lastBuild = lastBuild.getPreviousBuild();
		}
		return lastBuild;
	}
	
	public void doIndex(
			final StaplerRequest request, 
			final StaplerResponse response ) 
	throws IOException
	{
		AbstractBuild<?, ?> lastBuild = this.getLastBuildWithTap();
		if ( lastBuild == null )
		{
			response.sendRedirect2("nodata");
		}
		else
		{
			int buildNumber = lastBuild.getNumber();
			response.sendRedirect2( String.format("../%d/%s", buildNumber, TapBuildAction.URL_NAME));
		}
	}
	
}
