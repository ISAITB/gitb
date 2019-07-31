class TabController

    @$inject = ['$log', 'DataService']
    constructor: (@$log, @DataService) ->
        @$log.debug "Constructing TabController..."

        @selectedTab = 1   # selected tab

    selectFontClass: (tab) ->
        clazz = ''
        if @selectedTab == tab
            clazz = 'bold'
        else
            clazz = 'normal'
        clazz

    selectActiveClass: (tab) ->
        clazz = ''
        if @selectedTab == tab
            clazz = 'active'
        clazz

    selectTab:(tab) ->
        @selectedTab = tab

controllers.controller('TabController', TabController)