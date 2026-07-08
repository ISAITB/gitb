<#import "fragments/commonStyles.ftl" as commonStyles>
<#import "fragments/commonBlocks.ftl" as common>
<html>
	<head>
	    <style>
	        <@commonStyles.basic data.includePageNumbers/>
	        <@commonStyles.testResult />
	        .test-suite-listing {
	            border: 1px solid #000000;
	            border-radius: 5px;
	            margin-top: 10px;
	            padding: 10px;
	            background: #ffffff;
	        }
	        .test-suite-listing-header {
	            font-size: 14px;
	            font-weight: bold;
	        }
	        .test-suite-listing .step-pills {
	            margin-top: 8px;
	            margin-bottom: 0;
	        }
	        .test-suite-listing .step-pill {
	            padding: 5px 10px;
	        }
	        .documentation-page .documentation-content, .statement-documentation {
	            margin-top: 20px;
	        }
	        .documentation-content > *:first-child {
	            margin-top: 0;
	        }
	        .columns.statement-documentation-overview > .column.left {
	            width: 65%;
	        }
	        .columns.statement-documentation-overview > .column.right {
	            width: 34%;
	        }
	    </style>
    </head>
    <body id="top">
        <div class="title">${escape(data.title)}</div>
        <#assign hasContentBefore = false>
        <#if data.includeOverview>
            <#assign hasContentBefore = true>
            <div class="section details">
                <div class="section-title">
                    <div>Overview</div>
                </div>
                <div class="section-content">
                    <div class="columns statement-documentation-overview">
                        <div class="column left">
                            <table>
                                <tr>
                                    <td class="cell-label">${escape(data.labelDomain)}:</td>
                                    <td class="cell-value">${escape(data.testDomain)}</td>
                                </tr>
                                <#if data.testSpecificationGroup??>
                                    <tr>
                                        <td class="cell-label">${escape(data.labelSpecificationGroup)}:</td>
                                        <td class="cell-value">${escape(data.testSpecificationGroup)}</td>
                                    </tr>
                                    <tr>
                                        <td class="cell-label">${escape(data.labelSpecificationInGroup)}:</td>
                                        <td class="cell-value">${escape(data.testSpecification)}</td>
                                    </tr>
                                <#else>
                                    <tr>
                                        <td class="cell-label">${escape(data.labelSpecification)}:</td>
                                        <td class="cell-value">${escape(data.testSpecification)}</td>
                                    </tr>
                                </#if>
                                <#if data.testActor??>
                                    <tr>
                                        <td class="cell-label">${escape(data.labelActor)}:</td>
                                        <td class="cell-value">${escape(data.testActor)}</td>
                                    </tr>
                                </#if>
                            </table>
                        </div>
                        <div class="column right">
                            <table>
                                <tr>
                                    <td class="cell-label">Report date:</td>
                                    <td class="cell-value">${data.reportDate}</td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
            <div class="section-title">
                <div></div>
            </div>
        </#if>
        <#if data.includeStatementDocumentation && data.statementDocumentation??>
            <#assign hasContentBefore = true>
            <div class="section no-margin">
                <div class="section-content statement-documentation">
                    ${data.statementDocumentation}
                </div>
            </div>
        </#if>
        <#if data.includeTestCaseListing && data.testSuites?? && (data.includeTestSuiteDocumentation || data.includeTestCaseDocumentation)>
            <#assign hasContentBefore = true>
            <#if data.includeStatementDocumentation && data.statementDocumentation??>
                <page-before/>
                <div id="test-cases" class="title">Test cases</div>
                <div class="section">
            <#else>
                <div class="section" id="test-cases">
            </#if>
                <div class="section-content">
                    <#list data.testSuites as testSuite>
                        <#assign tsIndex = testSuite?counter>
                        <#assign documentedCases = []>
                        <#if testSuite.testCases?? && data.includeTestCaseDocumentation>
                            <#list testSuite.testCases as testCase>
                                <#if testCase.documentation??>
                                    <#assign documentedCases = documentedCases + [testCase]>
                                </#if>
                            </#list>
                        </#if>
                        <#assign suiteHasDoc = (data.includeTestSuiteDocumentation && testSuite.documentation??)>
                        <#if (documentedCases?size > 0) || suiteHasDoc>
                            <div class="test-suite-listing">
                                <div class="test-suite-listing-header">
                                    <#if suiteHasDoc><a class="page-link" href="#suite-${tsIndex}">${escape(testSuite.name)}</a><#else>${escape(testSuite.name)}</#if>
                                </div>
                                <#if (documentedCases?size > 0)>
                                    <div class="step-pills">
                                        <#list testSuite.testCases as testCase>
                                            <#assign tcIndex = testCase?counter>
                                            <#if testCase.documentation??>
                                                <div class="step-pill">
                                                    <div class="step-text value-inline"><a class="page-link" href="#case-${tsIndex}-${tcIndex}">${escape(testCase.name)}</a></div>
                                                </div>
                                            </#if>
                                        </#list>
                                    </div>
                                </#if>
                            </div>
                        </#if>
                    </#list>
                </div>
            </div>
        </#if>
        <#if data.testSuites?? && (data.includeTestSuiteDocumentation || data.includeTestCaseDocumentation)>
            <#list data.testSuites as testSuite>
                <#assign tsIndex = testSuite?counter>
                <#if data.includeTestSuiteDocumentation && testSuite.documentation??>
                    <#if hasContentBefore || tsIndex gt 1>
                      <page-before/>
                    </#if>
                    <#assign hasContentBefore = true>
                    <div id="suite-${tsIndex}" class="documentation-page">
                        <#if data.includeTestCaseListing>
                            <@common.subTitle "Test suite: "+testSuite.name "test-cases"/>
                        <#else>
                            <@common.subTitle "Test suite: "+testSuite.name />
                        </#if>
                        <div class="documentation-content">
                            ${testSuite.documentation}
                        </div>
                    </div>
                </#if>
                <#if data.includeTestCaseDocumentation && testSuite.testCases??>
                    <#list testSuite.testCases as testCase>
                        <#assign tcIndex = testCase?counter>
                        <#if testCase.documentation??>
                            <#if hasContentBefore || tcIndex gt 1>
                              <page-before/>
                            </#if>
                            <#assign hasContentBefore = true>
                            <div id="case-${tsIndex}-${tcIndex}" class="documentation-page">
                                <#if data.includeTestCaseListing>
                                    <@common.subTitle "Test case: "+testCase.name "test-cases"/>
                                <#else>
                                    <@common.subTitle "Test case: "+testCase.name />
                                </#if>
                                <div class="documentation-content">
                                    ${testCase.documentation}
                                </div>
                            </div>
                        </#if>
                    </#list>
                </#if>
            </#list>
        </#if>
    </body>
</html>