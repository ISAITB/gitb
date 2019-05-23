package com.gitb.vs.tdl;

import java.util.Arrays;

import static com.gitb.vs.tdl.ErrorLevel.*;

public enum ErrorCode {

    INVALID_ZIP_ARCHIVE(                                "TDL-001", "The test suite archive is not a valid ZIP archive.", ERROR),
    NO_TEST_SUITE_FOUND(                                "TDL-002", "The provided archive does not contain a test suite file.", ERROR),
    MULTIPLE_TEST_SUITES_FOUND(                         "TDL-003", "Only a single test suite file should be included. The provided archive contains %s test suites.", ERROR),
    DUPLICATE_TEST_CASE_ID(                             "TDL-004", "Multiple test cases found with ID [%s].", ERROR),
    DUPLICATE_TEST_SUITE_ID(                            "TDL-005", "Multiple test suites found with ID [%s].", ERROR),
    INVALID_TEST_SUITE_SYNTAX(                          "TDL-006", "%s", ERROR),
    INVALID_TEST_CASE_SYNTAX(                           "TDL-007", "%s", ERROR),
    NO_TEST_CASE_FOUND(                                 "TDL-008", "The provided archive must contain at least one test case.", ERROR),
    INVALID_TEST_CASE_REFERENCE(                        "TDL-009", "The test suite references a test case using an undefined ID [%s].", ERROR),
    INVALID_TEST_CASE_PREREQUISITE(                     "TDL-010", "The test suite specifies a prerequisite [%s] for test case [%s] that is not defined.", ERROR),
    INVALID_ACTOR_ID_REFERENCED_IN_TEST_CASE(           "TDL-011", "Test case [%s] references an actor by ID [%s] that is not defined in the test suite.", ERROR),
    NO_SUT_DEFINED_IN_TEST_CASE(                        "TDL-012", "Test case [%s] does not define an actor with role 'SUT'.", ERROR),
    TEST_CASE_REFERENCES_ACTOR_MULTIPLE_TIMES(          "TDL-013", "Test case [%s] defines multiple references to the same actor [%s].", ERROR),
    TEST_SUITE_DEFINES_ACTOR_MULTIPLE_TIMES(            "TDL-014", "The test suite defines actor [%s] multiple times.", ERROR),
    ACTOR_NOT_REFERENCED_IN_TEST_CASES(                 "TDL-015", "Actor [%s] is not referenced in any test cases.", WARNING),
    INVALID_EXTERNAL_ACTOR_REFERENCE(                   "TDL-016", "Test suite references an external actor [%s] that is not defined.", ERROR),
    INVALID_EXTERNAL_PARAMETER_REFERENCE(               "TDL-017", "Test case [%s] references a domain parameter [%s] in step [%s] that is not defined.", WARNING),
    TEST_CASE_NOT_REFERENCED(                           "TDL-018", "Test case [%s] is not referenced in the test suite.", WARNING),
    DEFAULT_ACTOR_NOT_REFERENCED_IN_TEST_CASES_AS_SUT(  "TDL-019", "The default actor for the specification [%s] is not referenced as a SUT in any test cases.", WARNING),
    INVALID_TEST_CASE_IMPORT(                           "TDL-020", "Test case [%s] defines an import [%s] that doesn't exist within the test suite.", ERROR),
    UNUSED_RESOURCE(                                    "TDL-021", "Resource [%s] is included in the test suite archive but is never used.", WARNING),
    INVALID_EMBEDDED_MESSAGING_HANDLER_REFERENCE(       "TDL-022", "Test case [%s] references a messaging handler [%s] that does not exist.", ERROR),
    INVALID_EMBEDDED_PROCESSING_HANDLER_REFERENCE(      "TDL-023", "Test case [%s] references a processing handler [%s] that does not exist.", ERROR),
    INVALID_EMBEDDED_VALIDATION_HANDLER_REFERENCE(      "TDL-024", "Test case [%s] references a validation handler [%s] that does not exist.", ERROR),
    INVALID_TX_REFERENCE_FOR_MESSAGING_END(             "TDL-025", "Test case [%s] specifies a messaging transaction end (etxn) for non-existent transaction ID [%s].", ERROR),
    INVALID_TX_REFERENCE_FOR_PROCESSING_END(            "TDL-026", "Test case [%s] specifies a processing transaction end (eptxn) for non-existent transaction ID [%s].", ERROR),
    INVALID_TX_REFERENCE_FOR_MESSAGING_STEP(            "TDL-027", "Test case [%s] specifies a messaging step (%s) that refers to non-existent transaction ID [%s].", ERROR),
    INVALID_TX_REFERENCE_FOR_PROCESSING_STEP(           "TDL-028", "Test case [%s] specifies a process step that refers to non-existent transaction ID [%s].", ERROR),
    MESSAGING_STEP_OUTSIDE_TX(                          "TDL-029", "Test case [%s] specifies a messaging step (%s) that is not contained within a transaction.", ERROR),
    PROCESSING_STEP_OUTSIDE_TX(                         "TDL-030", "Test case [%s] specifies a process step that is not contained within a transaction nor defines its own handler.", ERROR),
    MESSAGING_TX_END_WITHOUT_START(                     "TDL-031", "Test case [%s] specifies a messaging transaction end (etxn) without a corresponding begin (btxn).", ERROR),
    PROCESSING_TX_END_WITHOUT_START(                    "TDL-032", "Test case [%s] specifies a processing transaction end (eptxn) without a corresponding begin (bptxn).", ERROR),
    INVALID_ACTOR_REFERENCE_IN_STEP(                    "TDL-033", "Test case [%s] defines a [%s] step with an invalid actor reference [%s].", ERROR),
    REFERENCED_ACTOR_IN_STEP_HAS_UNEXPECTED_ROLE(       "TDL-034", "Test case [%s] defines a [%s] step referencing actor [%s] with invalid role [%s]. The referenced actor must have role [%s].", ERROR),
    INVALID_ENCODING(                                   "TDL-035", "Test case [%s] defines an invalid encoding value [%s] in a [%s] element.", ERROR),
    INVALID_DATA_TYPE_REFERENCE(                        "TDL-036", "Test case [%s] defines an invalid data type [%s].", ERROR),
    INVALID_SCRIPTLET_REFERENCE(                        "TDL-037", "Test case [%s] references an undefined scriptlet [%s].", ERROR),
    UNUSED_SCRIPTLET(                                   "TDL-038", "Test case [%s] defines a scriptlet [%s] but never calls it.", WARNING),
    INVALID_VARIABLE_REFERENCE(                         "TDL-039", "Test case [%s] in step [%s] defines an invalid variable reference [%s].", ERROR),
    VARIABLE_NOT_IN_SCOPE(                              "TDL-040", "Test case [%s] in step [%s] refers to an undefined variable [%s].", ERROR),
    SIMPLE_VARIABLE_REFERENCED_AS_CONTAINER(            "TDL-041", "Test case [%s] in step [%s] refers to a simple variable [%s] as if it was a container type.", ERROR),
    INVALID_EXPRESSION(                                 "TDL-042", "Test case [%s] in step [%s] defines an invalid expression [%s].", ERROR),
    UNEXPECTED_HANDLER_INPUT(                           "TDL-043", "Test case [%s] in step [%s] defines an unexpected input [%s].", WARNING),
    MISSING_HANDLER_INPUT(                              "TDL-044", "Test case [%s] in step [%s] does not provide a required input [%s].", ERROR),
    UNEXPECTED_HANDLER_CONFIG(                          "TDL-045", "Test case [%s] in step [%s] defines an unexpected configuration parameter [%s].", WARNING),
    MISSING_HANDLER_CONFIG(                             "TDL-046", "Test case [%s] in step [%s] does not provide a required configuration parameter [%s].", ERROR),
    INVALID_PROCESSING_HANDLER_OPERATION(               "TDL-047", "Test case [%s] in step [%s] refers to invalid processing operation [%s].", ERROR),
    DUPLICATE_SCRIPTLET_ID(                             "TDL-048", "Test case [%s] defines multiple scriptlets for ID [%s].", ERROR),
    MISSING_LIST_CONTAINED_TYPE(                        "TDL-049", "Test case [%s] defines a 'list' variable with no contained type. A default of 'string' is considered but it is best to define it explicitly (e.g. 'list[string]').", WARNING),
    MISSING_INTERACTION_OPTIONS(                        "TDL-050", "Test case [%s] defines a user interaction request with a [%s] attribute but no [options] attribute. The [%s] attribute will be ignored.", WARNING),
    INTERACTION_OPTIONS_FOR_NON_STRING_INPUT(           "TDL-051", "Test case [%s] defines a user interaction request of non-string [contentType] (%s) with a [options] attribute. The [options] attribute will be ignored.", WARNING),
    INTERACTION_OPTIONS_AND_LABELS_MISMATCH(            "TDL-052", "Test case [%s] defines a user interaction request with an [options] attribute. The number of options [%s] must match the number of labels [%s].", ERROR),
    INTERACTION_OPTIONS_SINGLE_OPTION(                  "TDL-053", "Test case [%s] defines a user interaction request with an [options] attribute but only a single option is defined.", WARNING),
    MISSING_TX_AND_HANDLER_FOR_PROCESSING_STEP(         "TDL-054", "Test case [%s] defines a process step with no transaction ID reference and no handler definition.", ERROR),
    MESSAGING_TX_NOT_CLOSED(                            "TDL-055", "Test case [%s] defines a messaging transaction [%s] that is not closed.", WARNING),
    PROCESSING_TX_NOT_CLOSED(                           "TDL-056", "Test case [%s] defines a processing transaction [%s] that is not closed.", WARNING),
    MESSAGING_TX_NOT_USED(                              "TDL-057", "Test case [%s] defines a messaging transaction [%s] that is never used.", WARNING),
    PROCESSING_TX_NOT_USED(                             "TDL-058", "Test case [%s] defines a processing transaction [%s] that is never used.", WARNING),
    DOUBLE_PROCESSING_HANDLER(                          "TDL-059", "Test case [%s] defines a process step that defined both a transaction reference [%s] and a handler [%s].", ERROR),
    VALUE_OF_MAP_VARIABLE_WITHOUT_NAME_OR_TYPE(         "TDL-060", "Test case [%s] defines for map variable [%s] a value with no name or type.", ERROR),
    VALUE_OF_NON_MAP_VARIABLE_WITH_NAME_OR_TYPE(        "TDL-061", "Test case [%s] defines for variable [%s] a value with name and type information that will be ignored.", WARNING),
    MULTIPLE_VALUES_FOR_PRIMITIVE_VARIABLE(             "TDL-062", "Test case [%s] defines a simple variable [%s] with multiple values.", ERROR);

    private String code;
    private String message;
    private ErrorLevel level;

    ErrorCode(String code, String message, ErrorLevel level) {
        this.code = code;
        this.message = message;
        this.level = level;
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
}
