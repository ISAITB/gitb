package com.gitb.validation.common;

import com.gitb.core.ValidationModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.XMLUtils;
import com.gitb.validation.IValidationHandler;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * Created by senan on 24.10.2014.
 */
public abstract class AbstractValidator implements IValidationHandler {

    protected ValidationModule validatorDefinition;

    @Override
    public ValidationModule getModuleDefinition() {
        return this.validatorDefinition;
    }

    protected static ValidationModule readModuleDefinition(String fileName) {
        try {
            ValidationModule module = null;
            InputStream resource = AbstractValidator.class.getResourceAsStream(fileName);

            if(resource != null) {
                module = XMLUtils.unmarshal(ValidationModule.class, new StreamSource(resource));
            }
            return module;
        } catch (Exception e) {
            throw new GITBEngineInternalError(e);
        }
    }
}
