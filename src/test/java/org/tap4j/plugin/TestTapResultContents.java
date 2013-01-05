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
package org.tap4j.plugin;

import hudson.model.FreeStyleBuild;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.tap4j.model.TestSet;
import org.tap4j.parser.Tap13Parser;
import org.tap4j.plugin.model.TestSetMap;
import org.tap4j.plugin.util.Constants;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
@Bug(14859)
public class TestTapResultContents extends HudsonTestCase {

	@Test
	public void testContents() throws IOException, InterruptedException {
		Tap13Parser parser = new Tap13Parser();
		TestSet ts = parser.parseFile(new File(TestTapResultContents.class.getResource("/org/tap4j/plugin/"+Constants.TAP_DIR_NAME+"/sample.tap").getFile()));
		TestSetMap tsm = new TestSetMap("sample.tap", ts);
		List<TestSetMap> tss = new ArrayList<TestSetMap>();
		tss.add(tsm);
		FreeStyleBuild a = new FreeStyleBuild(createFreeStyleProject()) {
			@Override
			public File getRootDir() {
				return new File(TestTapResultContents.class.getResource("/org/tap4j/plugin/").getFile());
			}
		};
		TapResult result = new TapResult("name", a, tss, true);
		String contents = result.getContents("sample.tap");
		String expected = "1..3\nok 1\nnot ok 2\n  # some IO error\n  # and more text here\nok 3 # SKIP error in test 2";
		assertEquals(expected, contents);
	}
	
}
