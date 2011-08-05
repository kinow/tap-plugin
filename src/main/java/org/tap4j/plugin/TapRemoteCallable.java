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
import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.tap4j.model.TestSet;
import org.tap4j.parser.ParserException;
import org.tap4j.parser.Tap13YamlParser;
import org.tap4j.plugin.model.TestSetMap;

/**
 * Executes remote TAP Stream retrieval and execution.
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 1.1
 */
public class TapRemoteCallable 
implements FileCallable<List<TestSetMap>>
{

	private String testResults;
	private BuildListener listener;
	private boolean parserErrors;
	
	public TapRemoteCallable( String testResults, BuildListener listener )
	{
		this.testResults = testResults;
		this.listener = listener;
		this.parserErrors = false;
	}
	
	public boolean hasParserErrors()
	{
		return this.parserErrors;
	}
	
	private static final long serialVersionUID = 2177054820555042304L;

	/* (non-Javadoc)
	 * @see hudson.FilePath.FileCallable#invoke(java.io.File, hudson.remoting.VirtualChannel)
	 */
	public List<TestSetMap> invoke( File f, VirtualChannel channel ) 
	throws IOException, InterruptedException
	{
		List<TestSetMap> testSets = null;
		
		if ( StringUtils.isBlank( testResults ) )
		{
			listener.getLogger().println();
			listener.getLogger().println("Empty TAP test results.");
			listener.getLogger().println();
			
			testSets = Collections.emptyList();
		}
		else
		{
			testSets = new LinkedList<TestSetMap>();
			
			String[] fileNames = null;
			
			listener.getLogger().println();
			
			listener.getLogger().println("Looking for TAP test results that match the pattern [" + this.testResults + "].");
			
			listener.getLogger().println();
			
			try
			{
				final FileSet fs = Util.createFileSet( f , testResults );
				final DirectoryScanner ds = fs.getDirectoryScanner();
				
				fileNames = ds.getIncludedFiles();
				
				listener.getLogger().println("Found ["+fileNames.length+"] TAP test result(s).");
				
				listener.getLogger().println();
			}
			catch (Exception e) 
			{
				e.printStackTrace( listener.fatalError(e.getMessage()) );				
				throw new AbortException(e.getMessage());
			}
			
			for ( String fileName : fileNames )
			{
				try
				{
					final File tapFile = new File( f, fileName );
					
					listener.getLogger().println("Parsing TAP test result ["+tapFile+"].");
					
					listener.getLogger().println();
					
					final TestSet testSet = new Tap13YamlParser().parseFile( tapFile );
					
					final TestSetMap map = new TestSetMap( tapFile.getAbsolutePath(), testSet );
					testSets.add( map );
				}
				catch ( ParserException pe )
				{
					this.parserErrors = true;
					pe.printStackTrace( listener.getLogger() );
				}
			}
		}
		
		return testSets;
	}

	
	
}
