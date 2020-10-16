tableDirectiveInputs = {
	columns: '=' # e.g.: {'sname': 'Short Name', 'fname': 'Full Name'}
	classes: '=' # e.g.: {'sname': 'short-name', 'fname': 'full-name'}
	data: '='
	onSelect: '='
	onDeselect: '='
	rowStyle: '='
	onAction: '='
	actionVisible: '='
	actionVisibleForRow: '='
	actionPendingProperty: '='
	actionIcon: '='
	operationsVisible: '='
	onDelete: '='
	deleteVisibleForRow: '='
	deletePendingProperty: '='
	onExport: '='
	exportVisible: '='
	exportVisibleForRow: '='
	exportPendingProperty: '='
	checkboxEnabled: '='
	onCheck: '='
	allowSelect: '='
	allowMultiSelect: '='
	tableCaption: '='
	paginationVisible: '='
	firstPage: '&'
	prevPage: '&'
	nextPage: '&'
	lastPage: '&'
	nextDisabled: '='
	prevDisabled: '='
	onSort: '='
}

@directives.directive 'tableDirective', [
	()->
		scope: tableDirectiveInputs
		restrict: 'AE'
		template: ''+
			'<div>'+
			'<table class="table table-directive">'+
				'<caption ng-if="tableCaptionVisible">{{tableCaption}}</caption>'+
				'<thead>'+
					'<tr>'+
						'<th ng-if="checkboxEnabled"></th>'+
						'<th ng-class="{sortable: column.sortable}" ng-repeat="column in columns" ng-click="!column.sortable || headerColumnClicked(column)">'+
							'{{column.title}} '+
							'<i ng-if="column.order == \'desc\'" class="fa fa-caret-down"></i>'+
							'<i ng-if="column.order == \'asc\'" class="fa fa-caret-up"></i>'+
						'</th>'+
						'<th ng-if="actionVisible" class="operations">Action</th>'+
						'<th ng-if="operationsVisible" class="operations">Operation</th>'+
						'<th ng-if="exportVisible" class="operations">Export</th>'+
					'</tr>'+
				'</thead>'+
				'<tbody>'+
					'<tr class="table-row-directive" ng-class="rowClass($index)" ng-repeat="row in data" ng-click="select($index)" table-row-directive data="row" columns="columns" classes="classes" action-visible="actionVisible" action-icon="actionIcon" operations-visible="operationsVisible" export-visible="exportVisible" export-visible-for-row="exportVisibleForRow" checkbox-enabled="checkboxEnabled" on-action="onAction" on-delete="onDelete" on-export="onExport" on-check="onCheck" delete-visible-for-row="deleteVisibleForRow" action-visible-for-row="actionVisibleForRow" action-pending-property="actionPendingProperty" delete-pending-property="deletePendingProperty" export-pending-property="exportPendingProperty"></tr>'+
				'</tbody>'+
			'</table>'+
				'<div ng-if="paginationVisible" class="text-center">'+
					'<ul class="pagination pagination-sm">'+
						'<li ng-class="prevDisabled ? \'disabled\' : \'\'"><a href ng-click="doFirstPage()">First</a></li>'+
						'<li ng-class="prevDisabled ? \'disabled\' : \'\'"><a href ng-click="doPrevPage()">Previous</a></li>'+
						'<li ng-class="nextDisabled ? \'disabled\' : \'\'"><a href ng-click="doNextPage()">Next</a></li>'+
						'<li ng-class="nextDisabled ? \'disabled\' : \'\'"><a href ng-click="doLastPage()">Last</a></li>'+
					'</ul>'+
				'</div>'+
			'</div>'

		replace: true
		link: (scope, element, attrs) ->
			scope.headerColumnClicked = (column) =>
				for col, i in scope.columns
					if col.field == column.field
						if !col.order?
							col.order = 'asc'
						else if col.order == 'asc'
							col.order = 'desc'
						else
							col.order = 'asc'
					else
						col.order = null
				scope.onSort? column
			scope.tableCaptionVisible = scope.tableCaption?

			scope.doFirstPage = () =>
				if scope.prevDisabled
					false
				else
					scope.firstPage()
			scope.doPrevPage = () =>
				if scope.prevDisabled
					false
				else
					scope.prevPage()
			scope.doNextPage = () =>
				if scope.nextDisabled
					false
				else
					scope.nextPage()
			scope.doLastPage = () =>
				if scope.nextDisabled
					false
				else
					scope.lastPage()
			scope.rowClass = (selectedIndex) =>
				rowClass = ''
				if scope.rowStyle
					rows = element.find 'tbody tr.table-row-directive'
					row = scope.data[selectedIndex]
					customClass = scope.rowStyle(row)
					if customClass?
						rowClass = rowClass + ' ' + customClass
				if scope.allowSelect || scope.allowMultiSelect || scope.onSelect
					rowClass = rowClass + ' selectable'
				
			scope.select = (selectedIndex)->
				rows = element.find 'tbody tr.table-row-directive'
				row = scope.data[selectedIndex]

				if !scope.allowSelect? || (scope.allowSelect? && scope.allowSelect)
					selectedRow = angular.element (rows[selectedIndex])

					if !scope.allowMultiSelect?
						oldSelectedRow = element.find 'tbody tr.table-row-directive.selected'
						oldSelectedRow.removeClass 'selected'

						selectedRow.addClass 'selected'
						scope.onSelect? row
					else
						selectedRowClasses = (selectedRow.attr 'class').split " "
						if 'selected' in selectedRowClasses
							selectedRow.removeClass 'selected'
							scope.onDeselect? row
						else
							selectedRow.addClass 'selected'
							scope.onSelect? row

				return
]

