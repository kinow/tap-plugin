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
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.test.TestResultAggregator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Publishes TAP results in Jenkins builds.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class TapPublisher extends Recorder implements MatrixAggregatable {
	private final String testResults;
	private final Boolean failIfNoResults;
	private final Boolean failedTestsMarkBuildAsFailure;
	private final Boolean outputTapToConsole;
	private final Boolean enableSubtests;
	private final Boolean discardOldReports;
	private final Boolean todoIsFailure;

	@DataBoundConstructor
	public TapPublisher(String testResults,
			Boolean failIfNoResults, 
			Boolean failedTestsMarkBuildAsFailure, 
			Boolean outputTapToConsole,
			Boolean enableSubtests, 
			Boolean discardOldReports,
			Boolean todoIsFailure) {
		this.testResults = testResults;
		if(failIfNoResults == null) {
			this.failIfNoResults = Boolean.FALSE;
		} else {
			this.failIfNoResults = failIfNoResults;
		}
		if(failedTestsMarkBuildAsFailure == null) {
			this.failedTestsMarkBuildAsFailure = Boolean.FALSE;
		} else {
			this.failedTestsMarkBuildAsFailure = failedTestsMarkBuildAsFailure;
		}
		this.outputTapToConsole = outputTapToConsole;
		if(enableSubtests == null) {
			this.enableSubtests = Boolean.TRUE;
		} else {
			this.enableSubtests = enableSubtests;
		}
		if(discardOldReports == null) {
			this.discardOldReports = Boolean.FALSE;
		} else {
			this.discardOldReports = discardOldReports;
		}
		if(todoIsFailure == null) {
			this.todoIsFailure = Boolean.TRUE;
		} else {
			this.todoIsFailure = todoIsFailure;
		}
	}

	public Object readResolve() {
		String testResults = this.getTestResults();
		Boolean failIfNoResults = this.getFailIfNoResults();
		Boolean failedTestsMarkBuildAsFailure = this.getFailedTestsMarkBuildAsFailure();
		Boolean outputTapToConsole = this.getOutputTapToConsole();
		Boolean enableSubtests = this.getEnableSubtests();
		Boolean discardOldReports = this.getDiscardOldReports();
		Boolean todoIsFailure = this.getTodoIsFailure();
		if (failedTestsMarkBuildAsFailure == null) {
			failedTestsMarkBuildAsFailure = Boolean.FALSE;
		}
		if(failIfNoResults == null) {
			failIfNoResults = Boolean.FALSE;
		}
		if (outputTapToConsole == null) {
			outputTapToConsole = Boolean.FALSE;
		}
		if(enableSubtests == null) {
			enableSubtests = Boolean.TRUE;
		}
		if(discardOldReports == null) {
			discardOldReports = Boolean.FALSE;
		}
		if(todoIsFailure == null) {
			todoIsFailure = Boolean.TRUE;
		}
		return new TapPublisher(testResults, failIfNoResults, failedTestsMarkBuildAsFailure, outputTapToConsole, enableSubtests, discardOldReports, todoIsFailure);
	}

	/**
	 * @return the failIfNoResults
	 */
	public Boolean getFailIfNoResults() {
		return failIfNoResults;
	}
	
	/**
	 * @return the testResults
	 */
	public String getTestResults() {
		return testResults;
	}

	public Boolean getFailedTestsMarkBuildAsFailure() {
		return failedTestsMarkBuildAsFailure;
	}

	/**
	 * @return the outputTapToConsole
	 */
	public Boolean getOutputTapToConsole() {
		return outputTapToConsole;
	}
	
	/**
	 * @return the enableSubtests
	 */
	public Boolean getEnableSubtests() {
		return enableSubtests;
	}
	
	/**
	 * @return the discardOldReports
	 */
	public Boolean getDiscardOldReports() {
		return discardOldReports;
	}
	
	/**
	 * @return the todoIsFailure
	 */
	public Boolean getTodoIsFailure() {
		return todoIsFailure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * hudson.tasks.BuildStepCompatibilityLayer#getProjectAction(hudson.model
	 * .AbstractProject)
	 */
	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new TapProjectAction(project);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
	 * , hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		PrintStream logger = listener.getLogger();
		logger.println("TAP Reports Processing: START");
		logger.println("Looking for TAP results report in workspace using pattern: "
				+ this.testResults);

		FilePath[] reports = locateReports(build.getWorkspace(),
				this.testResults);

		if (reports.length == 0) {
			if(this.getFailIfNoResults()) {
				logger.println("Did not find any matching files. Setting build result to FAILURE.");
				build.setResult(Result.FAILURE);
				return Boolean.FALSE;
			} else {
				logger.println("Did not find any matching files.");
				// build can still continue
				return Boolean.TRUE;
			}
		}

		/*
		 * filter out the reports based on timestamps. See JENKINS-12187
		 */
		if(this.getDiscardOldReports()) {
			reports = checkReports(build, reports, logger);
		}

		boolean filesSaved = saveReports(build.getWorkspace(), getTapReportDirectory(build), reports, logger);
		if (!filesSaved) {
			logger.println("Failed to save TAP reports");
			return Boolean.TRUE;
		}

		TapResult testResult = null; 
		try {
			testResult = loadResults(build, logger);
			testResult.tally();
		} catch (Throwable t) {
			/*
			 * don't fail build if TAP parser barfs. only print out the
			 * exception to console.
			 */
			t.printStackTrace(logger);
		}

		build.getActions().add(new TapTestResultAction(build, testResult));
		
		if (testResult.getTestSets().size() > 0) {
			// create an individual report for all of the results and add it to
			// the build
			TapBuildAction action = new TapBuildAction(build, testResult);
			build.getActions().add(action);
			if (testResult.hasParseErrors()) {
				build.setResult(Result.UNSTABLE);
			}
			if (testResult.getFailed() > 0) {
				if(this.getFailedTestsMarkBuildAsFailure()) {
					build.setResult(Result.FAILURE);
				} else {
					build.setResult(Result.UNSTABLE);
				}
			}
		} else {
			logger.println("Found matching files but did not find any TAP results.");
			return Boolean.TRUE;
		}
		logger.println("TAP Reports Processing: FINISH");
		return Boolean.TRUE;
	}

	/**
	 * @param build
	 * @param logger
	 * @return
	 */
	private TapResult loadResults(AbstractBuild<?, ?> owner,
			PrintStream logger) {
		FilePath tapDir = getTapReportDirectory(owner);
		FilePath[] results = null;
		try {
			results = tapDir.list("**/*.*");
		} catch (Exception e) {
			e.printStackTrace(logger);
		}

		TapResult tr = null;
		if (results == null) {
			tr = new TapResult("", owner, Collections.EMPTY_LIST, this.todoIsFailure);
			tr.setOwner(owner);
			return tr;
		}

		TapParser parser = new TapParser(this.outputTapToConsole, this.enableSubtests, this.todoIsFailure, logger);
		TapResult result = parser.parse(results, owner);
		result.setOwner(owner);
		return result;
	}

	/**
	 * @param workspace 
	 * @param tapDir
	 * @param reports
	 * @param logger
	 * @return
	 */
	private boolean saveReports(FilePath workspace, FilePath tapDir, FilePath[] reports,
			PrintStream logger) {
		logger.println("Saving reports...");
		try {
			tapDir.mkdirs();
			for (FilePath report : reports) {
				//FilePath dst = tapDir.child(report.getName());
				FilePath dst = getDistDir(workspace, tapDir, report);
				report.copyTo(dst);
			}
		} catch (Exception e) {
			e.printStackTrace(logger);
			return false;
		}
		return true;
	}
	
	private FilePath getDistDir(FilePath workspace, FilePath tapDir, FilePath orig) {
		if(orig == null)
			return null;
		StringBuilder difference = new StringBuilder();
		FilePath parent = orig.getParent();
		do {
			if(parent.equals(workspace))
				break;
			difference.insert(0, parent.getName() + File.separatorChar);
		} while((parent = parent.getParent()) !=null);
		difference.append(orig.getName());
		return tapDir.child(difference.toString());
	}

	/**
	 * @param build
	 * @return
	 */
	private FilePath getTapReportDirectory(AbstractBuild<?, ?> build) {
		return new FilePath(new File(build.getRootDir(), "tap-master-files"));
	}

	/**
	 * @param build
	 * @param reports
	 * @param logger
	 * @return
	 */
	private FilePath[] checkReports(AbstractBuild<?, ?> build,
			FilePath[] reports, PrintStream logger) {
		List<FilePath> filePathList = new ArrayList<FilePath>(reports.length);

		for (FilePath report : reports) {
			/*
			 * Check that the file was created as part of this build and is not
			 * something left over from before.
			 * 
			 * Checks that the last modified time of file is greater than the
			 * start time of the build
			 */
			try {
				/*
				 * dividing by 1000 and comparing because we want to compare
				 * secs and not milliseconds
				 */
				if (build.getTimestamp().getTimeInMillis() / 1000 <= report
						.lastModified() / 1000) {
					filePathList.add(report);
				} else {
					logger.println(report.getName()
							+ " was last modified before "
							+ "this build started. Ignoring it.");
				}
			} catch (IOException e) {
				// just log the exception
				e.printStackTrace(logger);
			} catch (InterruptedException e) {
				// just log the exception
				e.printStackTrace(logger);
			}
		}
		return filePathList.toArray(new FilePath[] {});
	}

	/**
	 * @param workspace
	 * @param testResults
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private FilePath[] locateReports(FilePath workspace, String testResults)
			throws IOException, InterruptedException {
		return workspace.list(testResults);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}
	
	/* (non-Javadoc)
	 * @see hudson.matrix.MatrixAggregatable#createAggregator(hudson.matrix.MatrixBuild, hudson.Launcher, hudson.model.BuildListener)
	 */
	public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
		return new TestResultAggregator(build, launcher, listener);
	}

	@Extension(ordinal = 1000.0)
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public DescriptorImpl() {
			super(TapPublisher.class);
			load();
		}

		@Override
		public String getDisplayName() {
			return "Publish TAP Results";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@Override
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return Boolean.TRUE;
		}

	}

}
