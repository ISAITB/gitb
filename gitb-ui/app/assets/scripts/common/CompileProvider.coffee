@common.config([
    '$compileProvider',
        ( $compileProvider ) ->
            $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|ftp|mailto|chrome-extension|data):/)
]);