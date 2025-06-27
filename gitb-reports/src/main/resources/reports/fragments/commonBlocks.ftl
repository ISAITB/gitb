<#macro subTitle text link="">
    <div class="sub-title">
        <div class="sub-title-text">${escape(text)}</div>
        <#if link != "">
            <div class="sub-title-link"><a href="#${link}">Top</a></div>
        </#if>
    </div>
</#macro>
<#macro printStepsOverview steps withLinks=false>
    <div class="step-pills"><#list steps as step><#assign index = step?counter><div class="step-pill">
        <div class="step-text value-inline"><#if withLinks><a class="page-link" href="#step-${index}">${escape(step.title?keep_before(":"))}</a><#else>${escape(step.title?keep_before(":"))}</#if><#if (step.title?keep_after(":"))?has_content>: </#if>${escape(step.title?keep_after(":"))}</div>
        <div class="step-icon icon value-inline"><img class="icon-img" src="classpath:reports/images/icon-${step.reportResult}.svg"/></div>
    </div></#list></div>
</#macro>
<#macro specificationInfo reference="" description="" link=""><#if reference != ""><span class="spec-reference"><#if link != ""><a href="${link}"></#if>${escape(reference)}<#if link != ""></a></#if></span><#elseif link != ""><a href="${link}">Link</a></#if><#if description != ""><#if reference != "" || link != ""><span class="inline-text-separator">|</span></#if><span class="spec-description">${escape(description)}</#if></#macro>
<#macro statementOverview data labelDomain labelSpecification labelSpecificationGroup labelSpecificationInGroup labelActor labelOrganisation labelSystem includeTestStatus organisation system reportDate>
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
                            <td class="cell-value">${escape(data.testDomain)}</td>
                        </tr>
                        <#if data.testSpecificationGroup??>
                            <tr>
                                <td class="cell-label">${escape(labelSpecificationGroup)}:</td>
                                <td class="cell-value">${escape(data.testSpecificationGroup)}</td>
                            </tr>
                            <tr>
                                <td class="cell-label">${escape(labelSpecificationInGroup)}:</td>
                                <td class="cell-value">${escape(data.testSpecification)}</td>
                            </tr>
                        <#else>
                            <tr>
                                <td class="cell-label">${escape(labelSpecification)}:</td>
                                <td class="cell-value">${escape(data.testSpecification)}</td>
                            </tr>
                        </#if>
                        <#if data.testActor??>
                            <tr>
                                <td class="cell-label">${escape(labelActor)}:</td>
                                <td class="cell-value">${escape(data.testActor)}</td>
                            </tr>
                        </#if>
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
                        <#if reportDate != "">
                            <tr>
                                <td class="cell-label">Report date:</td>
                                <td class="cell-value">${reportDate}</td>
                            </tr>
                        </#if>
                        <#if includeTestStatus>
                            <tr>
                                <td class="cell-label">Status:</td>
                                <td class="cell-value"><div class="value-inline result background-${data.overallStatus}">${printResult(data.overallStatus)}</div></td>
                            </tr>
                        </#if>
                    </table>
                </div>
                <div class="column right">
                    <#if includeTestStatus>
                        <table>
                            <tr>
                                <td class="cell-label">Test results:</td>
                                <td class="cell-value">${data.testStatus}</td>
                            </tr>
                            <tr>
                                <td class="cell-label">Result ratio:</td>
                                <td class="cell-value">${coverageBlock(data.completedTests, data.failedTests, data.undefinedTests, 350)}</td>
                            </tr>
                        </table>
                    </#if>
                </div>
            </div>
        </div>
    </div>
