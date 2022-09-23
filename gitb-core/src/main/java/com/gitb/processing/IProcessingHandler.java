package com.gitb.processing;

import com.gitb.StepHandler;
import com.gitb.core.Configuration;
import com.gitb.ps.ProcessingModule;

import java.util.List;

public interface IProcessingHandler extends StepHandler {

    ProcessingModule getModuleDefinition();

    String beginTransaction(String stepId, List<Configuration> config);

    ProcessingReport process(String session, String stepId, String operation, ProcessingData input);

    void endTransaction(String session, String stepId);

}
