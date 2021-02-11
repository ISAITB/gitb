package com.gitb.vs.tdl;

import java.util.Arrays;

import static com.gitb.vs.tdl.ErrorLevel.*;

public enum ErrorCode {

    INVALID_ZIP_ARCHIVE(                                "TDL-001", "The test suite archive is not a valid ZIP archive.", ERROR),
    NO_TEST_SUITE_FOUND(                                "TDL-002", "The provided archive does not contain a test suite file.", ERROR),
    MULTIPLE_TEST_SUITES_FOUND(                         "TDL-003", "Only a single test suite file should be included. The provided archive contains %s test suites.", ERROR),
    DUPLICATE_TEST_CASE_ID(                             "TDL-004", "Multiple test cases found with ID [%s].", ERROR),
    // Deprecated, given that only one test suite can be present.
//    DUPLICATE_TEST_SUITE_ID(                            "TDL-005", "Multiple test suites found with ID [%s].", ERROR),
    INVALID_TEST_SUITE_SYNTAX(                          "TDL-006", "%s", ERROR),
    INVALID_TEST_CASE_SYNTAX(                           "TDL-007", "%s", ERROR),
    NO_TEST_CASE_FOUND(                                 "TDL-008", "The provided archive must contain at least one test case.", ERROR),
    INVALID_TEST_CASE_REFERENCE(                        "TDL-009", "The test suite references a test case using an undefined ID [%s].", ERROR),
    INVALID_TEST_CASE_PREREQUISITE(                     "TDL-010", "The test suite specifies a prerequisite [%s] for test case [%s] that is not defined.", ERROR),
    INVALID_ACTOR_ID_REFERENCED_IN_TEST_CASE(           "TDL-011", "%s [%s] references an actor by ID [%s] that is not defined in the test suite.", ERROR, true),
    NO_SUT_DEFINED_IN_TEST_CASE(                        "TDL-012", "%s [%s] does not define an actor with role 'SUT'.", ERROR, true),
    TEST_CASE_REFERENCES_ACTOR_MULTIPLE_TIMES(          "TDL-013", "%s [%s] defines multiple references to the same actor [%s].", ERROR, true),
    TEST_SUITE_DEFINES_ACTOR_MULTIPLE_TIMES(            "TDL-014", "The test suite defines actor [%s] multiple times.", ERROR),
    ACTOR_NOT_REFERENCED_IN_TEST_CASES(                 "TDL-015", "Actor [%s] is not referenced in any test cases.", WARNING),
    INVALID_EXTERNAL_ACTOR_REFERENCE(                   "TDL-016", "Test suite references an external actor [%s] that is not defined.", ERROR),
    INVALID_EXTERNAL_PARAMETER_REFERENCE(               "TDL-017", "%s [%s] references a domain parameter [%s] in step [%s] that is not defined.", WARNING, true),
    TEST_CASE_NOT_REFERENCED(                           "TDL-018", "Test case [%s] is not referenced in the test suite.", WARNING),
    DEFAULT_ACTOR_NOT_REFERENCED_IN_TEST_CASES_AS_SUT(  "TDL-019", "The default actor for the specification [%s] is not referenced as a SUT in any test cases.", WARNING),
    INVALID_TEST_CASE_IMPORT(                           "TDL-020", "%s [%s] defines an import [%s] that doesn't exist within the test suite.", ERROR, true),
    // Deprecated since the possibility to import external resources and the use of variable expressions.
//    UNUSED_RESOURCE(                                    "TDL-021", "Resource [%s] is included in the test suite archive but is never used.", WARNING),
    INVALID_EMBEDDED_MESSAGING_HANDLER_REFERENCE(       "TDL-022", "%s [%s] references a messaging handler [%s] that does not exist.", ERROR, true),
    INVALID_EMBEDDED_PROCESSING_HANDLER_REFERENCE(      "TDL-023", "%s [%s] references a processing handler [%s] that does not exist.", ERROR, true),
    INVALID_EMBEDDED_VALIDATION_HANDLER_REFERENCE(      "TDL-024", "%s [%s] references a validation handler [%s] that does not exist.", ERROR, true),
    INVALID_TX_REFERENCE_FOR_MESSAGING_END(             "TDL-025", "%s [%s] specifies a messaging transaction end (etxn) for non-existent transaction ID [%s].", ERROR, true),
    INVALID_TX_REFERENCE_FOR_PROCESSING_END(            "TDL-026", "%s [%s] specifies a processing transaction end (eptxn) for non-existent transaction ID [%s].", ERROR, true),
    INVALID_TX_REFERENCE_FOR_MESSAGING_STEP(            "TDL-027", "%s [%s] specifies a messaging step (%s) that refers to non-existent transaction ID [%s].", ERROR, true),
    INVALID_TX_REFERENCE_FOR_PROCESSING_STEP(           "TDL-028", "%s [%s] specifies a process step that refers to non-existent transaction ID [%s].", ERROR, true),
    MESSAGING_STEP_OUTSIDE_TX(                          "TDL-029", "%s [%s] specifies a messaging step (%s) that is not contained within a transaction.", ERROR, true),
    PROCESSING_STEP_OUTSIDE_TX(                         "TDL-030", "%s [%s] specifies a process step that is not contained within a transaction nor defines its own handler.", ERROR, true),
    MESSAGING_TX_END_WITHOUT_START(                     "TDL-031", "%s [%s] specifies a messaging transaction end (etxn) without a corresponding begin (btxn).", ERROR, true),
    PROCESSING_TX_END_WITHOUT_START(                    "TDL-032", "%s [%s] specifies a processing transaction end (eptxn) without a corresponding begin (bptxn).", ERROR, true),
    INVALID_ACTOR_REFERENCE_IN_STEP(                    "TDL-033", "%s [%s] defines a %s step with an invalid actor reference [%s].", ERROR, true),
    REFERENCED_ACTOR_IN_STEP_HAS_UNEXPECTED_ROLE(       "TDL-034", "%s [%s] defines a %s step referencing actor [%s] with invalid role [%s]. The referenced actor must have role [%s].", ERROR, true),
    INVALID_ENCODING(                                   "TDL-035", "%s [%s] defines an invalid encoding value [%s] in a %s element.", ERROR, true),
    INVALID_DATA_TYPE_REFERENCE(                        "TDL-036", "%s [%s] defines an invalid data type [%s].", ERROR, true),
    INVALID_SCRIPTLET_REFERENCE(                        "TDL-037", "%s [%s] references a scriptlet [%s] that could not be found in the test suite.", ERROR, true),
    UNUSED_SCRIPTLET(                                   "TDL-038", "Test case [%s] defines an internal scriptlet [%s] but never calls it.", WARNING),

