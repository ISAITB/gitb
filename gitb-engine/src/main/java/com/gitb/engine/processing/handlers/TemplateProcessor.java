/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.processing.handlers;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.SessionManager;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.engine.utils.TemplateUtils;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@ProcessingHandler(name="TemplateProcessor")
public class TemplateProcessor extends AbstractProcessingHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateProcessor.class);
    private static final String OPERATION_PROCESS = "process";
    private static final String INPUT_TEMPLATE = "template";
    private static final String INPUT_SYNTAX = "syntax";
    private static final String INPUT_SYNTAX_FREEMARKER = "freemarker";
    private static final String INPUT_SYNTAX_GITB = "gitb";
    private static final String INPUT_PARAMETERS = "parameters";
    private static final String OUTPUT_DATA = "data";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("TemplateProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_PROCESS,
                List.of(createParameter(INPUT_TEMPLATE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The actual template content to use."),
                        createParameter(INPUT_SYNTAX, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The syntax used in the provided template (can be the default '"+ INPUT_SYNTAX_GITB +"' or '"+ INPUT_SYNTAX_FREEMARKER +"')."),
                        createParameter(INPUT_PARAMETERS, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map of input parameters to replace placeholders.")),
                List.of(createParameter(OUTPUT_DATA, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The output value after processing the template."))
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        var templateContent = getInputForName(input, INPUT_TEMPLATE, StringType.class);
        if (templateContent == null) {
            throw new IllegalArgumentException("No template was provided. Ensure you pass an input named '"+ INPUT_TEMPLATE +"' to this processing handler.");
        }
        var templateSyntax = determineTemplateSyntax(input, session);
        var parameters = getInputForName(input, INPUT_PARAMETERS, MapType.class);
        DataType outputValue;
        if (templateSyntax == Syntax.FREEMARKER) {
            var configuration = new Configuration(Configuration.VERSION_2_3_31);
            configuration.setTemplateLoader(new InMemoryTemplateLoader(templateContent));
            Template template;
            try {
                template = configuration.getTemplate("template");
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialise the provided FreeMarker template", e);
            }
            var outWriter = new StringWriter();
            try {
                template.process(toDataModel(parameters), outWriter);
                outWriter.flush();
            } catch (IOException | TemplateException e) {
                throw new IllegalStateException("Failed to process the provided FreeMarker template", e);
            }
            outputValue = new StringType(outWriter.toString());
        } else {
            // GITB syntax.
            var testSessionContext = SessionManager.getInstance().getContext(session);
            var templateScope = testSessionContext.getScope().createChildScope();
            if (parameters != null) {
                for (var entry: parameters.getItems().entrySet()) {
                    templateScope.createVariable(entry.getKey()).setValue(entry.getValue());
                }
            }
            outputValue = TemplateUtils.generateDataTypeFromTemplate(templateScope, templateContent, null, false);
        }
        var data = new ProcessingData();
        data.getData().put(OUTPUT_DATA, outputValue);
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private Syntax determineTemplateSyntax(ProcessingData input, String sessionId) {
        var syntax = Syntax.GITB;
        var syntaxInput = getInputForName(input, INPUT_SYNTAX, StringType.class);
        if (syntaxInput != null) {
            var syntaxInputValue = syntaxInput.getValue().toLowerCase(Locale.ROOT);
            if (INPUT_SYNTAX_FREEMARKER.equals(syntaxInputValue)) {
                syntax = Syntax.FREEMARKER;
            } else if (!INPUT_SYNTAX_GITB.equals(syntaxInputValue)) {
                LOG.warn(MarkerFactory.getDetachedMarker(sessionId), "Unsupported template syntax type [{}]. Considering [" + INPUT_SYNTAX_GITB + "] instead.", syntaxInput.getValue());
            }
        }
        return syntax;
    }

    private Object dataTypeToObject(DataType type) {
        if (type instanceof ListType) {
            var list = new ArrayList<>();
            for (int i=0; i < ((ListType)type).getSize(); i++) {
                list.add(dataTypeToObject(((ListType)type).getItem(i)));
            }
            return list;
        } else if (type instanceof MapType) {
            var map = new HashMap<String, Object>();
            ((MapType)type).getItems().forEach((key, value) -> map.put(key, dataTypeToObject(value)));
            return map;
        } else {
            return type.getValue();
        }
    }

    private Object toDataModel(MapType parameters) {
        if (parameters == null) {
            return Collections.emptyMap();
        } else {
            return dataTypeToObject(parameters);
        }
    }

    enum Syntax {
        FREEMARKER, GITB
    }

    static class InMemoryTemplateLoader implements TemplateLoader {

        private final StringType templateData;
        private final long currentTime = System.currentTimeMillis();
        private Reader templateReader;

        InMemoryTemplateLoader(StringType templateData) {
            this.templateData = templateData;
        }

        @Override
        public Object findTemplateSource(String s) {
            return templateData;
        }

        @Override
        public long getLastModified(Object o) {
            return currentTime;
        }

        @Override
        public Reader getReader(Object o, String s) throws IOException {
            closeReader();
            templateReader = new StringReader(templateData.getValue());
            return templateReader;
        }

        @Override
        public void closeTemplateSource(Object o) throws IOException {
            closeReader();
        }

        private void closeReader() throws IOException {
            if (templateReader != null) {
                templateReader.close();
                templateReader = null;
            }
        }
    }
}
