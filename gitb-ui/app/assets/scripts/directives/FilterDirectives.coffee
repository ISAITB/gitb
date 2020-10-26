@directives.directive 'tbTestFilter', ['DataService', 'Constants', '$q', 'ErrorService'
  (DataService, Constants, $q, ErrorService) ->
    scope:
      filters: '='
      state: '='
      applyFn: '='
      communityId: '='
      loadDomainsFn: '='
      loadSpecificationsFn: '='
      loadActorsFn: '='
      loadTestSuitesFn: '='
      loadTestCasesFn: '='
      loadCommunitiesFn: '='
      loadOrganisationsFn: '='
      loadSystemsFn: '='
      loadOrganisationPropertiesFn: '='
      loadSystemPropertiesFn: '='
    restrict: 'A'
    template: '
      <div class="panel panel-default">
        <div class="panel-heading" ng-click="clickedHeader()" ng-class="{\'clickable\': enableFiltering}">
            <h4 class="title">Filters</h4>
            <div class="btn-toolbar pull-right" ng-click="$event.stopPropagation();">
                <button type="button" class="btn btn-default" ng-click="applyFilters()" ng-disabled="state.updatePending"><span class="tab" ng-if="state.updatePending"><i class="fa fa-spinner fa-spin fa-lg fa-fw"></i></span>Refresh</button>
                <toggle id="sessions-toggle-filter" class="btn-group" ng-model="enableFiltering" on="Enabled" off="Disabled" ng-change="toggleFiltering()"></toggle>
            </div>
        </div>
        <div class="table-responsive" uib-collapse="!showFiltering">
          <div>
            <table class="table filter-table" ng-if="filterDefined(Constants.FILTER_TYPE.DOMAIN) || filterDefined(Constants.FILTER_TYPE.SPECIFICATION) || filterDefined(Constants.FILTER_TYPE.ACTOR) || filterDefined(Constants.FILTER_TYPE.TEST_SUITE) || filterDefined(Constants.FILTER_TYPE.TEST_CASE) || filterDefined(Constants.FILTER_TYPE.COMMUNITY) || filterDefined(Constants.FILTER_TYPE.ORGANISATION) || filterDefined(Constants.FILTER_TYPE.SYSTEM)">
              <thead>
                <tr>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.DOMAIN)">{{DataService.labelDomain()}}</th>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.SPECIFICATION)">{{DataService.labelSpecification()}}</th>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.ACTOR)">{{DataService.labelActor()}}</th>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.TEST_SUITE)">Test suite</th>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.TEST_CASE)">Test case</th>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.COMMUNITY)">Community</th>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.ORGANISATION)">{{DataService.labelOrganisation()}}</th>
                  <th ng-style="{{colStyle}}" ng-if="filterDefined(Constants.FILTER_TYPE.SYSTEM)">{{DataService.labelSystem()}}</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.DOMAIN)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.DOMAIN].filter"
                          output-model="filtering[Constants.FILTER_TYPE.DOMAIN].selection"
                          button-label="sname"
                          item-label="sname"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="sname"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.DOMAIN)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.SPECIFICATION)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.SPECIFICATION].filter"
                          output-model="filtering[Constants.FILTER_TYPE.SPECIFICATION].selection"
                          button-label="sname"
                          item-label="sname"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="sname"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.SPECIFICATION)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.ACTOR)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.ACTOR].filter"
                          output-model="filtering[Constants.FILTER_TYPE.ACTOR].selection"
                          button-label="actorId"
                          item-label="actorId"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="actorId"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.ACTOR)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.TEST_SUITE)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.TEST_SUITE].filter"
                          output-model="filtering[Constants.FILTER_TYPE.TEST_SUITE].selection"
                          button-label="sname"
                          item-label="sname"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="sname"
                          output-properties="id testCases"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.TEST_SUITE)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.TEST_CASE)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.TEST_CASE].filter"
                          output-model="filtering[Constants.FILTER_TYPE.TEST_CASE].selection"
                          button-label="sname"
                          item-label="sname"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="sname"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.TEST_CASE)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.COMMUNITY)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.COMMUNITY].filter"
                          output-model="filtering[Constants.FILTER_TYPE.COMMUNITY].selection"
                          button-label="sname"
                          item-label="sname"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="sname"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.COMMUNITY)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.ORGANISATION)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.ORGANISATION].filter"
                          output-model="filtering[Constants.FILTER_TYPE.ORGANISATION].selection"
                          button-label="sname"
                          item-label="sname"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="sname"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.ORGANISATION)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.SYSTEM)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.SYSTEM].filter"
                          output-model="filtering[Constants.FILTER_TYPE.SYSTEM].selection"
                          button-label="sname"
                          item-label="sname"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="sname"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.SYSTEM)">
                      </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div>
            <table class="table filter-table" ng-if="filterDefined(Constants.FILTER_TYPE.RESULT) || filterDefined(Constants.FILTER_TYPE.TIME) || filterDefined(Constants.FILTER_TYPE.SESSION)">
              <thead>
                <tr>
                    <th ng-style="{{colStyle}}" class="result-filter" ng-if="filterDefined(Constants.FILTER_TYPE.RESULT)">Result</th>
                    <th class="time-filter" ng-if="filterDefined(Constants.FILTER_TYPE.TIME)">Start time</th>
                    <th class="time-filter" ng-if="filterDefined(Constants.FILTER_TYPE.TIME)">End time</th>
                    <th class="session-filter" ng-if="filterDefined(Constants.FILTER_TYPE.SESSION)">Session</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.RESULT)">
                      <div isteven-multi-select
                          input-model="filtering[Constants.FILTER_TYPE.RESULT].filter"
                          output-model="filtering[Constants.FILTER_TYPE.RESULT].selection"
                          button-label="id"
                          item-label="id"
                          tick-property="ticked"
                          max-labels="0"
                          max-height="250px"
                          translation="translation"
                          helper-elements="filter"
                          search-property="id"
                          output-properties="id"
                          on-item-click="filterItemTicked(Constants.FILTER_TYPE.RESULT)">
                      </div>
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.TIME)">
                      <input date-range-picker class="form-control date-picker" type="text"
                            ng-model="startTime.date" options="startTimeOptions" clearable="true" readonly="readonly">
                  </td>
                  <td ng-if="filterDefined(Constants.FILTER_TYPE.TIME)">
                      <input date-range-picker class="form-control date-picker" type="text"
                            ng-model="endTime.date" options="endTimeOptions" clearable="true" readonly="readonly">
                  </td>
                  <td class="session-filter" ng-if="filterDefined(Constants.FILTER_TYPE.SESSION)">
                    <div class="input-group session-filter-input">
                      <input type="text" ng-trim="false" class="form-control" ng-click="sessionIdClicked()" ng-model="sessionState.id" ng-readonly="sessionState.readonly" ng-class="{\'clickable\': sessionState.readonly}"/>
                      <div class="input-group-btn">
                          <button class="btn btn-default" type="button" ng-click="applySessionId()" ng-disabled="sessionState.id == undefined"><i class="glyphicon" ng-class="{\'glyphicon-remove\': sessionState.readonly, \'glyphicon-ok\': !sessionState.readonly, \'faded\': sessionState.id == undefined}"></i></button>
                      </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div ng-if="filterDefined(Constants.FILTER_TYPE.ORGANISATION_PROPERTY)">
            <table class="table filter-table">
              <thead>
                <th>{{DataService.labelOrganisation()+" properties"}}</th>
              </thead>
              <tbody>
                <td style="padding-top: 0px;">
                  <div class="property-filters-container">
                    <div class="property-filter" ng-repeat="propFilter in organisationProperties">
                      <div tb-custom-property-filter
                        property-filter="propFilter"
                        properties="loadOrganisationProperties()"
                        apply-fn="applyOrganisationProperty" 
                        clear-fn="clearOrganisationProperty"
                        cancel-fn="cancelOrganisationProperty"></div>
                    </div>
                    <div class="property-filters-control" ng-class="{\'padded\': organisationProperties && organisationProperties.length > 0}">
                      <button ng-disabled="getApplicableCommunityId() == undefined" ng-show="!addingOrganisationProperty()" class="btn btn-default" ng-click="addOrganisationProperty()">Add</button>
                    </div>
                  </div>
                </td>
              </tbody>
            </table>
            <div>
          </div>
          <div ng-if="filterDefined(Constants.FILTER_TYPE.SYSTEM_PROPERTY)">
            <table class="table filter-table">
              <thead>
                <th>{{DataService.labelSystem()+" properties"}}</th>
              </thead>
              <tbody>
                <td style="padding-top: 0px;">
                  <div class="property-filters-container">
                    <div class="property-filter" ng-repeat="propFilter in systemProperties">
                      <div tb-custom-property-filter
                        property-filter="propFilter"
                        properties="loadSystemProperties()"
                        apply-fn="applySystemProperty" 
                        clear-fn="clearSystemProperty"
                        cancel-fn="cancelSystemProperty"></div>
                    </div>
                    <div class="property-filters-control" ng-class="{\'padded\': systemProperties && systemProperties.length > 0}">
                      <button ng-disabled="getApplicableCommunityId() == undefined" ng-show="!addingSystemProperty()" class="btn btn-default" ng-click="addSystemProperty()">Add</button>
                    </div>
                  </div>
                </td>
              </tbody>
            </table>
            <div>
          </div>
        </div>
      </div>
      '
    link: (scope, element, attrs) ->
      scope.DataService = DataService
      scope.Constants = Constants

      scope.filterDefined = (filterType) =>
        scope.definedFilters[filterType]?

      scope.setupFilter = (filterType, loadFn) =>
        scope.filtering[filterType] = {
          all : []
          filter : []
          selection : []
        }
        if scope.filterDefined(filterType)
          loadPromise = $q.defer()
          scope.loadPromises.push(loadPromise.promise)
          loadFn()
          .then (data) =>
            scope.filtering[filterType].all = data
            loadPromise.resolve()
          .catch (error) =>
            ErrorService.showErrorMessage(error)

      scope.filterValue = (filterType) =>
        if scope.filterDefined(filterType)
          values = _.map scope.filtering[filterType].selection, (s) -> s.id
        values

      scope.filterItemTicked = (filterType) =>
        if filterType == Constants.FILTER_TYPE.DOMAIN
          scope.setSpecificationFilter(scope.filtering[Constants.FILTER_TYPE.DOMAIN].selection, scope.filtering[Constants.FILTER_TYPE.DOMAIN].filter, true)
        if filterType == Constants.FILTER_TYPE.DOMAIN || filterType == Constants.FILTER_TYPE.SPECIFICATION
          scope.setActorFilter(scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].selection, scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, true)
          scope.setTestSuiteFilter(scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].selection, scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, true)
        if filterType == Constants.FILTER_TYPE.DOMAIN || filterType == Constants.FILTER_TYPE.SPECIFICATION || filterType == Constants.FILTER_TYPE.ACTOR || filterType == Constants.FILTER_TYPE.TEST_SUITE
          scope.setTestCaseFilter(scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].selection, scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter, true)
        if filterType == Constants.FILTER_TYPE.COMMUNITY
          scope.setOrganizationFilter(scope.filtering[Constants.FILTER_TYPE.COMMUNITY].selection, scope.filtering[Constants.FILTER_TYPE.COMMUNITY].filter, true)
        if filterType == Constants.FILTER_TYPE.COMMUNITY || filterType == Constants.FILTER_TYPE.ORGANISATION
          scope.setSystemFilter(scope.filtering[Constants.FILTER_TYPE.ORGANISATION].selection, scope.filtering[Constants.FILTER_TYPE.ORGANISATION].filter, true)
        scope.applyFilters()

      scope.currentFilters = () =>
        filters = {}
        filters[Constants.FILTER_TYPE.DOMAIN] = scope.filterValue(Constants.FILTER_TYPE.DOMAIN)
        filters[Constants.FILTER_TYPE.SPECIFICATION] = scope.filterValue(Constants.FILTER_TYPE.SPECIFICATION)
        filters[Constants.FILTER_TYPE.ACTOR] = scope.filterValue(Constants.FILTER_TYPE.ACTOR)
        filters[Constants.FILTER_TYPE.TEST_SUITE] = scope.filterValue(Constants.FILTER_TYPE.TEST_SUITE)
        filters[Constants.FILTER_TYPE.TEST_CASE] = scope.filterValue(Constants.FILTER_TYPE.TEST_CASE)
        filters[Constants.FILTER_TYPE.COMMUNITY] = scope.filterValue(Constants.FILTER_TYPE.COMMUNITY)
        filters[Constants.FILTER_TYPE.ORGANISATION] = scope.filterValue(Constants.FILTER_TYPE.ORGANISATION)
        filters[Constants.FILTER_TYPE.SYSTEM] = scope.filterValue(Constants.FILTER_TYPE.SYSTEM)
        filters[Constants.FILTER_TYPE.RESULT] = scope.filterValue(Constants.FILTER_TYPE.RESULT)
        if scope.filterDefined(Constants.FILTER_TYPE.TIME)
          filters.startTimeBegin = scope.startTime.date.startDate
          filters.startTimeBeginStr = scope.startTime.date.startDate?.format('DD-MM-YYYY HH:mm:ss')
          filters.startTimeEnd = scope.startTime.date.endDate
          filters.startTimeEndStr = scope.startTime.date.endDate?.format('DD-MM-YYYY HH:mm:ss')
          filters.endTimeBegin = scope.endTime.date.startDate
          filters.endTimeBeginStr = scope.endTime.date.startDate?.format('DD-MM-YYYY HH:mm:ss')
          filters.endTimeEnd = scope.endTime.date.endDate
          filters.endTimeEndStr = scope.endTime.date.endDate?.format('DD-MM-YYYY HH:mm:ss')
        if scope.filterDefined(Constants.FILTER_TYPE.SESSION)
          filters.sessionId = scope.sessionState.id
        if scope.filterDefined(Constants.FILTER_TYPE.ORGANISATION_PROPERTY)
          filters.organisationProperties = []
          for p in scope.organisationProperties
            if p.id? && p.value?
              filters.organisationProperties.push {
                id: p.id,
                value: p.value
              }
        if scope.filterDefined(Constants.FILTER_TYPE.SYSTEM_PROPERTY)
          filters.systemProperties = []
          for p in scope.systemProperties
            if p.id? && p.value?
              filters.systemProperties.push {
                id: p.id,
                value: p.value
              }
        filters

      scope.applyFilters = () =>
        scope.applyFn(scope.currentFilters())

      scope.clearFilter = (filterType) =>
        if scope.filterDefined(filterType)
          scope.filtering[filterType].selection = []

      scope.clearFilters = () =>
        scope.enableFiltering = false
        scope.showFiltering = false
        scope.clearFilter(Constants.FILTER_TYPE.DOMAIN)
        scope.clearFilter(Constants.FILTER_TYPE.ACTOR)
        scope.clearFilter(Constants.FILTER_TYPE.TEST_SUITE)
        scope.clearFilter(Constants.FILTER_TYPE.TEST_CASE)
        scope.clearFilter(Constants.FILTER_TYPE.COMMUNITY)
        scope.clearFilter(Constants.FILTER_TYPE.ORGANISATION)
        scope.clearFilter(Constants.FILTER_TYPE.SYSTEM)
        scope.clearFilter(Constants.FILTER_TYPE.RESULT)
        if scope.filterDefined(Constants.FILTER_TYPE.RESULT)
          scope.startTime.date = {startDate: null, endDate: null}
          scope.endTime.date = {startDate: null, endDate: null}
        scope.sessionId = undefined
        scope.organisationProperties = []
        scope.systemProperties = []
        scope.resetFilters()
        scope.applyFilters()

      scope.clickedHeader = () =>
        if scope.enableFiltering
          scope.showFiltering = !scope.showFiltering

      scope.toggleFiltering = () =>
        if !scope.enableFiltering
          DataService.async(scope.clearFilters)
        else
          scope.showFiltering = true

      scope.applyTimeFiltering = (ev, picker) =>
        scope.applyFilters()

      scope.clearStartTimeFiltering = (ev, picker) =>
        scope.clearTimeFiltering(scope.startTime.date)

      scope.clearEndTimeFiltering = (ev, picker) =>
        scope.clearTimeFiltering(scope.endTime.date)

      scope.clearTimeFiltering = (time) =>
        time.endDate = null
        time.startDate = null
        scope.applyFilters()

      scope.keepTickedProperty = (oldArr, newArr) ->
        if oldArr? and oldArr.length > 0
          for o, i in newArr
            n = _.find oldArr, (s) => `s.id == o.id`
            o.ticked = if n?.ticked? then n.ticked else false

      scope.setDomainFilter = () ->
        scope.filtering[Constants.FILTER_TYPE.DOMAIN].filter = _.map(scope.filtering[Constants.FILTER_TYPE.DOMAIN].all, _.clone)
        if scope.filtering[Constants.FILTER_TYPE.DOMAIN].selection.length > 0
          for f in scope.filtering[Constants.FILTER_TYPE.DOMAIN].filter
            found = _.find scope.filtering[Constants.FILTER_TYPE.DOMAIN].selection, (d) => `d.id == f.id`
            if found?
              f.ticked = true

      scope.setSpecificationFilter = (selection1, selection2, keepTick) ->
        if scope.filterDefined(Constants.FILTER_TYPE.DOMAIN)
          copy = _.map(scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, _.clone)
          selection = if selection1? and selection1.length > 0 then selection1 else selection2
          scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter = _.map((_.filter scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].all, (s) => (_.contains (_.map selection, (d) => d.id), s.domain)), _.clone)
          scope.keepTickedProperty(copy, scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter) if keepTick
          for i in [scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].selection.length - 1..0] by -1
            some = scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].selection[i]
            found = _.find scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, (s) => `s.id == some.id`
            if (!found?)
              scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].selection.splice(i, 1)
            else
              found.ticked = true
        else
          scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter = _.map(scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].all, _.clone)

      scope.setActorFilter = (selection1, selection2, keepTick) ->
        selection = if selection1? and selection1.length > 0 then selection1 else selection2
        copy = _.map(scope.filtering[Constants.FILTER_TYPE.ACTOR].filter, _.clone)
        scope.filtering[Constants.FILTER_TYPE.ACTOR].filter = _.map((_.filter scope.filtering[Constants.FILTER_TYPE.ACTOR].all, (a) => (_.contains (_.map selection, (s) => s.id), a.specification)), _.clone)
        scope.keepTickedProperty(copy, scope.filtering[Constants.FILTER_TYPE.ACTOR].filter) if keepTick

        for i in [scope.filtering[Constants.FILTER_TYPE.ACTOR].selection.length - 1..0] by -1
          some = scope.filtering[Constants.FILTER_TYPE.ACTOR].selection[i]
          found = _.find scope.filtering[Constants.FILTER_TYPE.ACTOR].filter, (s) => `s.id == some.id`
          if (!found?)
            scope.filtering[Constants.FILTER_TYPE.ACTOR].selection.splice(i, 1)

      scope.setTestSuiteFilter = (selection1, selection2, keepTick) ->
        selection = if selection1? and selection1.length > 0 then selection1 else selection2
        copy = _.map(scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter, _.clone)
        scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter = _.map((_.filter scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].all, (t) => (_.contains (_.map selection, (s) => s.id), t.specification)), _.clone)
        scope.keepTickedProperty(copy, scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter) if keepTick

        for i in [scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].selection.length - 1..0] by -1
          some = scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].selection[i]
          found = _.find scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter, (s) => `s.id == some.id`
          if (!found?)
            scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].selection.splice(i, 1)
          else
            found.ticked = true

      scope.setTestCaseFilter = (selection1, selection2, keepTick) ->
        selection = if selection1? and selection1.length > 0 then selection1 else selection2
        copy = _.map(scope.filtering[Constants.FILTER_TYPE.TEST_CASE].filter, _.clone)
        result = []
        for s, i in selection
          for t, i in s.testCases
            found = _.find scope.filtering[Constants.FILTER_TYPE.TEST_CASE].all, (c) => `c.id == t.id`
            result.push found
        scope.filtering[Constants.FILTER_TYPE.TEST_CASE].filter = _.map(result, _.clone)
        scope.keepTickedProperty(copy, scope.filtering[Constants.FILTER_TYPE.TEST_CASE].filter) if keepTick

        for i in [scope.filtering[Constants.FILTER_TYPE.TEST_CASE].selection.length - 1..0] by -1
          some = scope.filtering[Constants.FILTER_TYPE.TEST_CASE].selection[i]
          found = _.find scope.filtering[Constants.FILTER_TYPE.TEST_CASE].filter, (s) => `s.id == some.id`
          if (!found?)
            scope.filtering[Constants.FILTER_TYPE.TEST_CASE].selection.splice(i, 1)
          else
            found.ticked = true

      scope.setCommunityFilter = () ->
        scope.filtering[Constants.FILTER_TYPE.COMMUNITY].filter = _.map(scope.filtering[Constants.FILTER_TYPE.COMMUNITY].all, _.clone)

      scope.setOrganizationFilter = (selection1, selection2, keepTick) ->
        if scope.filterDefined(Constants.FILTER_TYPE.COMMUNITY)
          selection = if selection1? and selection1.length > 0 then selection1 else selection2
          copy = _.map(scope.filtering[Constants.FILTER_TYPE.ORGANISATION].filter, _.clone)
          scope.filtering[Constants.FILTER_TYPE.ORGANISATION].filter = _.map((_.filter scope.filtering[Constants.FILTER_TYPE.ORGANISATION].all, (o) => (_.contains (_.map selection, (s) => s.id), o.community)), _.clone)
          scope.keepTickedProperty(copy, scope.filtering[Constants.FILTER_TYPE.ORGANISATION].filter) if keepTick

          for i in [scope.filtering[Constants.FILTER_TYPE.ORGANISATION].selection.length - 1..0] by -1
            some = scope.filtering[Constants.FILTER_TYPE.ORGANISATION].selection[i]
            found = _.find scope.filtering[Constants.FILTER_TYPE.ORGANISATION].filter, (s) => `s.id == some.id`
            if (!found?)
              scope.filtering[Constants.FILTER_TYPE.ORGANISATION].selection.splice(i, 1)
        else
          scope.filtering[Constants.FILTER_TYPE.ORGANISATION].filter = _.map(scope.filtering[Constants.FILTER_TYPE.ORGANISATION].all, _.clone)

      scope.setSystemFilter = (selection1, selection2, keepTick) ->
        selection = if selection1? and selection1.length > 0 then selection1 else selection2
        copy = _.map(scope.filtering[Constants.FILTER_TYPE.SYSTEM].filter, _.clone)
        scope.filtering[Constants.FILTER_TYPE.SYSTEM].filter = _.map((_.filter scope.filtering[Constants.FILTER_TYPE.SYSTEM].all, (o) => (_.contains (_.map selection, (s) => s.id), o.owner)), _.clone)
        scope.keepTickedProperty(copy, scope.filtering[Constants.FILTER_TYPE.SYSTEM].filter) if keepTick

        for i in [scope.filtering[Constants.FILTER_TYPE.SYSTEM].selection.length - 1..0] by -1
          some = scope.filtering[Constants.FILTER_TYPE.SYSTEM].selection[i]
          found = _.find scope.filtering[Constants.FILTER_TYPE.SYSTEM].filter, (s) => `s.id == some.id`
          if (!found?)
            scope.filtering[Constants.FILTER_TYPE.SYSTEM].selection.splice(i, 1)

      scope.resetFilters = () =>
        scope.setDomainFilter()
        scope.setCommunityFilter()
        scope.setSpecificationFilter(scope.filtering[Constants.FILTER_TYPE.DOMAIN].filter, [], false)
        scope.setActorFilter(scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, [], false)
        scope.setTestSuiteFilter(scope.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, [], false)
        scope.setTestCaseFilter(scope.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter, [], false)
        scope.setOrganizationFilter(scope.filtering[Constants.FILTER_TYPE.COMMUNITY].filter, [], false)
        scope.setSystemFilter(scope.filtering[Constants.FILTER_TYPE.ORGANISATION].filter, [], false)
        for r in scope.filtering[Constants.FILTER_TYPE.RESULT].filter
          r.ticked = false
        if scope.filterDefined(Constants.FILTER_TYPE.SESSION)
          scope.sessionState.id = undefined
          scope.sessionState.readonly = true

      scope.getAllTestResults = () =>
        results = []
        for k, v of Constants.TEST_CASE_RESULT
          results.push({id: v})
        results

      scope.sessionIdClicked = () =>
        if scope.sessionState.readonly
          scope.sessionState.readonly = false
          if !scope.sessionState.id?
            scope.sessionState.id = ''

      scope.applySessionId = () =>
        if scope.sessionState.id?
          if scope.sessionState.readonly
            # Clear
            scope.sessionState.id = undefined
            scope.applyFilters()
          else
            # Apply
            trimmed = scope.sessionState.id.trim()
            scope.sessionState.id = trimmed
            if scope.sessionState.id.length == 0
              scope.sessionState.id = undefined
            scope.sessionState.readonly = true
            scope.applyFilters()

      scope.state.currentFilters = scope.currentFilters

      scope.definedFilters = {}
      for filterType in scope.filters
        scope.definedFilters[filterType] = true
      topColumnCount = 0
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.DOMAIN)
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.SPECIFICATION)
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.ACTOR)
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.TEST_SUITE)
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.TEST_CASE)
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.COMMUNITY)
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.ORGANISATION)
      topColumnCount += 1 if scope.filterDefined(Constants.FILTER_TYPE.SYSTEM)
      scope.colWidth = (100 / topColumnCount) + '%'
      scope.colStyle = {
        'width': scope.colWidth
      }

      scope.translation =
        selectAll       : ""
        selectNone      : ""
        reset           : ""
        search          : "Search..."
        nothingSelected : "All"
      scope.enableFiltering = false
      scope.filtering = {}
      scope.loadPromises = []
      scope.setupFilter(Constants.FILTER_TYPE.DOMAIN, scope.loadDomainsFn)
      scope.setupFilter(Constants.FILTER_TYPE.SPECIFICATION, scope.loadSpecificationsFn)
      scope.setupFilter(Constants.FILTER_TYPE.ACTOR, scope.loadActorsFn)
      scope.setupFilter(Constants.FILTER_TYPE.TEST_SUITE, scope.loadTestSuitesFn)
      scope.setupFilter(Constants.FILTER_TYPE.TEST_CASE, scope.loadTestCasesFn)
      scope.setupFilter(Constants.FILTER_TYPE.COMMUNITY, scope.loadCommunitiesFn)
      scope.setupFilter(Constants.FILTER_TYPE.ORGANISATION, scope.loadOrganisationsFn)
      scope.setupFilter(Constants.FILTER_TYPE.SYSTEM, scope.loadSystemsFn)
      scope.filtering[Constants.FILTER_TYPE.RESULT] = {
        all: scope.getAllTestResults()
        filter: scope.getAllTestResults()
        selection: []
      }
      if scope.filterDefined(Constants.FILTER_TYPE.TIME)
        scope.startTime = {
          date: {
            startDate: null
            endDate: null
          }
        }
        scope.startTimeOptions =
          locale:
            format: "DD-MM-YYYY"
          eventHandlers:
            'apply.daterangepicker': scope.applyTimeFiltering
            'cancel.daterangepicker': scope.clearStartTimeFiltering

        scope.endTime = {
          date: {
            startDate: null
            endDate: null
          }
        }
        scope.endTimeOptions =
          locale:
            format: "DD-MM-YYYY"
          eventHandlers:
            'apply.daterangepicker': scope.applyTimeFiltering
            'cancel.daterangepicker': scope.clearEndTimeFiltering

      if scope.filterDefined(Constants.FILTER_TYPE.SESSION)
        scope.sessionState = {
          readonly: true
        }

      scope.organisationProperties = []
      scope.systemProperties = []
      scope.uuidCounter = 0

      scope.addingOrganisationProperty = () =>
        if scope.organisationProperties.length > 0
          last = _.last(scope.organisationProperties)
          return last == undefined || last.id == undefined
        return false

      scope.addingSystemProperty = () =>
        if scope.systemProperties.length > 0
          last = _.last(scope.systemProperties)
          return last == undefined || last.id == undefined
        return false

      scope.addOrganisationProperty = () =>
        deferred = $q.defer()
        if !scope.cachedOrganisationProperties?
          scope.cachedOrganisationProperties = {}
        communityIdToUse = scope.getApplicableCommunityId()
        if communityIdToUse?
          cacheKey = 'org_'+communityIdToUse
          cachedData = scope.cachedOrganisationProperties[cacheKey]
          if cachedData?
            deferred.resolve(cachedData)
          else
            scope.loadOrganisationPropertiesFn(communityIdToUse)
            .then (data) =>
              for p in data
                p.allowedValues = JSON.parse(p.allowedValues)
              scope.cachedOrganisationProperties[cacheKey] = data
              deferred.resolve(data)
            .catch (error) =>
              ErrorService.showErrorMessage(error)
              deferred.resolve()
        else
          deferred.reject()
        deferred.promise.then (properties) =>
          if properties? && properties.length > 0
            scope.uuidCounter = scope.uuidCounter + 1
            scope.organisationProperties.push({
              uuid: scope.uuidCounter
            })

      scope.addSystemProperty = () =>
        deferred = $q.defer()
        if !scope.cachedSystemProperties?
          scope.cachedSystemProperties = {}
        communityIdToUse = scope.getApplicableCommunityId()
        if communityIdToUse?
          cacheKey = 'sys_'+communityIdToUse
          cachedData = scope.cachedSystemProperties[cacheKey]
          if cachedData?
            deferred.resolve(cachedData)
          else
            scope.loadSystemPropertiesFn(communityIdToUse)
            .then (data) =>
              for p in data
                p.allowedValues = JSON.parse(p.allowedValues)
              scope.cachedSystemProperties[cacheKey] = data
              deferred.resolve(data)
            .catch (error) =>
              ErrorService.showErrorMessage(error)
              deferred.resolve()
        else
          deferred.reject()
        deferred.promise.then (properties) =>
          if properties? && properties.length > 0
            scope.uuidCounter = scope.uuidCounter + 1
            scope.systemProperties.push({
              uuid: scope.uuidCounter
            })

      scope.applyOrganisationProperty = () =>
        scope.applyFilters()

      scope.applySystemProperty = () =>
        scope.applyFilters()

      scope.clearOrganisationProperty = (propertyDefinition) =>
        _.remove(scope.organisationProperties, (prop) => prop.uuid == propertyDefinition.uuid)
        scope.applyFilters()

      scope.clearSystemProperty = (propertyDefinition) =>
        _.remove(scope.systemProperties, (prop) => prop.uuid == propertyDefinition.uuid)
        scope.applyFilters()

      scope.cancelOrganisationProperty = () =>
        scope.organisationProperties.pop()

      scope.cancelSystemProperty = () =>
        scope.systemProperties.pop()

      scope.getApplicableCommunityId = () =>
        if scope.communityId?
          communityIdToUse = scope.communityId
        else if scope.filterDefined(Constants.FILTER_TYPE.COMMUNITY) && scope.filtering[Constants.FILTER_TYPE.COMMUNITY].selection.length == 1
          communityIdToUse = scope.filtering[Constants.FILTER_TYPE.COMMUNITY].selection[0].id
        else
          if scope.organisationProperties?.length > 0
            scope.organisationProperties = []
          if scope.systemProperties?.length > 0
            scope.systemProperties = []
        communityIdToUse

      scope.loadOrganisationProperties = () =>
        communityIdToUse = scope.getApplicableCommunityId()
        scope.cachedOrganisationProperties['org_'+communityIdToUse]

      scope.loadSystemProperties = () =>
        communityIdToUse = scope.getApplicableCommunityId()
        scope.cachedSystemProperties['sys_'+communityIdToUse]

      $q.all(scope.loadPromises)
      .then () =>
        scope.resetFilters()
        scope.applyFilters()

]

