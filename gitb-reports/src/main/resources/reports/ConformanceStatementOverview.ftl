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
            .test-case-container.disabled .test-case-first-line .test-case-name > a {
                color: #847ef0;
            }
            .test-case-container.disabled .test-case-first-line .test-case-name {
                color: #B4B4B4;
            }
            .test-case-container.disabled .test-case-description {
                color: #B4B4B4;
            }
            .test-case-prescription-level {
                padding-right: 20px;
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
                padding: 5px 10px;
                background: #efefef;
                page-break-inside: avoid;
            }
            .test-case-name {
                display: inline-block;
                font-weight: bold;
            }
            .test-case-description {
                display: inline-block;
                margin-top: 5px;
                margin-bottom: 5px;
                width: 99%;
            }
            .test-case-first-line-start {
                display: inline-block;
            }
            .test-case-status {
                display: inline;
                float: right;
            }
            .test-case-table {
                width: 950px;
            }
            .test-case-prescription-td {
                width: 20px;
            }
            .test-case-name-td {
            }
            .test-case-tags-td {
                text-align: right;
                vertical-align: top;
            }
            .test-case-tags {
                padding-right: 10px;
                padding-left: 10px;
            }
            .test-case-tag {
                display: inline;
                border-radius: 15px;
                padding-left: 5px;
                padding-right: 5px;
                margin-right: 3px;
                font-weight: bold;
                font-size: smaller;
                white-space: nowrap;
            }
            .test-suite-header-texts {
                display: inline-block;
                width: 95%;
            }
            .test-suites td {
                vertical-align: top;
            }
            .test-case-legend {
                border: 1px solid #404040;
                margin-top: 10px;
                border-radius: 5px;
                background: #404040;
                page-break-inside: avoid;
                position: relative;
            }
            .legend-prescription-text {
                padding-left: 5px;
                padding-right: 30px;
                font-style: italic;
            }
            .legend-tags-div.with-padding {
                padding-top: 10px;
            }
            .legend-tag-description {
                padding-left: 10px;
                padding-top: 3px;
                padding-bottom: 3px;
                font-style: italic;
            }
            .legend-highlight {
                position: absolute;
                top: 8px;
                left: 6px;
            }
            .legend-highlight > img {
                width: 24px;
                margin-top: -4px;
            }
            .legend-content {
                margin-left: 36px;
                background: #efefef;
                padding: 10px;
                border-top-right-radius: 5px;
                border-bottom-right-radius: 5px;
            }
            .legend-tag-pill .test-case-tag {
                margin-top: 3px;
                margin-bottom: 3px;
            }
	    </style>
    </head>
    <body>
        <#if title??><div class="title">${escape(title)}</div></#if>
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
                                    <div class="test-case-container<#if testCase.disabled> disabled</#if>">
                                        <div class="test-case-first-line">
                                            <div class="test-case-first-line-start">
                                                <table class="test-case-table">
                                                    <tr>
                                                        <#if hasOptionalTests || hasDisabledTests>
                                                            <td class="test-case-prescription-td"><div class="test-case-prescription-level icon"><img src="classpath:reports/images/icon-<#if testCase.disabled>disabled<#elseif testCase.optional>optional<#else>required</#if>.png"/></div></td>
                                                        </#if>
                                                        <td class="test-case-name-td"><div class="test-case-name"><#if includeTestCases?? && includeTestCases><a class="page-link" href="#test-${tsIndex}-${index}"></#if>${escape(testCase.testName)}<#if includeTestCases?? && includeTestCases></a></#if></div></td>
                                                        <#if testCase.tags??>
                                                            <td class="test-case-tags-td"><div class="test-case-tags"><#list testCase.tags as tag><div class="test-case-tag" style="background-color: ${tag.background()}; color: ${tag.foreground()}">${escape(tag.name())}</div></#list></div></td>
                                                        </#if>
                                                    </tr>
                                                </table>
                                            </div>
                                            <div class="test-case-status icon value-inline"><img src="classpath:reports/images/icon-${testCase.reportResult}.png"/></div>
                                        </div>
                                        <#if testCase.testDescription?? && testCase.testDescription != "">
                                            <div class="test-case-description"><div>${escape(testCase.testDescription)}</div></div>
                                        </#if>
                                    </div>
                                </#list>
                            </div>
                        </div>
                    </#list>
                    <#if hasOptionalTests || hasDisabledTests || distinctTags??>
                        <div class="test-case-legend">
                            <div class="legend-highlight"><img src="classpath:reports/images/icon-legend.png"/></div>
                            <div class="legend-content">
                                <#if hasOptionalTests || hasDisabledTests>
                                    <div class="legend-prescription-div">
                                        <table class="legend-prescription-table">
                                            <tr>
                                                <#if hasRequiredTests>
                                                    <td class="legend-prescription-icon icon"><img src="classpath:reports/images/icon-required.png"/></td>
                                                    <td class="legend-prescription-text">Required test case.</td>
                                                </#if>
                                                <#if hasOptionalTests>
                                                    <td class="legend-prescription-icon icon"><img src="classpath:reports/images/icon-optional.png"/></td>
                                                    <td class="legend-prescription-text">Optional test case.</td>
                                                </#if>
                                                <#if hasDisabledTests>
                                                    <td class="legend-prescription-icon icon"><img src="classpath:reports/images/icon-disabled.png"/></td>
                                                    <td class="legend-prescription-text">Disabled test case.</td>
                                                </#if>
                                            </tr>
                                        </table>
                                    </div>
                                </#if>
                                <#if distinctTags??>
                                    <div class="legend-tags-div <#if hasOptionalTests || hasDisabledTests>with-padding</#if>">
                                        <table class="legend-tag-table">
                                            <#list distinctTags as tag>
                                                <tr>
                                                    <td class="legend-tag-pill"><div class="test-case-tag" style="background-color: ${tag.background()}; color: ${tag.foreground()}"
                                                    >${escape(tag.name())}</div></td>
                                                    <td class="legend-tag-description">${escape(tag.description())}</td>
                                                </tr>
                                            </#list>
                                        </table>
                                    </div>
                                </#if>
                            </div>
                        </div>
                    </#if>
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