</#macro>
<#macro statementTestCases data includeTestCaseReports=false>
    <div id="test-cases" class="section test-suites">
        <div class="section-title">
            <div>Test cases</div>
        </div>
        <div class="section-content">
            ${data.prepareTestCaseGroups()}
            <#assign overallIndex = 0>
            <#list data.testSuites as testSuite>
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
                            <img src="classpath:reports/images/icon-${testSuite.overallStatus}.svg"/>
                        </div>
                    </div>
                    <div class="test-suite-content">
                        <#list testSuite.testCases as testCase>
                            <#assign index = testCase?counter>
                            <#assign overallIndex = overallIndex + 1>
                            <#if testCase.inGroup && testCase.firstInGroup>
                                <div class="test-case-group-overall-container"><#t>
                            </#if>
                            <div class="test-case-container<#if testCase.disabled> disabled</#if><#if testCase.inGroup> in-group</#if>"><#t>
                                <#if testCase.inGroup>
                                    <#assign group = data.getTestCaseGroup(testSuite.testSuiteId, testCase.getGroup())>
                                    <div class="group-marker background-<#if group.result??>${group.result}<#else>NONE</#if><#if testCase.firstInGroup> group-first</#if><#if testCase.lastInGroup> group-last</#if>"></div><#t>
                                </#if>
                                <div class="test-case-content<#if testCase.inGroup> in-group</#if><#if testCase.firstInGroup> group-first</#if><#if testCase.lastInGroup> group-last</#if>">
                                    <div class="test-case-first-line"><#t>
                                        <#if data.hasOptionalTests() || data.hasDisabledTests()>
                                            <div class="test-case-prescription-level icon"><#t>
                                                <img src="classpath:reports/images/icon-<#if testCase.disabled>disabled<#elseif testCase.optional>optional<#else>required</#if>.svg"/><#t>
                                            </div><#t>
                                        </#if>
                                        <#if testCase.inGroup>
                                            <#assign group = data.getTestCaseGroup(testSuite.testSuiteId, testCase.getGroup())>
                                            <#if group?? && group.name??>
                                                <div class="test-case-group-container"><#t>
                                                    <div class="test-case-group"><#t>
                                                        ${escape(group.name)}<#t>
                                                    </div><#t>
                                                </div><#t>
                                            </#if>
                                        </#if><#t>
                                        <div class="test-case-name<#if !includeTestCaseReports> without-link</#if>"><#t>
                                            <#if includeTestCaseReports><a class="page-link" href="#test-${tsIndex}-${index}"></#if><#t>
                                                ${escape(testCase.testName)}<#t>
                                            <#if includeTestCaseReports></a></#if>
                                        </div><#t>
                                        <#if testCase.tags??>
                                            <div class="test-case-tags"><#t>
                                                <#list testCase.tags as tag>
                                                    <div class="test-case-tag" style="background-color: ${tag.background()}; color: ${tag.foreground()}">${escape(tag.name())}</div><#t>
                                                </#list>
                                            </div><#t>
                                        </#if><#t>
                                        <div class="test-case-status icon"><#t>
                                            <img src="classpath:reports/images/icon-${testCase.reportResult}.svg"/><#t>
                                        </div><#t>
                                    </div><#t>
                                    <#if testCase.testDescription?? && testCase.testDescription != "">
                                        <div class="test-case-description"><#t>
                                            <div>${escape(testCase.testDescription)}</div><#t>
                                        </div><#t>
                                    </#if>
                                    <#if testCase.specReference?? || testCase.specDescription?? || testCase.specLink??>
                                        <div class="test-suite-test-case-spec-info"><#t>
                                            <div class="spec-reference-container"><#t>
                                                <@common.specificationInfo testCase.specReference testCase.specDescription testCase.specLink/><#t>
                                            </div><#t>
                                        </div><#t>
                                    </#if>
                                </div><#t>
                            </div><#t>
                            <#if testCase.inGroup && testCase.lastInGroup>
                                </div><#t>
                            </#if>
                        </#list>
                    </div>
                    <#if testSuite.specReference?? || testSuite.specDescription?? || testSuite.specLink??>
                        <div class="test-suite-spec-info">
                            <@common.specificationInfo testSuite.specReference testSuite.specDescription testSuite.specLink/>
                        </div>
                    </#if>
                </div>
            </#list>
            <#if data.hasOptionalTests() || data.hasDisabledTests() || data.hasTags()>
                <div class="test-case-legend">
                    <div class="legend-highlight"><img src="classpath:reports/images/icon-legend.svg"/></div>
                    <div class="legend-content">
                        <#if data.hasOptionalTests() || data.hasDisabledTests()>
                            <div class="legend-prescription-div">
                                <table class="legend-prescription-table">
                                    <tr>
                                        <#if data.hasRequiredTests()>
                                            <td class="legend-prescription-icon icon"><img class="icon-img" src="classpath:reports/images/icon-required.svg"/></td>
                                            <td class="legend-prescription-text">Required test case.</td>
                                        </#if>
                                        <#if data.hasOptionalTests()>
                                            <td class="legend-prescription-icon icon"><img class="icon-img" src="classpath:reports/images/icon-optional.svg"/></td>
                                            <td class="legend-prescription-text">Optional test case.</td>
                                        </#if>
                                        <#if data.hasDisabledTests()>
                                            <td class="legend-prescription-icon icon"><img class="icon-img" src="classpath:reports/images/icon-disabled.svg"/></td>
                                            <td class="legend-prescription-text">Disabled test case.</td>
                                        </#if>
                                    </tr>
                                </table>
                            </div>
                        </#if>
                        <#if data.hasTags()>
                            <div class="legend-tags-div <#if data.hasOptionalTests() || data.hasDisabledTests()>with-padding</#if>">
                                <table class="legend-tag-table">
                                    <#list data.getDistinctTags() as tag>
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
</#macro>