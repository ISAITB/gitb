package com.gitb.engine;

import com.gitb.tpl.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by serbay on 10/15/14.
 */
public class TDLToTPLConverterTest {
	@Test
	public void testConverter() {
		TestCase testCaseDescription = TestCaseManager.getTestCasePresentation("Sample/Actor1/testcases/TestCase1");

		assertNotNull(testCaseDescription);
	}
}
