package com.gitb.processing;

import com.gitb.core.Configuration;
import com.gitb.ps.ProcessingModule;

import java.util.List;

public interface IProcessingHandler {

    ProcessingModule getModuleDefinition();

    String beginTransaction(List<Configuration> config);

    ProcessingReport process(String session, String operation, ProcessingData input);

    void endTransaction(String session);

}