    INVALID_VARIABLE_REFERENCE(                         "TDL-039", "%s [%s] in step %s defines an invalid variable reference [%s].", ERROR, true),
    VARIABLE_NOT_IN_SCOPE(                              "TDL-040", "%s [%s] in step %s refers to an undefined variable [%s].", ERROR, true),
    SIMPLE_VARIABLE_REFERENCED_AS_CONTAINER(            "TDL-041", "%s [%s] in step %s refers to a simple variable [%s] as if it was a container type.", ERROR, true),
    INVALID_EXPRESSION(                                 "TDL-042", "%s [%s] in step %s defines an invalid expression [%s].", ERROR, true),
    UNEXPECTED_HANDLER_INPUT(                           "TDL-043", "%s [%s] in step %s defines an unexpected input [%s].", WARNING, true),
    MISSING_HANDLER_INPUT(                              "TDL-044", "%s [%s] in step %s does not provide a required input [%s].", ERROR, true),
    UNEXPECTED_HANDLER_CONFIG(                          "TDL-045", "%s [%s] in step %s defines an unexpected configuration parameter [%s].", WARNING, true),
    MISSING_HANDLER_CONFIG(                             "TDL-046", "%s [%s] in step %s does not provide a required configuration parameter [%s].", ERROR, true),
    INVALID_PROCESSING_HANDLER_OPERATION(               "TDL-047", "%s [%s] in step %s refers to invalid processing operation [%s].", ERROR, true),

    DUPLICATE_SCRIPTLET_ID(                             "TDL-048", "Test case [%s] defines multiple scriptlets for ID [%s].", ERROR),

