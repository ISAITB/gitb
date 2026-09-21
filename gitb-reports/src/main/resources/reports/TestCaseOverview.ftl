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
            .comment-container {
              border-radius: 5px;
              border: 1px solid #000000;
            }
            .comment-header {
              display: table;
              padding: 10px;
              border-top-left-radius: 5px;
              border-top-right-radius: 5px;
              border-bottom: 1px solid #000000;
              background-color: #ededed;
              width: 100%;
            }
            .comment-spacer {
              margin-top: 4px;
              height: 22px;
              margin-bottom: 4px;
              margin-left: 30px;
              border-left: 1px solid #000000;
            }
            .comment-header-icon {
              display: table-cell;
              width: 22px;
              white-space: nowrap;
            }
            .comment-header-icon > img {
              width: 10px;
            }
            .comment-header-author {
              display: table-cell;
              margin-left: 10px;
              vertical-align: middle;
              padding-top: 5px;
              padding-bottom: 5px;
            }
            .comment-header-date-container, .comment-header-forced-container {
              display: table-cell;
              margin-left: 10px;
              width: 1px;
            }
            .comment-header-date, .comment-header-forced {
              border-radius: 8px;
              border: 1px solid #000000;
              background: #FFFFFF;
              padding: 4px 8px;
              white-space: nowrap;
              margin-left: 10px;
            }
            .comment-content {
              padding-left: 10px;
              padding-right: 10px;
              padding-top: 4px;
              padding-bottom: 4px;
            }
            .comment-container.admin .comment-header {
              background-color: #d9edf7;
            }
            .comment-container.admin .comment-header-icon > img {
              width: 16px;
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

                <#if specReference?? || specDescription?? || specLink??>
                    <div class="separator"></div>
                    <div class="spec-info">
                        <div class="columns">
                            <div class="column single">
                                <table>
                                    <tr>
                                        <td class="cell-label">Reference:</td>
                                        <td class="cell-value"><@common.specificationInfo specReference specDescription specLink/>
                                    </tr>
                                </table>
                            </div>
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
                                    <td class="cell-value"><div class="value-inline result background-${reportResult}">${printResult(reportResult)}</div></td>
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
                                <#if sessionId??>
                                    <tr>
                                        <td class="cell-label">Session ID:</td>
                                        <td class="cell-value"<#if endTime??> colspan="3"</#if>>${sessionId}</td>
                                    </tr>
                                </#if>
                            </table>
                        </div>
                    </div>
                    <#if outputMessages??>
                        <div class="row output-message ${reportResult}"><#t>
                            <#list outputMessages as message><#t>
                                <div>${escape(message)}</div><#t>
                            </#list><#t>
                        </div><#t>
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
                            <td class="cell-value"><#t>
                                <div class="value-inline"><#t>
                                    <#if userComment?? || adminComment??><#t>
                                      <a class="page-link" href="#annex-comments">Test session comments</a><#t>
                                    </#if><#t>
                                    <#if documentation??><#t>
                                      <#if userComment?? || adminComment??><span class="inline-text-separator">|</span></#if><#t>
                                      <a class="page-link" href="#annex-documentation">Test case documentation</a><#t>
                                    </#if><#t>
                                    <#if logMessages??><#t>
                                      <#if userComment?? || adminComment?? || documentation??><span class="inline-text-separator">|</span></#if><#t>
                                      <a class="page-link" href="#annex-log">Test session log</a><#t>
                                    </#if><#t>
                                </div><#t>
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

        <#if userComment?? || adminComment??>
            <page-before/>
            <div id="annex-comments" class="comments">
                <div class="title">Test session comments</div>
                <div class="comments-content">
                  <#if userComment?? && adminComment??>
                    <#if adminComment.showFirst()>
                      <@common.printAdminComment adminComment />
                      <div class="comment-spacer"></div><#t>
                      <@common.printUserComment userComment organisation />
                    <#else>
                      <@common.printUserComment userComment organisation />
                      <div class="comment-spacer"></div><#t>
                      <@common.printAdminComment adminComment />
                    </#if>
                  <#elseif userComment??>
                    <@common.printUserComment userComment organisation />
                  <#elseif adminComment??>
                    <@common.printAdminComment adminComment />
                  </#if>
                </div><#t>
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