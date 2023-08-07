<#import "fragments/commonStyles.ftl" as commonStyles>
<#import "fragments/commonBlocks.ftl" as common>
<html>
	<head>
	    <style>
	        <@commonStyles.basic />
	        <@commonStyles.testResult />
	        <@commonStyles.testCoverage />
            .column.left {
                width: 39%;
            }
            .column.right {
                width: 60%;
            }
            .test-suite-container {
                border: 1px solid #000000;
                margin-top: 10px;
                border-radius: 5px;
                padding: 10px;
                background: #ffffff;
            }
            .test-suite-content {
                margin-top: 10px;
            }
            .test-suite-header {
                display: block;
            }
            .test-suite-name {
                display: inline-block;
                font-weight: bold;
            }
            .test-suite-description {
                display: inline-block;
                border-left: 1px solid #7c7c7c;
                padding-left: 10px;
                margin-left: 10px;
                margin-right: 20px;
            }
            .test-suite-status {
                display: inline;
                float: right;
                padding-right: 11px;
            }
            .test-case-status.icon img, .test-suite-status.icon img {
                margin-top: -3px;
            }
            .test-case-container {
                border: 1px solid #000000;
                margin-top: 5px;
                border-radius: 5px;
                padding: 5px 10px;;
                background: #efefef;
                page-break-inside: avoid;
            }
            .test-case-name {
                display: inline-block;
                font-weight: bold;
            }
            .test-case-name.no-link {
                font-weight: normal;
            }
            .test-case-name.ignored {
                font-style: italic;
            }
            .test-case-description {
                display: inline-block;
                border-left: 1px solid #7c7c7c;
                padding-left: 10px;
                margin-left: 10px;
                margin-right: 20px;
            }
            .test-case-status {
                display: inline;
                float: right;
            }
            .test-suite-header-texts {
                display: inline-block;
                width: 95%;
            }
            .test-case-texts {
                display: inline-block;
                width: 95%;
            }
            .test-suite-name {
                min-width: 180px;
            }
            .test-suite-description, .test-case-name, test-case-description {
                min-width: 170px;
            }
            .test-suites td {
                vertical-align: top;
            }
            .section-title-note {
                padding-left: 10px;
                padding-right: 10px;
            }
	    </style>
    </head>
    <body>
        <div class="title">${escape(title)}</div>
        <#if includeMessage?? && includeMessage>
            <div class="report-message">
                ${message}
            </div>
        </#if>
        <#if includeDetails?? && includeDetails>
            <div class="section details">
                <div class="section-title">
                    <div>Overview</div>
                </div>
                <div class="section-content">
                    <div class="columns">
                        <div class="column left">
                            <table>
                                <tr>
                                    <td class="cell-label">${escape(labelDomain)}:</td>
                                    <td class="cell-value">${escape(testDomain)}</td>
                                </tr>
                                <tr>
                                    <td class="cell-label">${escape(labelSpecification)}:</td>
                                    <td class="cell-value">${escape(testSpecification)}</td>
                                </tr>
                                <tr>
                                    <td class="cell-label">${escape(labelActor)}:</td>
                                    <td class="cell-value">${escape(testActor)}</td>
                                </tr>
                            </table>
                        </div>
                        <div class="column right">
                            <table>
                                <tr>
                                    <td class="cell-label">${escape(labelOrganisation)}:</td>
                                    <td class="cell-value">${escape(organisation)}</td>
                                </tr>
                                <tr>
                                    <td class="cell-label">${escape(labelSystem)}:</td>
                                    <td class="cell-value">${escape(system)}</td>
                                </tr>
                            </table>
                        </div>
                    </div>
                    <div class="separator"></div>
                    <div class="columns">
                        <div class="column left">
                            <table>
                                <tr>
                                    <td class="cell-label">Report date:</td>
                                    <td class="cell-value">${reportDate}</td>
                                </tr>
                                <#if includeTestStatus?? && includeTestStatus>
                                    <tr>
                                        <td class="cell-label">Status:</td>
                                        <td class="cell-value"><div class="value-inline result background-${overallStatus}">${printResult(overallStatus)}</div></td>
                                    </tr>
                                </#if>
                            </table>
                        </div>
                        <div class="column right">
                            <#if includeTestStatus?? && includeTestStatus>
                                <table>
                                    <tr>
                                        <td class="cell-label">Test results:</td>
                                        <td class="cell-value">${testStatus}</td>
                                    </tr>
                                    <tr>
                                        <td class="cell-label">Result ratio:</td>
                                        <td class="cell-value">${coverageBlock(completedTests, failedTests, undefinedTests, 350)}</td>
                                    </tr>
                                </table>
                            </#if>
                        </div>
                    </div>
                </div>
            </div>
        </#if>
        <#if testSuites??>
            <div id="test-cases" class="section test-suites">
                <div class="section-title">
                    <div>Test cases</div>
                </div>
                <#if hasOptionalTests || hasDisabledTests>
                    <div class="section-title-note">
                        <#if hasOptionalTests && hasDisabledTests>
                            <b>Note:</b> The list below includes test cases that are optional (*) and disabled (**), displayed also in italics, that do not count towards the
                            overall conformance status.
                        <#elseif hasOptionalTests>
                            <b>Note:</b> The list below includes test cases that are optional (*), displayed also in italics, that do not count towards the
                            overall conformance status.
                        <#else>
                            <b>Note:</b> The list below includes test cases that are disabled (**), displayed also in italics, that do not count towards the
                            overall conformance status.
                        </#if>
                    </div>
                </#if>
                <div class="section-content">
                    <#assign overallIndex = 0>
                    <#list testSuites as testSuite>
                        <#assign tsIndex = testSuite?counter>
                        <div class="test-suite-container">
                            <div class="test-suite-header">
                                <div class="test-suite-header-texts">
                                    <table>
                                        <tr>
                                            <td><div class="test-suite-name"><div>${escape(testSuite.testSuiteName)}</div></div></td>
                                            <#if testSuite.testSuiteDescription??>
                                                <td><div class="test-suite-description"><div>${escape(testSuite.testSuiteDescription)}</div></div></td>
                                            </#if>
                                        </tr>
                                    </table>
                                </div>
                                <div class="test-suite-status icon value-inline">
                                    <img src="classpath:reports/images/icon-${testSuite.overallStatus}.png"/>
                                </div>
                            </div>
                            <div class="test-suite-content">
                                <#list testSuite.testCases as testCase>
                                    <#assign index = testCase?counter>
                                    <#assign overallIndex = overallIndex + 1>
                                    <div class="test-case-container">
                                        <div class="test-case-texts">
                                            <table>
                                                <tr>
                                                    <td><div class="test-case-name <#if testCase.optional || testCase.disabled>ignored</#if>"><div><#if includeTestCases?? && includeTestCases><a class="page-link" href="#test-${tsIndex}-${index}">#${overallIndex}</a>: </#if>${escape(testCase.testName)}<#if testCase.optional> *</#if><#if testCase.disabled> **</#if></div></div></td>
                                                    <#if testCase.testDescription??>
                                                        <td><div class="test-case-description"><div>${escape(testCase.testDescription)}</div></div></td>
                                                    </#if>
                                                </tr>
                                            </table>
                                        </div>
                                        <div class="test-case-status icon value-inline"><img src="classpath:reports/images/icon-${testCase.reportResult}.png"/></div>
                                    </div>
                                </#list>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>
            <#if includeTestCases?? && includeTestCases>
                <div class="test-case-reports">
                    <#list testSuites as testSuite>
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
                                            <table>
                                                <tr>
                                                    <td class="cell-label">Description:</td>
                                                    <td class="cell-value">${escape(testCase.testDescription)}</td>
                                                </tr>
                                            </table>
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
