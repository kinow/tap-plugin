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

import hudson.FilePath;
import hudson.model.ModelObject;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.tap4j.consumer.TapConsumer;
import org.tap4j.consumer.TapConsumerFactory;
import org.tap4j.model.BailOut;
import org.tap4j.model.Directive;
import org.tap4j.model.TestResult;
import org.tap4j.model.TestSet;
import org.tap4j.plugin.model.ParseErrorTestSetMap;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.DiagnosticUtil;
import org.tap4j.util.DirectiveValues;
import org.tap4j.util.StatusValues;

/**
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.0
 */
public class TapResult implements ModelObject, Serializable {

	private static final long serialVersionUID = 4343399327336076951L;

	private AbstractBuild<?, ?> build;
	private List<TestSetMap> testSets;
	private List<TestSetMap> parseErrorTestSets;
	private int failed = 0;
	private int passed = 0;
	private int skipped = 0;
	private int bailOuts = 0;
	private int total = 0;
	private String name;

	public TapResult(String name, AbstractBuild<?, ?> owner,
			List<TestSetMap> testSets) {
		this.name = name;
		this.build = owner;
		this.testSets = this.filterTestSet(testSets);
		this.parseErrorTestSets = this.filterParseErrorTestSets(testSets);
	}

	/**
	 * @param testSets
	 *            Untiltered test sets
	 * @return Test sets that failed to parse
	 */
	private List<TestSetMap> filterParseErrorTestSets(List<TestSetMap> testSets) {
		final List<TestSetMap> filtered = new ArrayList<TestSetMap>();
		for (TestSetMap testSet : testSets) {
			if (testSet instanceof ParseErrorTestSetMap) {
				filtered.add(testSet);
			}
		}
		return filtered;
	}

	/**
	 * @param testSets
	 *            Unfiltered test sets
	 * @return Test sets that didn't fail to parse
	 */
	private List<TestSetMap> filterTestSet(List<TestSetMap> testSets) {
		final List<TestSetMap> filtered = new ArrayList<TestSetMap>();
		for (TestSetMap testSet : testSets) {
			if (testSet instanceof ParseErrorTestSetMap == false) {
				filtered.add(testSet);
			}
		}
		return filtered;
	}

	public void tally() {

		failed = 0;
		passed = 0;
		skipped = 0;
		bailOuts = 0;
		total = 0;

		for (TestSetMap testSet : testSets) {
			TestSet realTestSet = testSet.getTestSet();
			List<TestResult> testResults = realTestSet.getTestResults();

			total += testResults.size();

			for (TestResult testResult : testResults) {
				if (isSkipped(testResult)) {
					skipped += 1;
				} else if (isFailure(testResult)) {
					failed += 1;
				} else {
					passed += 1;
				}
			}

			this.bailOuts += realTestSet.getNumberOfBailOuts();
		}
	}

	public AbstractBuild<?, ?> getOwner() {
		return this.build;
	}

	/**
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(AbstractBuild<?, ?> owner) {
		this.build = owner;
	}

	public List<TestSetMap> getTestSets() {
		return this.testSets;
	}

	public boolean isEmptyTestSet() {
		return this.testSets.size() <= 0;
	}

	/**
	 * @return the parseErrorTestSets
	 */
	public List<TestSetMap> getParseErrorTestSets() {
		return parseErrorTestSets;
	}

	public boolean hasParseErrors() {
		return this.parseErrorTestSets.size() > 0;
	}

	public int getFailed() {
		return this.failed;
	}

	public int getSkipped() {
		return this.skipped;
	}

	public int getPassed() {
		return this.passed;
	}

	public int getBailOuts() {
		return this.bailOuts;
	}

	public int getTotal() {
		return this.total;
	}

	private boolean isSkipped(TestResult testResult) {
		boolean r = false;
		Directive directive = testResult.getDirective();
		if (directive != null
				&& directive.getDirectiveValue() == DirectiveValues.SKIP) {
			r = true;
		}
		return r;
	}

