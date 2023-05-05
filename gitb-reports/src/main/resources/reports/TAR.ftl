<#import "fragments/commonStyles.ftl" as commonStyles>
<#import "fragments/testStepReport.ftl" as testStepReport>
<html>
	<head>
	    <style>
	        <@commonStyles.basic />
	        <@testStepReport.styles />
	    </style>
    </head>
    <body>
        <div class="title">${title}</div>
        <@testStepReport.printStep .data_model "report" />
    </body>
</html>