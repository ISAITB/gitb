<#macro basic pageCounter=true>
    @page {
        size: a4 landscape;
        margin: 50px;
        <#if pageCounter>
            @bottom-right {
                font-family: "FreeSans";
                font-size: 10px;
                content: 'Page '+counter(page)+' of '+counter(pages);
            }
        </#if>
    }
    page-before {
      display: block;
      /* Create a page break before this element. */
      page-break-before: always;
    }
    body {
        margin: 0px;
        font-family: "FreeSans";
        font-size: 14px;
        line-height: 1.4;
    }
    table {
        border-spacing: 0px;
    }
    td, th {
        font-family: "FreeSans";
        font-size: 14px;
        line-height: 1.4;
    }
    .title {
        font-size: 30px;
        margin-bottom: 30px;
        background-color: #ededed;
        padding: 10px;
        border-radius: 5px;
        border: 1px solid #000000;
        page-break-inside: avoid;
    }
    .page-break-avoid {
        page-break-inside: avoid;
    }
    .sub-title {
        font-size: 18px;
        margin-bottom: 30px;
        background-color: #ededed;
        padding: 10px;
        border-radius: 5px;
        border: 1px solid #000000;
        position: relative;
        page-break-inside: avoid;
    }
    .sub-title-text {
        display: inline-block;
        margin-right: 30px;
        width: 90%;
    }
    .sub-title-link {
        margin-top: 2px;
        font-size: 14px;
        text-align: right;
        float: right;
    }
    .section-title {
        border-bottom: 1px solid #000000;
        padding-bottom: 5px;
        margin-bottom: 10px;
        font-size: 18px;
        padding-left: 15px;
        page-break-inside: avoid;
    }
    .section {
        margin-top: 30px;
    }
    .section.no-margin {
        margin-top: 0px;
    }
    .row {
        display: block;
        padding: 4px 0px 4px 0px;
    }
    .column {
        display: inline-block;
        vertical-align: top;
    }
    .column.single {
        width: 99%;
    }
    .column.left {
        width: 39%;
    }
    .column.right {
        width: 60%;
    }
    .value {
        display: inline-block;
        vertical-align: top;
    }
    .value-inline {
        display: inline;
        vertical-align: top;
    }
    .label {
        font-weight: bold;
        display: inline-block;
        vertical-align: top;
    }
    td.cell-label {
        font-weight: bold;
        padding-left: 10px;
        padding-right: 10px;
        text-align: right;
        min-width: 120px;
    }
    td.cell-label, td.cell-value {
        vertical-align: top;
        padding: 4px;
    }
    .result {
        font-size: 90%;
        padding: 0.3em 0.6em 0.3em 0.6em;
        font-weight: bold;
        color: #fff;
        text-align: center;
        white-space: nowrap;
        vertical-align: baseline;
        border-radius: 4px;
    }
    .icon {
        display: inline;
        padding-top: 2px;
        padding-bottom: 2px;
    }
    .icon img {
        width: 16px;
        margin-top: -4px;
    }
    .separator {
        margin-top: 10px;
        margin-left: 10px;
        margin-right: 10px;
        padding-top: 10px;
        border-top: 1px solid #c4c4c4;
    }
    .background-SUCCESS {
        background-color: #5cb85c;
    }
    .background-FAILURE {
        background-color: #c9302c;
    }
    .background-WARNING {
        background-color: #f0ad4e;
    }
    .background-UNDEFINED {
        background-color: #343a40;
    }
    .background-strong-error {
        background-color: #f1aeb5;
    }
    .background-strong-warning {
        background-color: #fbebb9;
    }
    .background-strong-info {
        background-color: #bddded;
    }
    .background-error {
        background-color: #f8d7da;
    }
    .background-warning {
        background-color: #fdf5dc;
    }
    .background-info {
        background-color: #d9edf7;
    }
    .background-normal {
        background: #ededed;
    }
    .border-normal {
        border: 1px solid #000000;
    }
    .border-error {
        border: 1px solid #f1aeb5;
    }
    .border-warning {
        border: 1px solid #fbebb9;
    }
    .border-info {
        border: 1px solid #bddded;
    }
    .inline-text-separator {
        margin-left: 10px;
        margin-right: 10px;
    }
</#macro>
<#macro testResult>
    .label-end-time {
        font-weight: bold;
        display: inline-block;
        margin-left: 30px;
        margin-right: 10px;
    }
    .output-message {
        margin-top: 10px;
        margin-left: 20px;
        margin-right: 20px;
        border-radius: 5px;
        padding: 10px;
        page-break-inside: avoid;
    }
    .output-message.FAILURE {
        background: #f8d7da;
        border: 1px solid #f1aeb5;
    }
    .output-message.SUCCESS {
        background: #def1de;
        border: 1px solid #bee3be;
    }
    .output-message.UNDEFINED {
        background: #ededed;
        border: 1px solid #000000;
    }
    td.cell-label-end-time {
        font-weight: bold;
        padding-left: 10px;
        padding-right: 10px;
    }
    .step-pills {
        margin-bottom: 60px;
    }
    .step-pill {
        display: inline-block;
        padding: 5px 10px 5px 5px;
        border: 1px solid #000000;
        border-radius: 4px;
        margin-top: 5px;
        margin-right: 5px;
        background: #ededed;
    }
    .step-pill .step-text {
        margin-right: 5px;
    }
</#macro>
<#macro testCoverage>
    .coverage-container {
        margin-top: -2px;
    }
    .coverage-result.start {
        border-top-left-radius: 4px;
        border-bottom-left-radius: 4px;
    }
    .coverage-result.end {
        border-top-right-radius: 4px;
        border-bottom-right-radius: 4px;
    }
    .coverage-result {
        display: inline-block;
        text-align: center;
        font-size: 90%;
        font-weight: bold;
        padding: 1px 0px;
    }
    .coverage-passed {
        color: #ffffff;
        background-color: #5cb85c;
    }
    .coverage-failed {
        color: #ffffff;
        background: #c9302c;
    }
    .coverage-undefined {
        color: #ffffff;
        background: #343a40;
    }
</#macro>