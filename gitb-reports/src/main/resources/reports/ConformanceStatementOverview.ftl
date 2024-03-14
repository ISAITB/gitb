<#import "fragments/commonStyles.ftl" as commonStyles>
<#import "fragments/commonBlocks.ftl" as common>
<html>
	<head>
	    <style>
	        <@commonStyles.basic data.includePageNumbers/>
	        <@commonStyles.testResult />
	        <@commonStyles.testCoverage />
	        <@commonStyles.conformanceStatement />
	    </style>
    </head>
    <body>
        <#if data.title??><div class="title">${escape(data.title)}</div></#if>
        <#if data.includeMessage?? && data.includeMessage>
            <div class="report-message">
                ${data.message}
            </div>
        </#if>
        <#if data.includeDetails?? && data.includeDetails>
            <@common.statementOverview data=data labelDomain=data.labelDomain labelSpecification=data.labelSpecification labelSpecificationGroup=data.labelSpecificationGroup labelSpecificationInGroup=data.labelSpecificationInGroup labelActor=data.labelActor labelOrganisation=data.labelOrganisation labelSystem=data.labelSystem includeTestStatus=data.includeTestStatus organisation=data.organisation system=data.system reportDate=data.reportDate/>
        </#if>
        <#if data.testSuites??>
            <@common.statementTestCases data=data includeTestCaseReports=data.includeTestCases/>
            <#if data.includeTestCases?? && data.includeTestCases>
                <div class="test-case-reports">
                    <#list data.testSuites as testSuite>
                        <#assign tsIndex = testSuite?counter>
                        <#list testSuite.testCases as testCase>
                            <#assign index = testCase?counter>
                            <page-before/>
                            <div id="test-${tsIndex}-${index}" class="test-case-report">
                                <@common.subTitle testCase.title "test-cases"/>
                                <div class="section">
                                    <div class="section-title">
                                        <div>Overview</div>
                                    </div>
                                    <div class="section-content">
                                       <div class="columns">
                                            <div class="column left">
                                                <table>
                                                    <tr>
                                                        <td class="cell-label">Test case:</td>
                                                        <td class="cell-value">${escape(testCase.testName)}</td>
                                                    </tr>
                                                </table>
                                            </div>
                                            <div class="column right">
                                                <table>
                                                    <tr>
                                                        <td class="cell-label">Test suite:</td>
                                                        <td class="cell-value">${escape(testSuite.testSuiteName)}</td>
                                                    </tr>
                                                </table>
                                            </div>
                                        </div>
                                        <#if testCase.testDescription??>
                                           <div class="columns">
                                               <div class="column single">
                                                    <table>
                                                        <tr>
                                                            <td class="cell-label">Description:</td>
                                                            <td class="cell-value">${escape(testCase.testDescription)}</td>
                                                        </tr>
                                                    </table>
                                               </div>
                                           </div>
                                        </#if>
                                        <#if testCase.specReference?? || testCase.specDescription?? || testCase.specLink??>
                                            <div class="separator"></div>
                                            <div class="columns">
                                                <div class="column single">
                                                    <table>
                                                        <tr>
                                                            <td class="cell-label">Reference:</td>
                                                            <td class="cell-value"><@common.specificationInfo testCase.specReference testCase.specDescription testCase.specLink/>
                                                        </tr>
                                                    </table>
                                                </div>
                                            </div>
                                        </#if>
                                        <div class="separator"></div>
                                        <div class="session-result">
                                            <div class="columns">
                                                <div class="column left">
                                                    <table>
                                                        <tr>
                                                            <td class="cell-label">Result:</td>
                                                            <td class="cell-value"><div class="value-inline result background-${testCase.reportResult}">${testCase.reportResult}</div></td>
                                                        </tr>
                                                    </table>
                                                </div>
                                                <div class="column right">
                                                    <table>
                                                        <tr>
                                                            <td class="cell-label">Start time:</td>
                                                            <td class="cell-value">${testCase.startTime}</td>
                                                            <#if testCase.endTime??>
                                                                <td class="cell-label-end-time">End time:</td>
                                                                <td class="cell-value">${testCase.endTime}</td>
                                                            </#if>
                                                        </tr>
                                                    </table>
                                                </div>
                                            </div>
                                            <#if testCase.outputMessage??>
                                                <div class="row output-message ${testCase.reportResult}">
                                                    ${escape(testCase.outputMessage)}
                                                </div>
                                            </#if>
                                        </div>
                                    </div>
                                </div>
                                <#if testCase.steps??>
                                    <div class="section">
                                        <div class="section-title">
                                            <div>Steps</div>
                                        </div>
                                        <div class="section-content">
                                            <@common.printStepsOverview testCase.steps false />
                                        </div>
                                    </div>
                                </#if>
                            </div>
                        </#list>
                    </#list>
                </div>
            </#if>
        </#if>
    </body>
</html>
