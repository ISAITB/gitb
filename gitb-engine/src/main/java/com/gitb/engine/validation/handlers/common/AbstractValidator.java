package com.gitb.engine.validation.handlers.common;

import com.gitb.core.Configuration;
import com.gitb.core.ValidationModule;
import com.gitb.engine.AbstractHandler;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.XMLUtils;
import com.gitb.validation.IValidationHandler;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 24.10.2014.
 */
public abstract class AbstractValidator extends AbstractHandler implements IValidationHandler {

    public static final String TEST_CASE_ID_INPUT = "com.gitb.TestCaseID";

    protected ValidationModule validatorDefinition;

    @Override
    public ValidationModule getModuleDefinition() {
        return this.validatorDefinition;
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs, String stepId) {
        // Ignore the step ID for embedded validators.
        return validate(configurations, inputs);
    }

    protected static ValidationModule readModuleDefinition(String fileName) {
        try {
            ValidationModule module = null;
            InputStream resource = AbstractValidator.class.getResourceAsStream(fileName);
            if (resource != null) {
                module = XMLUtils.unmarshal(ValidationModule.class, new StreamSource(resource));
            }
            return module;
        } catch (Exception e) {
            throw new GITBEngineInternalError(e);
        }
    }

    protected String getTestCaseId(Map<String, DataType> inputs) {
        return (String) inputs.get(TEST_CASE_ID_INPUT).getValue();
    }

    public abstract TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs);

}