	private boolean isFailure(TestResult testResult) {
		boolean r = false;
		Directive directive = testResult.getDirective();
		StatusValues status = testResult.getStatus();
		if (directive != null
				&& directive.getDirectiveValue() == DirectiveValues.TODO) {
			r = true;
		} else if (status != null && status == StatusValues.NOT_OK) {
			r = true;
		}
		return r;
	}

	/**
	 * Called from TapResult/index..jelly
	 */
	public String createDiagnosticTable(String tapFile, Map<String, Object> diagnostic) {
		return DiagnosticUtil.createDiagnosticTable(tapFile, diagnostic);
	}

	public boolean isTestResult(Object tapResult) {
		return (tapResult != null && tapResult instanceof TestResult);
	}

	public boolean isBailOut(Object tapResult) {
		return (tapResult != null && tapResult instanceof BailOut);
	}

	/**
	 * @return the name
	 */
	@Exported(visibility = 999)
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.ModelObject#getDisplayName()
	 */
	public String getDisplayName() {
		return getName();
	}
	
	public String getContents(String fileName) {
		String contents = "";
		if(fileName != null) {
			FilePath tapDir = new FilePath(new FilePath(new File(build.getRootDir(), "tap")), fileName);
			try {
				if(tapDir.exists()) {
					contents = org.apache.commons.lang.StringEscapeUtils.escapeHtml(tapDir.readToString());
				}
			} catch (IOException e) {
				contents = e.getMessage();
			} catch (InterruptedException e) {
				contents = e.getMessage();
			}
		}
		return contents;
	}
	
	public void doDownloadAttachment(StaplerRequest request, StaplerResponse response) {
		String f = request.getParameter("f");
		String key = request.getParameter("key");
		try {
			FilePath tapDir = new FilePath(new FilePath(new File(build.getRootDir(), "tap")), f);
			ServletOutputStream sos = response.getOutputStream();
			if(tapDir.exists()) {
				String tapStream = tapDir.readToString();
				TapConsumer consumer = TapConsumerFactory.makeTap13YamlConsumer();
				TestSet ts = consumer.load(tapStream);
				
				String content = getContent(ts, key);
				if(StringUtils.isNotBlank(content)) {
					response.setContentType("application/force-download");
					//response.setContentLength((int)tapDir.length());
			        response.setContentLength(-1);
					response.setHeader("Content-Transfer-Encoding", "binary");
					response.setHeader("Content-Disposition","attachment; filename=\"" + f + "\"");//fileName);
					
					sos.write(Base64.decodeBase64(content));
					sos.print('\n');
				} else {
					sos.println("Couldn't locate attachment in YAMLish: " + f);
				}
			} else {
				sos.println("Couldn't read FilePath.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
		}
	}

	/**
	 * @param ts
	 * @param key
	 * @return
	 */
	private String getContent(TestSet ts, String key) {
		for(TestResult tr : ts.getTestResults()){
			Map<String, Object> diagnostics = tr.getDiagnostic();
			String parentKey = null;
			if(diagnostics != null && diagnostics.size() > 0) {
				return recursivelySearch(diagnostics, parentKey, key);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private String recursivelySearch(Map<String, Object> diagnostics, String parentKey, String key) {
		for(String diagnosticKey : diagnostics.keySet()) {
			Object value = diagnostics.get(diagnosticKey);
			if(value != null) {
				if(value instanceof Map<?, ?>) {
					return recursivelySearch((Map<String, Object>)value, diagnosticKey, key);
				} else {
					if(parentKey != null && parentKey.equals(key)) {
						Object o = diagnostics.get("File-Content");
						if(o == null)
							o = diagnostics.get("File-content");
						if(o != null && o instanceof String)
							return (String)o;
					} else if(diagnosticKey.equalsIgnoreCase("file-name") && value.equals(key)) {
						Object o = diagnostics.get("File-Content");
						if(o == null)
							o = diagnostics.get("File-content");
						if(o != null && o instanceof String)
							return (String)o;
					}
				}
			}
		}
		return null;
	}

}
