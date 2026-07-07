<#import "fragments/commonStyles.ftl" as commonStyles>
<html>
	<head>
	    <style>
	        <@commonStyles.basic />
	    </style>
    </head>
    <body>
        <div class="title">${title}</div>
        <div class="documentation-content">
            ${documentation}
        </div>
    </body>
</html>