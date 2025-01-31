<#import "fragments/commonStyles.ftl" as commonStyles>
<#import "fragments/commonBlocks.ftl" as common>
<#assign statementCounter = 0>
<#macro printConformanceItem item alternateStyle>
    <div class="conformance-item border-normal <#if alternateStyle>background-normal<#else>background-white</#if>">
        <div class="conformance-item-first-line">
            <table>
                <tr>
                    <td class="conformance-item-name">
                        <#if item.isStatement() && data.includeTestCases>
                            <#assign statementCounter = statementCounter+1>
                            <a href="#statement-${statementCounter}">${escape(item.name)}</a>
                        <#else>
                            ${escape(item.name)}
                        </#if>
                    </td>
                    <#if item.isStatement()>
                        <td class="conformance-item-data-td"><#t>
                            <div class="conformance-item-data-container"><#t>
                                <div class="conformance-item-coverage">${coverageBlock(item.data.completedTests, item.data.failedTests, item.data.undefinedTests, 270)}</div><#t>
                                <div class="conformance-item-status"><#t>
                                    <div class=icon><#t>
                                        <img src="classpath:reports/images/icon-${item.data.overallStatus}.png"/><#t>
                                    </div><#t>
                                </div><#t>
                            </div><#t>
                        </td><#t>
                    <#else>
                        <td class="conformance-item-status-td">
                            <div class="conformance-item-status icon">
                                <img src="classpath:reports/images/icon-${item.overallStatus}.png"/>
                            </div>
                        </td>
                    </#if>
                </tr>
            </table>
        </div>
        <#if item.items?has_content>
            <div class="conformance-items">
                <#list item.items as child>
                    <@printConformanceItem item=child alternateStyle=!alternateStyle />
                </#list>
            </div>
        </#if>
    </div>
</#macro>
<html>
	<head>
	    <style>
	        <@commonStyles.basic data.includePageNumbers/>
	        <@commonStyles.testResult />
	        <@commonStyles.testCoverage />
	        <@commonStyles.conformanceStatement />
	        .conformance-item {
                border-radius: 5px;
                padding: 10px;
                margin-top: 10px;
	        }
	        .conformance-item-data-container {
	            display: table;
	            width: 100%;
	        }
	        .conformance-item-coverage {
                display: table-cell;
                vertical-align: middle;
	        }
	        .conformance-item-data-container > .conformance-item-status {
                display: table-cell;
                vertical-align: middle;
                width: 1px;
	        }
	        .conformance-item-name {
                font-weight: bold;
	            width: 99%;
	        }
	        .conformance-item-status-td {
	            min-width: 30px;
	        }
	        .conformance-item-data-td {
	            min-width: 300px;
	        }
	        .placeholder-badge-td {
	            padding: 5px;
	        }
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
            <div class="section details">
                <div class="section-title">
                    <div>Overview</div>
                </div>
                <div class="section-content">
                    <div class="columns">
                        <#if data.testDomain?? || data.testSpecificationGroup?? || data.testSpecification?? || data.testActor??>
                            <div class="column left">
                                <table>
                                    <#if data.testDomain??>
                                        <tr>
                                            <td class="cell-label">${escape(data.labelDomain)}:</td>
                                            <td class="cell-value">${escape(data.testDomain)}</td>
                                        </tr>
                                    </#if>
                                    <#if data.testSpecificationGroup??>
                                        <tr>
                                            <td class="cell-label">${escape(data.labelSpecificationGroup)}:</td>
                                            <td class="cell-value">${escape(data.testSpecificationGroup)}</td>
                                        </tr>
                                    </#if>
                                    <#if data.testSpecification??>
                                        <tr>
                                            <#if data.testSpecificationGroup??>
                                                <td class="cell-label">${escape(data.labelSpecificationInGroup)}:</td>
                                            <#else>
                                                <td class="cell-label">${escape(data.labelSpecification)}:</td>
                                            </#if>
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
                                        <td class="cell-label">${escape(data.labelOrganisation)}:</td>
                                        <td class="cell-value">${escape(data.organisation)}</td>
                                    </tr>
                                    <tr>
                                        <td class="cell-label">${escape(data.labelSystem)}:</td>
                                        <td class="cell-value">${escape(data.system)}</td>
                                    </tr>
                                </table>
                            </div>
                        <#else>
                            <div class="column left">
                                <table>
                                    <tr>
                                        <td class="cell-label">${escape(data.labelOrganisation)}:</td>
                                        <td class="cell-value">${escape(data.organisation)}</td>
                                    </tr>
                                </table>
                            </div>
                            <div class="column right">
                                <table>
                                    <tr>
                                        <td class="cell-label">${escape(data.labelSystem)}:</td>
                                        <td class="cell-value">${escape(data.system)}</td>
                                    </tr>
                                </table>
                            </div>
                        </#if>
                    </div>
                    <div class="separator"></div>
                    <div class="columns">
                        <div class="column <#if data.includeTestStatus?? && data.includeTestStatus>left<#else>single</#if>">
                            <table>
                                <tr>
                                    <td class="cell-label">Report date:</td>
                                    <td class="cell-value">${data.reportDate}</td>
                                </tr>
                                <#if data.includeTestStatus?? && data.includeTestStatus>
                                    <tr>
                                        <td class="cell-label">Status:</td>
                                        <td class="cell-value"><div class="value-inline result background-${data.overallStatus}">${printResult(data.overallStatus)}</div></td>
                                    </tr>
                                </#if>
                            </table>
                        </div>
                        <#if data.includeTestStatus?? && data.includeTestStatus>
                            <div class="column right">
                                <table>
                                    <tr>
                                        <td class="cell-label">Statement results:</td>
                                        <td class="cell-value">${data.statementStatus}</td>
                                    </tr>
                                    <tr>
                                        <td class="cell-label">Statement ratio:</td>
                                        <td class="cell-value">${coverageBlock(data.completedStatements, data.failedStatements, data.undefinedStatements, 350)}</td>
                                    </tr>
                                </table>
                            </div>
                        </#if>
                    </div>
                </div>
            </div>
        </#if>
        <#if data.includeConformanceItems && data.conformanceItems??>
            <div id="conformance-items" class="section conformance-items">
                <div class="section-title">
                    <div>Conformance statements</div>
                </div>
                <div class="section-content">
                    <div class="conformance-items">
                        <#list data.conformanceItems as item>
                            <@printConformanceItem item=item alternateStyle=false/>
                        </#list>
                    </div>
                </div>
            </div>
        </#if>
        <#if data.includeConformanceItems && data.includeTestCases && data.conformanceItems??>
            <div class=statement-reports">
                <#list data.conformanceStatements as statement>
                    <#assign index = statement?counter>
                    <page-before/>
                    <div id="statement-${index}" class="statement-report">
                        <@common.subTitle text="Conformance Statement Report #"+index link="conformance-items"/>
                    </div>
                    <#if statement??>
                        <@common.statementOverview data=statement labelDomain=data.labelDomain labelSpecification=data.labelSpecification labelSpecificationGroup=data.labelSpecificationGroup labelSpecificationInGroup=data.labelSpecificationInGroup labelActor=data.labelActor labelOrganisation=data.labelOrganisation labelSystem=data.labelSystem includeTestStatus=true organisation=data.organisation system=data.system  reportDate=""/>
                        <#if statement.testSuites??>
                            <@common.statementTestCases data=statement includeTestCaseReports=false/>
                        </#if>
                    </#if>
                </#list>
            </div>
        </#if>
    </body>
</html>