@directives.directive 'tableRowDirective', [
	() ->
		scope:
			data: '='
			columns: '='
			classes: '='
			operationsVisible: '='
			actionVisible: '='
			actionVisibleForRow: '='
			actionPendingProperty: '='
			exportVisible: '='
			exportVisibleForRow: '='
			exportPendingProperty: '='
			checkboxEnabled: '='
			onDelete: '='
			deleteVisibleForRow: '='
			deletePendingProperty: '='
			onAction: '='
			actionIcon: '='
			onExport: '='
			onCheck: '='
		restrict: 'A'
		template: ''+
			'<td ng-if="checkboxEnabled">'+
				'<input type="checkbox" ng-model="data.checked" ng-change="check()">' +
			'</td>' +
			'<td class="{{row.class}}" ng-repeat="row in rows">'+
				'<div ng-if="row.boolean">'+
					'<i class="glyphicon" ng-class="{\'glyphicon-ok\': row.data, \'glyphicon-remove\': !row.data}"></i>'+
				'</div>'+
				'<div ng-if="!row.boolean">'+
					'{{row.data}}'+
				'</div>'+
			'</td>'+
			'<td class="operations" ng-if="actionVisible">'+
				'<button type="button" ng-if="(!actionVisibleForRow || actionVisibleForRow(data)) && !data[actionPendingProperty]" class="btn btn-default" ng-click="action(); $event.stopPropagation();"><i ng-class="actionIcon"></i></button>'+
				'<button type="button" ng-if="(!actionVisibleForRow || actionVisibleForRow(data)) && data[actionPendingProperty]" class="btn btn-default pending" disabled="disabled"><i class="fa fa-spinner fa-spin fa-lg fa-fw"></i></button>'+
			'</td>' +
			'<td class="operations" ng-if="operationsVisible">'+
				'<button type="button" ng-if="(!deleteVisibleForRow || deleteVisibleForRow(data)) && !data[deletePendingProperty]" class="btn btn-default" ng-click="delete(); $event.stopPropagation();"><i class="fa fa-times"></i></button>'+
				'<button type="button" ng-if="(!deleteVisibleForRow || deleteVisibleForRow(data)) && data[deletePendingProperty]" class="btn btn-default pending" disabled="disabled"><i class="fa fa-spinner fa-spin fa-lg fa-fw"></i></button>'+
			'</td>' +
			'<td class="operations" ng-if="exportVisible">'+
				'<button type="button" ng-if="(!exportVisibleForRow || exportVisibleForRow(data)) && !data[exportPendingProperty]" class="btn btn-default" ng-click="export(); $event.stopPropagation();"><i class="fa fa-file-pdf-o"></i></button>'+
				'<button type="button" ng-if="(!exportVisibleForRow || exportVisibleForRow(data)) && data[exportPendingProperty]" class="btn btn-default pending" disabled="disabled"><i class="fa fa-spinner fa-spin fa-lg fa-fw"></i></button>'+
			'</td>'
		link: (scope, element, attrs) ->
			if !scope.actionPendingProperty?
				scope.actionPendingProperty = 'actionPending'
			if !scope.deletePendingProperty?
				scope.deletePendingProperty = 'deletePending'
			if !scope.exportPendingProperty?
				scope.exportPendingProperty = 'exportPending'
			scope.rows = _.map scope.columns, (column)->
				row = {}
				row.data = scope.data[column.field]
				row.boolean = _.isBoolean row.data
				row.class = if classes? then classes[column.field] else 'tb-'+column.title.toLowerCase().replace(" ", "-")

				row
			scope.delete = () =>
				if scope.onDelete?
					scope.onDelete(scope.data)
			scope.export = () =>
				scope.onExport? scope.data
			scope.check = () =>
				scope.onCheck? scope.data
			scope.action = () =>
				scope.onAction? scope.data
]

