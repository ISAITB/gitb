<#import "commonBlocks.ftl" as common>
<#macro styles>
    .report-item {
        margin-top: 10px;
        margin-bottom: 10px;
        border-radius: 4px;
        page-break-inside: avoid;
    }
    .step-report .section.overview td.cell-label {
        min-width: inherit;
        padding-left: 20px;
    }
    .step-report .details .metadata .row .label {
        margin-right: 10px;
    }
    .report-item-container {
        margin-left:5px;
        border-bottom-right-radius: 4px;
        border-top-right-radius: 4px;
        border-bottom-left-radius: 0px;
        border-top-left-radius: 0px;
        padding: 8px;
    }
    .report-item-container .metadata {
        margin-top: 10px;
    }
    .report-item-container .metadata .row {
        margin-top: 5px;
    }
    .step-report .description {
        display: inline;
    }
    .context-item {
        border-radius: 5px;
        padding: 10px;
        margin-top: 10px;
    }

    .context-item-key {
        font-weight: bold;
    }

    .context-item-value {
        margin-top: 5px;
        padding: 5px;
        border: 1px solid #000000;
        font-family: "FreeMono";
        font-size: 12px;
    }
</#macro>
<#macro printStep report id subTitle="" subTitleLink="">
    <div class="step-report">
        <div id="${id}" class="section overview page-break-avoid <#if subTitle?? && subTitle != "">no-margin</#if>">
            <#if subTitle?? && subTitle != "">
                <@common.subTitle subTitle subTitleLink />
            </#if>
            <div class="section-title">
                <div>Overview</div>
            </div>
            <div class="section-content">
                <table>
                    <tr>
                        <td class="cell-label">Date:</td>
                        <td class="cell-value">${report.reportDate}</td>
                    </tr>
                    <tr>
                        <td class="cell-label">Result:</td>
                        <td class="cell-value"><div class="value-inline result background-${report.reportResult}">${printResult(report.reportResult)}</div></td>
                    </tr>
                    <#if (report.errorCount?? && report.errorCount > 0) || (report.warningCount?? && report.warningCount > 0) || (report.messageCount?? && report.messageCount > 0)>
                        <tr>
                            <td class="cell-label">Findings:</td>
                            <td class="cell-value"><#if report.errorCount??>${report.errorCount}<#else>0</#if> error(s), <#if report.warningCount??>${report.warningCount}<#else>0</#if> warning(s), <#if report.messageCount??>${report.messageCount}<#else>0</#if> message(s)</td>
                        </tr>
                    </#if>
                </table>
            </div>
        </div>
        <#if report.reportItems??>
            <div class="section details">
                <div class="section-title">
                    <div>Report details</div>
                </div>
                <div class="section-content">
                    <#list report.reportItems as item>
                        <div class="report-item background-strong-${item.level}">
                            <div class="report-item-container background-${item.level} border-${item.level}">
                                <div class="row">
                                    <div class="icon"><img src="classpath:reports/images/${item.level}.png"/></div>
                                    <div class="description">${escape(item.description)}</div>
                                </div>
                                <#if item.location?? || item.test??>
                                    <div class="metadata">
                                        <#if item.location??>
                                            <div class="row">
                                                <div class="label">Location:</div>
                                                <div class="value-inline">${escape(item.location)}</div>
                                            </div>
                                        </#if>
                                        <#if item.test??>
                                            <div class="row">
                                                <div class="label">Test:</div>
                                                <div class="value-inline">${escape(item.test)}</div>
                                            </div>
                                        </#if>
                                    </div>
                                </#if>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>
        </#if>
        <#if report.contextItems??>
            <div class="section context">
                <div class="section-title">
                    <div>Report data</div>
                </div>
                <div class="section-content">
                    <#list report.contextItems as contextItem>
                        <@printItem contextItem />
                    </#list>
                </div>
            </div>
        </#if>
    </div>
</#macro>
<#macro printItem item>
    <div class="context-item border-normal">
        <div class="context-item-key">${escape(item.key)}</div>
        <#if item.value??>
            <div class="context-item-value background-normal">${escape(item.value)}</div>
        <#else>
            <div class="context-items">
                <#list item.items as child>
                    <@printItem child />
                </#list>
            </div>
        </#if>
    </div>
</#macro>