    MISSING_LIST_CONTAINED_TYPE(                        "TDL-049", "%s [%s] defines a 'list' variable with no contained type. A default of 'string' is considered but it is best to define it explicitly (e.g. 'list[string]').", WARNING, true),
    MISSING_INTERACTION_OPTIONS(                        "TDL-050", "%s [%s] defines a user interaction request with a [%s] attribute but no [options] attribute. The [%s] attribute will be ignored.", WARNING, true),
    INTERACTION_OPTIONS_FOR_NON_STRING_INPUT(           "TDL-051", "%s [%s] defines a user interaction request of non-string [contentType] (%s) with a [options] attribute. The [options] attribute will be ignored.", WARNING, true),
    INTERACTION_OPTIONS_AND_LABELS_MISMATCH(            "TDL-052", "%s [%s] defines a user interaction request with an [options] attribute. The number of options [%s] must match the number of labels [%s].", ERROR, true),
    INTERACTION_OPTIONS_SINGLE_OPTION(                  "TDL-053", "%s [%s] defines a user interaction request with an [options] attribute but only a single option is defined.", WARNING, true),
    MISSING_TX_AND_HANDLER_FOR_PROCESSING_STEP(         "TDL-054", "%s [%s] defines a process step with no transaction ID reference and no handler definition.", ERROR, true),
    MESSAGING_TX_NOT_CLOSED(                            "TDL-055", "%s [%s] defines a messaging transaction [%s] that is not closed.", WARNING, true),
    PROCESSING_TX_NOT_CLOSED(                           "TDL-056", "%s [%s] defines a processing transaction [%s] that is not closed.", WARNING, true),
    MESSAGING_TX_NOT_USED(                              "TDL-057", "%s [%s] defines a messaging transaction [%s] that is never used.", WARNING, true),
    PROCESSING_TX_NOT_USED(                             "TDL-058", "%s [%s] defines a processing transaction [%s] that is never used.", WARNING, true),
    DOUBLE_PROCESSING_HANDLER(                          "TDL-059", "%s [%s] defines a process step that defined both a transaction reference [%s] and a handler [%s].", ERROR, true),
    VALUE_OF_MAP_VARIABLE_WITHOUT_NAME_OR_TYPE(         "TDL-060", "%s [%s] defines for map variable [%s] a value with no name or type.", ERROR, true),
    VALUE_OF_NON_MAP_VARIABLE_WITH_NAME_OR_TYPE(        "TDL-061", "%s [%s] defines for variable [%s] a value with name and type information that will be ignored.", WARNING, true),
    MULTIPLE_VALUES_FOR_PRIMITIVE_VARIABLE(             "TDL-062", "%s [%s] defines a simple variable [%s] with multiple values.", ERROR, true),

    POTENTIALLY_INVALID_ORGANISATION_VARIABLE(          "TDL-063", "References are made to custom organisation properties %s. Ensure these will be defined at runtime.", INFO),
    POTENTIALLY_INVALID_SYSTEM_VARIABLE(                "TDL-064", "References are made to custom system properties %s. Ensure these will be defined at runtime.", INFO),
    TEST_SUITE_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT(  "TDL-065", "The test suite defines for its documentation both a value and a resource import.", WARNING),
    TEST_SUITE_DOCUMENTATION_REFERENCE_INVALID(         "TDL-066", "The test suite defines its documentation via import [%s] that cannot be resolved.", ERROR),
    TEST_CASE_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT(   "TDL-067", "%s [%s] defines for its documentation both a value and a resource import.", WARNING, true),
    TEST_CASE_DOCUMENTATION_REFERENCE_INVALID(          "TDL-068", "%s [%s] defines its documentation via import [%s] that cannot be resolved.", ERROR, true),
    TEST_STEP_DOCUMENTATION_BOTH_AS_VALUE_AND_IMPORT(   "TDL-069", "%s [%s] defines a step [%s] with documentation both as a value and a resource import.", WARNING, true),
    TEST_STEP_DOCUMENTATION_REFERENCE_INVALID(          "TDL-070", "%s [%s] defines a step [%s] with a documentation import [%s] that cannot be resolved.", ERROR, true),

    DUPLICATE_ENDPOINT_NAME(                            "TDL-071", "The test suite defines for actor [%s] multiple endpoints with name [%s].", ERROR),
    DUPLICATE_PARAMETER_NAME(                           "TDL-072", "The test suite defines in actor [%s] an endpoint [%s] with multiple parameters named [%s].", ERROR),
    PARAMETER_PREREQUISITE_VALUE_NOT_ALLOWED(           "TDL-073", "The test suite defines parameter [%s] that depends on parameter [%s] but its expected value [%s] is not allowed.", ERROR),
    INVALID_PARAMETER_PREREQUISITE(                     "TDL-074", "The test suite defines parameter [%s] that depends on non-existent parameter [%s].", ERROR),
    INVALID_PARAMETER_PREREQUISITE_SELF(                "TDL-075", "The test suite defines parameter [%s] that depends on itself.", ERROR),
    PARAMETER_PREREQUISITE_WITHOUT_EXPECTED_VALUE(      "TDL-076", "The test suite defines parameter [%s] that depends on parameter [%s] without defining the expected value.", ERROR),
    PARAMETER_PREREQUISITE_VALUE_WITHOUT_PREREQUISITE(  "TDL-077", "The test suite defines parameter [%s] with an expected prerequisite value without defining the prerequisite parameter.", WARNING),
    PARAMETER_ALLOWED_VALUES_DUPLICATE_VALUE(           "TDL-078", "The test suite defines parameter [%s] with a duplicate allowed value [%s].", WARNING),
    PARAMETER_ALLOWED_VALUES_DUPLICATE_VALUE_LABEL(     "TDL-079", "The test suite defines parameter [%s] with a duplicate allowed value label [%s].", WARNING),
    PARAMETER_ALLOWED_VALUES_AND_LABELS_MISMATCH(       "TDL-080", "The test suite defines parameter [%s] with [%s] allowed values and [%s] labels.", ERROR),
    PARAMETER_ALLOWED_VALUE_LABELS_WITHOUT_VALUES(      "TDL-081", "The test suite defines parameter [%s] with allowed value labels but no values.", WARNING),

