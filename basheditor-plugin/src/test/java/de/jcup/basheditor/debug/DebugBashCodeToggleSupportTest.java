/*
 * Copyright 2019 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.basheditor.debug;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import de.jcup.basheditor.CallTestContext;
import de.jcup.basheditor.TestScriptLoader;
import de.jcup.basheditor.debug.launch.OSUtil;

public class DebugBashCodeToggleSupportTest {

	private static final String BASE_EXPECTED_DEBUG_ENABLED_CODE = "source "+OSUtil.toUnixPath(System.getProperty("user.home")+"/.basheditor/remote-debugging-v1.sh");
	private static final String EXPECTED_DEBUG_ENABLED_CODE = BASE_EXPECTED_DEBUG_ENABLED_CODE+" localhost "+BashDebugConstants.DEFAULT_DEBUG_PORT+" #BASHEDITOR-TMP-REMOTE-DEBUGGING-END";
	private DebugBashCodeToggleSupport supportToTest;

	@Before
	public void before() {
		supportToTest = new DebugBashCodeToggleSupport();
	}
	
	@Test
    public void bugfix_139_convert_unixpathes_in_windows() throws Exception {
	    
	    /* windows pathes are adopted to unix pathes in min-gw style*/
	    assertEquals("/C/Users/albert/.basheditor/remote-debugging-v1.sh", supportToTest.convertToUnixStylePath("C:\\Users\\albert\\.basheditor\\remote-debugging-v1.sh"));
	    assertEquals("/D/some/Other/.path/xYz.sh", supportToTest.convertToUnixStylePath("D:\\some\\Other\\.path\\xYz.sh"));
	    assertEquals("/X", supportToTest.convertToUnixStylePath("X:"));
	    assertEquals("/Y/file1.txt", supportToTest.convertToUnixStylePath("Y:\\file1.txt"));
	    
	    /* unix pathes keep as is */
	    assertEquals("/C/Users/albert/.basheditor/remote-debugging-v1.sh", supportToTest.convertToUnixStylePath("/C/Users/albert/.basheditor/remote-debugging-v1.sh"));
	    assertEquals("/D/some/Other/.path/xYz.sh", supportToTest.convertToUnixStylePath("/D/some/Other/.path/xYz.sh"));
	    assertEquals("/X", supportToTest.convertToUnixStylePath("/X"));
	    assertEquals("/Y/file1.txt", supportToTest.convertToUnixStylePath("/Y/file1.txt"));
	    assertEquals("/file1.txt", supportToTest.convertToUnixStylePath("/file1.txt"));
	    
	}
	
	@Test
    public void bugfix_139_check_convertToUnixStylePath_really_called() throws Exception {
        CallTestContext supportToTestWas = new CallTestContext();
        /* prepare */
        supportToTest = new DebugBashCodeToggleSupport() {
            @Override
            String convertToUnixStylePath(String path) {
                supportToTestWas.called=true;
                return super.convertToUnixStylePath(path);
            }
        };
        
        /* execute */
        supportToTest.enableDebugging("", null,-1);
        
        /* check converter method replaces this as expected */
        assertTrue(supportToTestWas.called);
        
    }
	
	@Test
	public void enable_debugging_empty_code_results_in_firstline_including_temp_debugger_file() throws Exception {
		/* execute */
		String newCode = supportToTest.enableDebugging("","localhost", BashDebugConstants.DEFAULT_DEBUG_PORT);

		/* test */
		String[] asArray = newCode.split("\n");
		assertEquals(1, asArray.length);
		assertTrue(asArray[0].startsWith(EXPECTED_DEBUG_ENABLED_CODE));

	}

	@Test
	public void enable_debugging_starting_with_comment_results_in_firstline_including_temp_debugger_file_and_with_comment_before() throws Exception {
		/* execute */
		String newCode = supportToTest.enableDebugging("#! /bin/mybash","localhost", BashDebugConstants.DEFAULT_DEBUG_PORT);

		/* test */
		String[] asArray = newCode.split("\n");
		assertEquals(2, asArray.length);
		assertEquals(EXPECTED_DEBUG_ENABLED_CODE, asArray[0]);
		assertEquals("#! /bin/mybash", asArray[1]);

	}
	
	@Test
	public void enable_debugging_starting_with_not_comment_but_code_results_in_firstline_including_temp_debugger_file_and_new_line_with_command_before() throws Exception {
		/* execute */
		String newCode = supportToTest.enableDebugging("echo alpha","localhost", BashDebugConstants.DEFAULT_DEBUG_PORT);

		/* test */
		String[] asArray = newCode.split("\n");
		assertEquals(2, asArray.length);
		assertEquals(EXPECTED_DEBUG_ENABLED_CODE, asArray[0]);
		assertEquals("echo alpha", asArray[1]);

	}

	@Test
	public void enable_debugging_will_automatically_create_debug_bash_code_file_which_contains_data_of_code_builder() throws Exception {
		/* prepare */
		File file = new File(System.getProperty("user.home"),"/.basheditor/remote-debugging-v1.sh");
		if (file.exists()) {
			file.delete();
		}
		assertFalse(file.exists());

		/* execute */
		supportToTest.enableDebugging("","localhost",BashDebugConstants.DEFAULT_DEBUG_PORT);

		/* test */
		assertTrue(file.exists()); // file must be recreated
		// check content is as expected:

		DebugBashCodeBuilder codeBuilder = new DebugBashCodeBuilder();
		String expected = codeBuilder.buildDebugBashCodeSnippet();

		String contentOfFile = TestScriptLoader.loadScript(file);
		assertEquals(expected, contentOfFile);

	}

	@Test
	public void disable_debugging_empty_code_results_in_empty_code() throws Exception {
		assertEquals("", supportToTest.disableDebugging(""));
	}

	@Test
	public void disable_debugging_first_line_has_include_but_nothing_else_results_in_empty_code() throws Exception {
		assertEquals("", supportToTest.disableDebugging(EXPECTED_DEBUG_ENABLED_CODE+"\n"));
	}
	@Test
	public void disable_debugging_first_line_has_include_and_one_empty_line_nothing_else_results_in_one_empty_line() throws Exception {
		assertEquals("\n", supportToTest.disableDebugging(EXPECTED_DEBUG_ENABLED_CODE+"\n\n"));
	}

	@Test
	public void disable_debugging_first_line_has_include_and_comment_after_include_only_comment_remains() throws Exception {
		assertEquals("#! /bin/mybash", supportToTest.disableDebugging(EXPECTED_DEBUG_ENABLED_CODE + "\n#! /bin/mybash"));
	}

	@Test
	public void disable_debugging_first_line_has_include_and_comment_secondline_has_alpha_after_include_only_comment_remains_in_first_line_second_has_alpha() throws Exception {
		assertEquals("#! /bin/mybash\nalpha", supportToTest.disableDebugging(EXPECTED_DEBUG_ENABLED_CODE + "\n#! /bin/mybash\nalpha"));
	}
	
	@Test
	public void disable_debugging_first_line_has_include_and_second_an_echo_alpha_result_first_line_will_be_echo_alpha() throws Exception {
		assertEquals("echo alpha", supportToTest.disableDebugging(EXPECTED_DEBUG_ENABLED_CODE + "\necho alpha"));
	}

}