sessionTableDirectiveInputs = JSON.parse(JSON.stringify(tableDirectiveInputs))
sessionTableDirectiveInputs.sessionTableId = '@?'
sessionTableDirectiveInputs.sessionIdProperty = '@?'
sessionTableDirectiveInputs.testSuiteNameProperty = '@?'
sessionTableDirectiveInputs.testCaseNameProperty = '@?'

@directives.directive 'sessionTableDirective', ['$timeout'
	($timeout) ->
		scope: sessionTableDirectiveInputs
		restrict: 'AE'
		template: '
			<div>
				<table ng-attr-id="sessionTableId" class="table table-directive expandable-table">
					<caption ng-if="tableCaptionVisible">{{tableCaption}}</caption>
					<thead>
						<tr>
							<th ng-if="checkboxEnabled"></th>
							<th ng-class="{sortable: column.sortable}" ng-repeat="column in columns" ng-click="!column.sortable || headerColumnClicked(column)">
								{{column.title}} 
								<i ng-if="column.order == \'desc\'" class="fa fa-caret-down"></i>
								<i ng-if="column.order == \'asc\'" class="fa fa-caret-up"></i>
							</th>
							<th ng-if="actionVisible" class="operations">Action</th>
							<th ng-if="operationsVisible" class="operations">Operation</th>
							<th ng-if="exportVisible" class="operations">Export</th>
						</tr>
					</thead>
					<tbody>
						<tr table-row-directive class="table-row-directive expandable-table-row-collapsed" 
							ng-repeat-start="row in data" 
							ng-class="rowClass(row)" 
							ng-click="onExpand(row)" 
							data="row" 
							columns="columns" 
							classes="classes" 
							action-visible="actionVisible" 
							action-icon="actionIcon" 
							operations-visible="operationsVisible" 
							export-visible="exportVisible"
							export-visible-for-row="exportVisibleForRow"
							checkbox-enabled="checkboxEnabled" on-action="onAction" on-delete="onDelete" 
							on-export="onExport"
							on-check="onCheck"
							delete-visible-for-row="deleteVisibleForRow"
							action-visible-for-row="actionVisibleForRow"
							action-pending-property="actionPendingProperty"
							delete-pending-property="deletePendingProperty"
							export-pending-property="exportPendingProperty">
						</tr>
						<tr ng-repeat-end class="expandable-table-row-expanded">
							<td ng-attr-colspan="{{columnCount}}" class="expandable-table-expandable-cell-no-spacings" ng-class="{\'collapsed\': !row.expanded && !row.collapsing, \'collapsing\': row.collapsing}">
								<table width="100%" style="table-layout:fixed">
									<tr>
										<td>
											<div uib-collapse="!row.expanded" collapsing="rowCollapsing(row)" collapsed="rowCollapsed(row)">
												<div class="panel panel-default">
													<div class="panel-heading session-table-title">
														<div class="session-table-title-part">
															<div class="session-table-title-label">Test suite</div>
															<div class="session-table-title-value">{{row[testSuiteNameProperty]}}</div>
														</div>
														<div class="session-table-title-part">
															<div class="session-table-title-label">Test case</div>
															<div class="session-table-title-value">{{row[testCaseNameProperty]}}</div>
														</div>
														<div class="session-table-title-part">
															<div class="session-table-title-label">Session</div>
															<div class="session-table-title-value">{{row[sessionIdProperty]}}</div>
														</div>
													</div>
													<div class="panel-body" style="overflow-x: auto; overflow-y: hidden;">
														<div ng-show="!row.hideLoadingIcon"><span><i class="fa fa-spinner fa-spin fa-lg fa-fw"></i></span></div>
														<div uib-collapse="!row.diagramExpanded" class="no-margin">
															<div ng-if="row.expanded || row.diagramLoaded">
																<div test-session-presentation session-id="getSessionId(row)" session-object="row" on-ready="diagramReady"></div>
															</div>
														</div>
													</div>
												</div>
											</div>
										</td>
									</tr>
								</table>
							</td>
						</tr>
					</tbody>
				</table>
				<div ng-if="paginationVisible" class="text-center table-paging-controls-expandable">
					<ul class="pagination pagination-sm"  >
						<li ng-class="prevDisabled ? \'disabled\' : \'\'"><a href ng-click="doFirstPage()">First</a></li>
						<li ng-class="prevDisabled ? \'disabled\' : \'\'"><a href ng-click="doPrevPage()">Previous</a></li>
						<li ng-class="nextDisabled ? \'disabled\' : \'\'"><a href ng-click="doNextPage()">Next</a></li>
						<li ng-class="nextDisabled ? \'disabled\' : \'\'"><a href ng-click="doLastPage()">Last</a></li>
					</ul>
				</div>
			</div>
			'
		replace: true
		link: (scope, element, attrs) ->
			if !scope.sessionTableId?
				scope.sessionTableId = "session-table"
			if !scope.sessionIdProperty?
				scope.sessionIdProperty = "sessionId"
			if !scope.testSuiteNameProperty
				scope.testSuiteNameProperty = "testSuiteName"
			if !scope.testCaseNameProperty
				scope.testCaseNameProperty = "testCaseName"
			scope.columnCount = scope.columns.length
			if scope.checkboxEnabled
				scope.columnCount += 1
			if scope.actionVisible
				scope.columnCount += 1
			if scope.operationsVisible
				scope.columnCount += 1
			if scope.exportVisible
				scope.columnCount += 1
			scope.rowCollapsing = (data) ->
				data.collapsing = true
			scope.rowCollapsed = (data) ->
				data.collapsing = false
			scope.getSessionId = (data) =>
				data[scope.sessionIdProperty]
			scope.diagramReady = (sessionId, test) ->
				test.diagramLoaded = true
				$timeout(() -> 
					test.hideLoadingIcon = true
					test.diagramExpanded = true
				, 200)
			scope.onExpand = (data) =>
				data.collapsing = false
				data.expanded = !data.expanded? || !data.expanded
			scope.rowClass = (row) =>
				rowClass = ''
				if scope.rowStyle
					customClass = scope.rowStyle(row)
					if customClass?
						rowClass = rowClass + ' ' + customClass
				if scope.allowSelect || scope.allowMultiSelect || scope.onSelect
					rowClass = rowClass + ' selectable'
				rowClass
			scope.doFirstPage = () =>
				if scope.prevDisabled
					false
				else
					scope.firstPage()
			scope.doPrevPage = () =>
				if scope.prevDisabled
					false
				else
					scope.prevPage()
			scope.doNextPage = () =>
				if scope.nextDisabled
					false
				else
					scope.nextPage()
			scope.doLastPage = () =>
				if scope.nextDisabled
					false
				else
					scope.lastPage()
				
]
