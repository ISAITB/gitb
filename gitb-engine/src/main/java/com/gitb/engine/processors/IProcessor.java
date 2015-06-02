package com.gitb.engine.processors;

import com.gitb.tr.TestStepReportType;

/**
 * Created by tuncay on 9/2/14.
 */
public interface IProcessor {
	public TestStepReportType process(Object object) throws Exception;
}
