package com.gitb.validation;


import com.gitb.core.Configuration;
import com.gitb.core.ValidationModule;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;

import java.util.List;
import java.util.Map;

/**
 * Created by tuncay on 9/2/14.
 */
public interface IValidationHandler {

	/**
	 * Returns the validation module definition
	 * @return module definition
	 */
	public ValidationModule getModuleDefinition();

    /**
     * Validates the content with given validator
     * @param inputs
     * @return
     */
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs);

}
