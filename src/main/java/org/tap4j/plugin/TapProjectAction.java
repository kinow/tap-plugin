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
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.tap4j.plugin.util.GraphHelper;

/**
 * A TAP Project action, with a graph and a list of builds.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapProjectAction extends AbstractTapProjectAction
{

	private AbstractProject<?, ?> project;

	/**
	 * Used to figure out if we need to regenerate the graphs or not. Only used
	 * in newGraphNotNeeded() method. Key is the request URI and value is the
	 * number of builds for the project.
	 */
	private transient Map<String, Integer> requestMap = new HashMap<String, Integer>();

	public TapProjectAction(AbstractProject<?, ?> project)
	{
		this.project = project;
	}

	public AbstractProject<?, ?> getProject()
	{
		return this.project;
	}

	protected Class<TapBuildAction> getBuildActionClass()
	{
		return TapBuildAction.class;
	}

	public TapBuildAction getLastBuildAction()
	{
		TapBuildAction action = null;
		final AbstractBuild<?, ?> lastBuild = this.getLastBuildWithTap();

		if (lastBuild != null)
		{
			action = lastBuild.getAction(TapBuildAction.class);
		}

		return action;
	}

	/**
	 * @return
	 */
	private AbstractBuild<?, ?> getLastBuildWithTap()
	{
		AbstractBuild<?, ?> lastBuild = this.project.getLastBuild();
		while (lastBuild != null
				&& lastBuild.getAction(TapBuildAction.class) == null)
		{
			lastBuild = lastBuild.getPreviousBuild();
		}
		return lastBuild;
	}

	public void doIndex( final StaplerRequest request,
			final StaplerResponse response ) throws IOException
	{
		AbstractBuild<?, ?> lastBuild = this.getLastBuildWithTap();
		if (lastBuild == null)
		{
			response.sendRedirect2("nodata");
		} else
		{
			int buildNumber = lastBuild.getNumber();
			response.sendRedirect2(String.format("../%d/%s", buildNumber,
					TapBuildAction.URL_NAME));
		}
	}

	/**
	 * Generates the graph that shows test pass/fail ratio
	 * 
	 * @param req
	 *            -
	 * @param rsp
	 *            -
	 * @throws IOException
	 *             -
	 */
	public void doGraph( final StaplerRequest req, StaplerResponse rsp )
			throws IOException
	{
		if (newGraphNotNeeded(req, rsp))
		{
			return;
		}

		final DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

		populateDataSetBuilder(dataSetBuilder);
		new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight())
		{
			protected JFreeChart createGraph()
			{
				return GraphHelper.createChart(req, dataSetBuilder.build());
			}
		}.doPng(req, rsp);
	}

	public void doGraphMap( final StaplerRequest req, StaplerResponse rsp )
			throws IOException
	{
		if (newGraphNotNeeded(req, rsp))
		{
			return;
		}

		final DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

		// TODO: optimize by using cache
		populateDataSetBuilder(dataSetBuilder);
		new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight())
		{
			protected JFreeChart createGraph()
			{
				return GraphHelper.createChart(req, dataSetBuilder.build());
			}
		}.doMap(req, rsp);
	}

	/**
	 * Returns <code>true</code> if there is a graph to plot.
	 * 
	 * @return Value for property 'graphAvailable'.
	 */
	public boolean isGraphActive()
	{
		AbstractBuild<?, ?> build = getProject().getLastBuild();
		// in order to have a graph, we must have at least two points.
		int numPoints = 0;
		while (numPoints < 2)
		{
			if (build == null)
			{
				return false;
			}
			if (build.getAction(getBuildActionClass()) != null)
			{
				numPoints++;
			}
			build = build.getPreviousBuild();
		}
		return true;
	}

	/**
	 * If number of builds hasn't changed and if checkIfModified() returns true,
	 * no need to regenerate the graph. Browser should reuse it's cached image
	 * 
	 * @param req
	 * @param rsp
	 * @return true, if new image does NOT need to be generated, false otherwise
	 */
	private boolean newGraphNotNeeded( final StaplerRequest req,
			StaplerResponse rsp )
	{
		Calendar t = getProject().getLastCompletedBuild().getTimestamp();
		Integer prevNumBuilds = requestMap.get(req.getRequestURI());
		int numBuilds = getProject().getBuilds().size();

		// change null to 0
		prevNumBuilds = prevNumBuilds == null ? 0 : prevNumBuilds;
		if (prevNumBuilds != numBuilds)
		{
			requestMap.put(req.getRequestURI(), numBuilds);
		}

		if (requestMap.keySet().size() > 10)
		{
			// keep map size in check
			requestMap.clear();
		}

		if (prevNumBuilds == numBuilds && req.checkIfModified(t, rsp))
		{
			/*
			 * checkIfModified() is after '&&' because we want it evaluated only
			 * if number of builds is different
			 */
			return true;
		}

		return false;
	}

	protected void populateDataSetBuilder(
			DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataset )
	{

		for (AbstractBuild<?, ?> build = getProject().getLastBuild(); build != null; build = build
				.getPreviousBuild())
		{
			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(
					build);
			TapBuildAction action = build.getAction(getBuildActionClass());
			if (action != null)
			{
				TapResult report = action.getResult();
				report.updateStats();

				dataset.add(report.getPassed(), "Passed",
						label);
				dataset.add(report.getFailed(), "Failed",
						label);
				dataset.add(report.getSkipped(),
						"Skipped", label);
			}
		}
	}

	/**
	 * Getter for property 'graphWidth'.
	 * 
	 * @return Value for property 'graphWidth'.
	 */
	public int getGraphWidth()
	{
		return 500;
	}

	/**
	 * Getter for property 'graphHeight'.
	 * 
	 * @return Value for property 'graphHeight'.
	 */
	public int getGraphHeight()
	{
		return 200;
	}

}
