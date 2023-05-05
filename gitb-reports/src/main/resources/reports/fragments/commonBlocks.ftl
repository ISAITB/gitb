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
        <div class="step-icon icon value-inline"><img src="classpath:reports/images/icon-${step.reportResult}.png"/></div>
    </div></#list></div>
</#macro>