@directives.directive 'tbCustomPropertyFilter', [
  () ->
    scope:
      propertyFilter: '='
      properties: '='
      applyFn: '='
      clearFn: '='
      cancelFn: '='
    restrict: 'A'
    template: '
      <div class="custom-property-filter">
        <div ng-if="applied" class="custom-property-filter-container applied">
          <div class="property-id">{{appliedName}}</div>
          <div class="property-value">{{appliedValueLabel}}</div>
          <div class="property-controls">
            <button class="btn btn-default" ng-click="clear()"><i class="glyphicon glyphicon-remove"></i></button>
          </div>
        </div>
        <div ng-if="!applied" class="custom-property-filter-container non-applied">
          <div class="property-id">
            <select ng-change="propertyChanged()" ng-model="propertyInfo.property" class="form-control" ng-options="prop.name for prop in properties"></select>
          </div>
          <div class="property-value" ng-if="!propertyInfo.property.allowedValues">
            <input ng-model="propertyInfo.value" type="text" class="form-control"/>
          </div>
          <div class="property-value" ng-if="propertyInfo.property.allowedValues">
            <select ng-model="propertyInfo.valueObj" class="form-control">
              <option ng-repeat="v in propertyInfo.property.allowedValues" ng-value="v">{{v.label}}</option>
            </select>
          </div>
          <div class="property-controls">
            <button class="btn btn-default" ng-click="apply()"><i class="glyphicon glyphicon-ok"></i></button>
            <button class="btn btn-default" ng-click="cancel()"><i class="glyphicon glyphicon-remove"></i></button>
          </div>
        </div>
      </div>
      '
    link: (scope, element, attrs) ->
      scope.applied = false
      scope.propertyInfo = {}
      scope.propertyChanged = () =>
        scope.propertyInfo.valueObj = undefined
        scope.propertyInfo.value = undefined
      scope.apply = () =>
        if scope.propertyInfo.property == undefined || (scope.propertyInfo.valueObj == undefined && (scope.propertyInfo.value == undefined || scope.propertyInfo.value.trim() == ''))
          scope.cancel()
        else
          scope.appliedName = scope.propertyInfo.property.name
          if scope.propertyInfo.valueObj?
            scope.appliedValue = scope.propertyInfo.valueObj.value
            scope.appliedValueLabel = scope.propertyInfo.valueObj.label
          else
            scope.appliedValue = scope.propertyInfo.value.trim()
            scope.appliedValueLabel = scope.appliedValue
          scope.propertyFilter.id = scope.propertyInfo.property.id
          scope.propertyFilter.value = scope.appliedValue
          scope.applyFn(scope.propertyFilter)
          scope.applied = true
      scope.clear = () =>
        scope.clearFn(scope.propertyFilter)
      scope.cancel = () =>
        scope.cancelFn(scope.propertyFilter)

]