    INVALID_SCRIPTLET_SYNTAX(                           "TDL-082", "%s", ERROR),
    SCRIPTLET_REFERENCE_DID_NOT_MATCH_SCRIPTLET(        "TDL-083", "%s [%s] defines a call step referring to a scriptlet file within the test suite, but the referred file [%s] is not a scriptlet.", ERROR, true),
    UNEXPECTED_SCRIPTLET_INPUT(                         "TDL-084", "%s [%s] defines a call step for scriptlet [%s] that is providing an unexpected input [%s].", ERROR, true),
    MISSING_SCRIPTLET_INPUT(                            "TDL-085", "%s [%s] defines a call step for scriptlet [%s] that does not provide an expected input [%s].", ERROR, true),
    DUPLICATE_OUTPUT(                                   "TDL-086", "%s [%s] defines a %s step that requests the same output [%s] multiple times.", WARNING, true),
    UNEXPECTED_SCRIPTLET_OUTPUT(                        "TDL-087", "%s [%s] defines a call step for scriptlet [%s] that requests an unsupported output [%s].", WARNING, true),
    EXTERNAL_SCRIPTLET_USED(                            "TDL-088", "Calls are made to scriptlet [%s] from test suite [%s]. Ensure this will be available at runtime.", INFO),
    EXTERNAL_STATIC_IMPORT_USED(                        "TDL-089", "Imports are defined for resource [%s] from test suite [%s]. Ensure this will be available at runtime.", INFO),
    EXTERNAL_DYNAMIC_IMPORT_USED(                       "TDL-090", "Imports are made from test suite [%s] using expression [%s]. Ensure these will be available at runtime.", INFO),
    DUPLICATE_INPUT(                                    "TDL-091", "%s [%s] defines a %s step with a duplicate input [%s].", ERROR, true),
    DUPLICATE_CONFIG(                                   "TDL-092", "%s [%s] defines a %s step with a duplicate configuration value [%s].", ERROR, true),
    ACTOR_REFERENCES_IN_SCRIPTLET(                      "TDL-093", "Scriptlets reference actors %s in messaging steps. Ensure these will be defined by their calling test cases at runtime.", INFO),

    DUPLICATE_INTERNAL_SCRIPTLET_PROPERTY_NAME(         "TDL-094", "Test case [%s] includes a scriptlet [%s] that defines the same %s [%s] multiple times.", ERROR),
    DUPLICATE_PROPERTY_NAME(                            "TDL-095", "%s [%s] defines the same %s [%s] multiple times.", ERROR, true),
    INTERNAL_SCRIPTLET_OUTPUT_NOT_FOUND_AS_VARIABLE(    "TDL-096", "Test case [%s] includes a scriptlet [%s] with an output [%s] that is not defined through an expression, and for which no variable with the same name exists in scope.", ERROR),
    EXTERNAL_SCRIPTLET_OUTPUT_NOT_FOUND_AS_VARIABLE(    "TDL-097", "Scriptlet [%s] includes an output [%s] that is not defined through an expression, and for which no variable with the same name exists in scope.", ERROR),
    DUPLICATE_INTERNAL_SCRIPTLET_DEFINITION_NAME(       "TDL-098", "Test case [%s] includes a scriptlet [%s] that defines [%s] as a %s.", ERROR),
    DUPLICATE__DEFINITION_NAME(                         "TDL-099", "%s [%s] defines [%s] as a %s.", ERROR, true),

    ;

    private String code;
    private String message;
    private ErrorLevel level;
    private boolean prefixWithResourceType;

    ErrorCode(String code, String message, ErrorLevel level) {
        this(code, message, level, false);
    }

    ErrorCode(String code, String message, ErrorLevel level, boolean prefixWithResourceType) {
        this.code = code;
        this.message = message;
        this.level = level;
        this.prefixWithResourceType = prefixWithResourceType;
    }

    public String getCode() {
        return code;
    }

    public String getMessage(String... arguments) {
        if (arguments == null || arguments.length == 0) {
            return "["+code+"] " + message;
        }
        Object[] args = Arrays.copyOf(arguments, arguments.length);
        return "["+code+"] " + String.format(message, args);
    }

    public ErrorLevel getLevel() {
        return level;
    }

    public boolean isPrefixWithResourceType() {
        return prefixWithResourceType;
    }
}
