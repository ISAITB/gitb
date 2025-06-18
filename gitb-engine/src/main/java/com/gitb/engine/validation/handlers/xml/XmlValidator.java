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

package com.gitb.engine.validation.handlers.xml;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.engine.utils.ReportItemComparator;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.engine.validation.handlers.common.AbstractValidator;
import com.gitb.engine.validation.handlers.schematron.SchematronReportHandler;
import com.gitb.engine.validation.handlers.schematron.SchematronValidator;
import com.gitb.engine.validation.handlers.xsd.XsdReportHandler;
import com.gitb.engine.validation.handlers.xsd.XsdValidator;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.*;

import java.util.*;

@ValidationHandler(name="XmlValidator")
public class XmlValidator extends AbstractValidator {

    private final static String XML_ARGUMENT_NAME = "xml";
    private final static String XSD_ARGUMENT_NAME = "xsd";
    private final static String SCHEMATRON_ARGUMENT_NAME = "schematron";
    private final static String SCHEMATRON_TYPE_ARGUMENT_NAME = "schematronType";
    private final static String STOP_ON_XSD_ERRORS_ARGUMENT_NAME = "stopOnXsdErrors";
    private final static String SHOW_ARTEFACTS_ARGUMENT_NAME = "showValidationArtefacts";
    private final static String SHOW_SCHEMATRON_TESTS_ARGUMENT_NAME = "showSchematronTests";
    private final static String SORT_BY_SEVERITY_ARGUMENT_NAME  = "sortBySeverity";
    private final static String MODULE_DEFINITION_XML = "/validation/xml-validator-definition.xml";

    public XmlValidator() {
        this.validatorDefinition = readModuleDefinition(MODULE_DEFINITION_XML);
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs) {
        var xml = Objects.requireNonNull(getAndConvert(inputs, XML_ARGUMENT_NAME, DataType.OBJECT_DATA_TYPE, ObjectType.class), "Input ["+XML_ARGUMENT_NAME+"] must be provided");
        var xsd = getAndConvert(inputs, XSD_ARGUMENT_NAME, DataType.SCHEMA_DATA_TYPE, SchemaType.class);
        var schematrons = getAndConvert(inputs, SCHEMATRON_ARGUMENT_NAME, DataType.LIST_DATA_TYPE, ListType.class);
        var schematronType = getAndConvert(inputs, SCHEMATRON_TYPE_ARGUMENT_NAME, DataType.STRING_DATA_TYPE, StringType.class);
        var stopOnXsdErrors = getAndConvert(inputs, STOP_ON_XSD_ERRORS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var showArtefacts = getAndConvert(inputs, SHOW_ARTEFACTS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var sortBySeverity = getAndConvert(inputs, SORT_BY_SEVERITY_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var showTests = getAndConvert(inputs, SHOW_SCHEMATRON_TESTS_ARGUMENT_NAME, DataType.BOOLEAN_DATA_TYPE, BooleanType.class);
        var testCaseId = new StringType(getTestCaseId(inputs));
        TAR xsdReport = null;
        List<TAR> schematronReports = new ArrayList<>();
        List<TAR> allReports = new ArrayList<>();
        if (xsd != null) {
            var map = new HashMap<>(Map.of(
                    XsdValidator.XML_ARGUMENT_NAME, xml,
                    XsdValidator.XSD_ARGUMENT_NAME, xsd,
                    TEST_CASE_ID_INPUT, testCaseId
            ));
            putIfNotNull(map, XsdValidator.SHOW_SCHEMA_ARGUMENT_NAME, showArtefacts);
            xsdReport = (TAR)new XsdValidator().validate(configurations, map);
            allReports.add(xsdReport);
        }
        /*
        We proceed with Schematron validation if:
        - There are configured schematrons.
        - There are no XSD errors or XSD errors are set to not stop the validation.
         */
        if (schematrons != null && !schematrons.isEmpty() && (xsdReport == null || xsdReport.getResult() != TestResultType.FAILURE || (stopOnXsdErrors != null && !((Boolean) stopOnXsdErrors.getValue())))) {
            var schematronValidator = new SchematronValidator();
            for (var schematron: schematrons) {
                var map = new HashMap<>(Map.of(
                        SchematronValidator.CONTENT_ARGUMENT_NAME, xml,
                        SchematronValidator.SCHEMATRON_ARGUMENT_NAME, schematron,
                        TEST_CASE_ID_INPUT, testCaseId
                ));
                putIfNotNull(map, SchematronValidator.SCHEMATRON_TYPE_ARGUMENT_NAME, schematronType);
                putIfNotNull(map, SchematronValidator.SHOW_SCHEMATRON_ARGUMENT_NAME, showArtefacts);
                putIfNotNull(map, SchematronValidator.SHOW_TESTS_ARGUMENT_NAME, showTests);
                schematronReports.add((TAR)schematronValidator.validate(configurations, map));
            }
            allReports.addAll(schematronReports);
        }
        var report = TestCaseUtils.createEmptyReport();
        if (!allReports.isEmpty()) {
            report = TestCaseUtils.mergeReports(allReports);
            var context = new AnyContent();
            context.getItem().add(TestCaseUtils.getInputFor(report.getContext().getItem(), XsdReportHandler.XML_ITEM_NAME).get(0));
            // Add validation artefacts.
            if (showArtefacts == null || ((Boolean) showArtefacts.getValue())) {
                // Add XSD.
                if (xsdReport != null) {
                    context.getItem().add(TestCaseUtils.getInputFor(xsdReport.getContext().getItem(), XsdReportHandler.XSD_ITEM_NAME).get(0));
                }
                // Add schematrons.
                if (!schematronReports.isEmpty()) {
                    var schematronsToShow = new AnyContent();
                    if (schematronReports.size() == 1) {
                        // Show as single Schematron file.
                        schematronsToShow = TestCaseUtils.getInputFor(schematronReports.get(0).getContext().getItem(), SchematronReportHandler.SCH_ITEM_NAME).get(0);
                    } else {
                        // Show as list of Schematron files.
                        schematronsToShow.setName(SchematronReportHandler.SCH_ITEM_NAME);
                        for (var schematronReport: schematronReports) {
                            var item = TestCaseUtils.getInputFor(schematronReport.getContext().getItem(), SchematronReportHandler.SCH_ITEM_NAME).get(0);
                            item.setName("");
                            schematronsToShow.getItem().add(item);
                        }
                    }
                    context.getItem().add(schematronsToShow);
                }
            }
            report.setName("XML validation");
            report.setContext(context);
            if (report.getReports() != null) {
                var sortType = ReportItemComparator.SortType.LOCATION_THEN_SEVERITY;
                if (sortBySeverity != null && ((Boolean) sortBySeverity.getValue())) {
                    sortType = ReportItemComparator.SortType.SEVERITY_THEN_LOCATION;
                }
                report.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator(sortType));
            }
        }
        return report;
    }

    private <T extends DataType> void putIfNotNull(Map<String, T> map, String key, T value) {
        if (value != null) {
            map.put(key, value);
        }
    }

}
