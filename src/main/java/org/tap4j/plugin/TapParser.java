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

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.tap4j.model.TestSet;
import org.tap4j.parser.ParserException;
import org.tap4j.parser.Tap13YamlParser;
import org.tap4j.plugin.model.ParseErrorTestSetMap;
import org.tap4j.plugin.model.TestSetMap;

/**
 * Executes remote TAP Stream retrieval and execution.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.1
 */
public class TapParser {

	/** Prints the logs to the web server's console / log files */
	private static final Logger log = Logger.getLogger(TapParser.class
			.getName());
	private Boolean outputTapToConsole;
	private Boolean enableSubtests;
	private Boolean todoIsFailure;
	
	/** Build's logger to print logs as part of build's console output */
	private PrintStream logger;
	private boolean parserErrors;
	private boolean hasFailedTests;

	public TapParser(Boolean outputTapToConsole, Boolean enableSubtests, Boolean todoIsFailure, PrintStream logger) {
		this.outputTapToConsole = outputTapToConsole;
		this.enableSubtests = enableSubtests;
		this.todoIsFailure = todoIsFailure;
		this.logger = logger;
		this.parserErrors = false;
		this.hasFailedTests = false;
	}

	public boolean hasParserErrors() {
		return this.parserErrors;
	}

	public boolean hasFailedTests() {
		return this.hasFailedTests;
	}

	public TapResult parse(FilePath[] results, AbstractBuild<?, ?> build) {
		this.parserErrors = Boolean.FALSE;
		this.hasFailedTests = Boolean.FALSE;
		final List<TestSetMap> testSets = new LinkedList<TestSetMap>();
		if (null == results) {
			log("File paths not specified. paths var is null. Returning empty test results.");
		} else {
			for (FilePath path : results) {
				File tapFile = new File(path.getRemote());
				if (!tapFile.isFile()) {
					log("'" + tapFile.getAbsolutePath()
							+ "' points to an invalid test report");
					continue; // move to next file
				} else {
					log("Processing '" + tapFile.getAbsolutePath() + "'");
				}
				try {
					log("Parsing TAP test result [" + tapFile + "].");
	
					final Tap13YamlParser parser;
					if(enableSubtests != null) {
						parser = new Tap13YamlParser(enableSubtests);
					} else {
						parser = new Tap13YamlParser();
					}
					final TestSet testSet = parser.parseFile(tapFile);
	
					if (testSet.containsNotOk() || testSet.containsBailOut()) {
						this.hasFailedTests = Boolean.TRUE;
					}
	
					final TestSetMap map = new TestSetMap(
							tapFile.getName(), testSet);
					testSets.add(map);
	
					if (this.outputTapToConsole) {
						try {
							log(FileUtils.readFileToString(tapFile));
						} catch (RuntimeException re) {
							log(re);
						} catch (IOException e) {
							log(e);
						}
					}
				} catch (ParserException pe) {
					testSets.add(new ParseErrorTestSetMap(tapFile.getAbsolutePath(), pe));
					this.parserErrors = Boolean.TRUE;
					log(pe);
				}
			}
		}
		//final TapResult testResult = new TapResult(UUID.randomUUID().toString(), build, testSets);
		final TapResult testResult = new TapResult("TAP Test Results", build, testSets, this.todoIsFailure);
		return testResult;
	}

	private void log(String str) {
		if (logger != null) {
			logger.println(str);
		} else {
			log.fine(str);
		}
	}

	private void log(Exception ex) {
		if (logger != null) {
			ex.printStackTrace(logger);
		} else {
			log.severe(ex.toString());
		}
	}

}
