<#import "fragments/commonStyles.ftl" as commonStyles>
<#import "fragments/testStepReport.ftl" as testStepReport>
<#import "fragments/commonBlocks.ftl" as common>
<html>
	<head>
	    <style>
	        <@commonStyles.basic />
	        <@commonStyles.testResult />
	        <@testStepReport.styles />

            .party-metadata {
                margin-top:20px;
            }
            .column.left {
                width: 39%;
            }
            .column.right {
                width: 60%;
            }
            .log-content {
                font-family: "FreeMono";
                font-size: 12px;
                border: 1px solid #000000;
                padding: 5px;
            }
            .log-message.level-1 {
               color: #000000;
            }
            .log-message.level-2 {
               font-weight:bold;
               color: #f0ad4e;
            }
            .log-message.level-3 {
               font-weight:bold;
               color: #c9302c;
            }
            .step {
                margin-bottom: 30px;
            }
	    </style>
    </head>
    <body id="top">
        <div class="title">${title}</div>
        <div class="section">
            <div class="section-title">
                <div>Overview</div>
            </div>
            <div class="section-content">
                <div class="party-metadata">
                    <div class="columns">
                        <div class="column left">
                            <table>
                                <tr>
                                    <td class="cell-label">${escape(labelOrganisation)}:</td>
                                    <td class="cell-value">${escape(organisation)}</td>
                                </tr>
                            </table>
                        </div>
                        <div class="column right">
                            <table>
                                <tr>
                                    <td class="cell-label">${escape(labelSystem)}:</td>
                                    <td class="cell-value">${escape(system)}</td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </div>
                <div class="separator"></div>
                <div class="test-metadata">
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
                                    <td class="cell-label">Test name:</td>
                                    <td class="cell-value">${escape(testName)}</td>
                                </tr>
                                <tr>
                                    <td class="cell-label">Description:</td>
                                    <td class="cell-value">${escape(testDescription)}</td>
                                </tr>
                           </table>
                        </div>
                    </div>
                </div>
                <div class="separator"></div>
                <div class="session-result">
                    <div class="columns">
                        <div class="column left">
                            <table>
                                <tr>
                                    <td class="cell-label">Result:</td>
                                    <td class="cell-value"><div class="value-inline result background-${reportResult}">${reportResult}</div></td>
                                </tr>
                            </table>
                        </div>
                        <div class="column right">
                            <table>
                                <tr>
                                    <td class="cell-label">Start time:</td>
                                    <td class="cell-value">${startTime}</td>
                                    <#if endTime??>
                                        <td class="cell-label-end-time">End time:</td>
                                        <td class="cell-value">${endTime}</td>
                                    </#if>
                                </tr>
                            </table>
                        </div>
                    </div>
                    <#if outputMessage??>
                        <div class="row output-message ${reportResult}">
                            ${escape(outputMessage)}
                        </div>
                    </#if>
                </div>
            </div>
        </div>

        <#if documentation?? || logMessages??>
            <div class="section">
                <div class="section-title">
                    <div>References</div>
                </div>
                <div class="section-content">
                    <table>
                        <tr>
                            <td class="cell-label">Annexes:</td>
                            <td class="cell-value">
                                <div class="value-inline">
                                    <#if documentation??><a class="page-link" href="#annex-documentation">Test case documentation</a></#if>
                                    <#if documentation?? && logMessages??> | </#if>
                                    <#if logMessages??><a class="page-link" href="#annex-log">Test session log</a></#if>
                                </div>
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
        </#if>

        <#if steps??>
            <page-before/>
            <div id="step-reports" class="title">Step reports</div>
            <div class="section">
                <div class="section-content">
                    <@common.printStepsOverview steps true />
                </div>
            </div>
            <div class="steps">
                <#list steps as step>
                    <div class="step">
                        <@testStepReport.printStep step "step-"+(step?counter) step.title "step-reports" />
                    </div>
                </#list>
            </div>
        </#if>

        <#if documentation??>
            <page-before/>
            <div id="annex-documentation" class="documentation">
                <div class="title">Test case documentation</div>
                <div class="documentation-content">
                    ${documentation}
                </div>
            </div>
        </#if>

        <#if logMessages??>
            <page-before/>
            <div id="annex-log" class="log">
                <div class="title">Test session log</div>
                <div class="log-content background-normal">
                    <#list logMessages as message>
                        <div class="log-message level-${message.level}">${escape(message.text)}</div>
                    </#list>
                </div>
            </div>
        </#if>
    </body>
</html>