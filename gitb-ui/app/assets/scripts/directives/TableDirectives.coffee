@directives.directive 'tableDirective', [
	()->
		scope:
			columns: '=' # e.g.: {'sname': 'Short Name', 'fname': 'Full Name'}
			classes: '=' # e.g.: {'sname': 'short-name', 'fname': 'full-name'}
			data: '='
			onSelect: '='
			onDeselect: '='
			operationsVisible: '='
			exportVisible: '='
			checkboxEnabled: '='
			onDelete: '='
			onExport: '='
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
			actionVisible: '='
			onAction: '='
			onSort: '='
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
						'<th ng-if="actionVisible">Action</th>'+
						'<th ng-if="operationsVisible">Operation</th>'+
						'<th ng-if="exportVisible">Export</th>'+
					'</tr>'+
				'</thead>'+
				'<tbody>'+
					'<tr class="table-row-directive" ng-repeat="row in data" ng-click="select($index)" table-row-directive data="row" columns="columns" classes="classes" action-visible="actionVisible" operations-visible="operationsVisible" export-visible="exportVisible" checkbox-enabled="checkboxEnabled" on-action="onAction" on-delete="onDelete" on-export="onExport" on-check="onCheck"></tr>'+
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
			exportVisible: '='
			checkboxEnabled: '='
			onDelete: '='
			onAction: '='
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
				'<button class="btn btn-default" ng-click="action()"><i class="fa fa-search"></i></button>'+
			'</td>' +
			'<td class="operations" ng-if="operationsVisible">'+
				'<button class="btn btn-default" ng-click="delete()"><i class="fa fa-times"></i></button>'+
			'</td>' +
			'<td class="operations" ng-if="exportVisible">'+
          '<button class="btn btn-default" ng-click="export()"><i class="fa fa-file-pdf-o"></i></button>'+
      '</td>'
		link: (scope, element, attrs) ->
			scope.rows = _.map scope.columns, (column)->
				row = {}
				row.data = scope.data[column.field]
				row.boolean = _.isBoolean row.data
				row.class = if classes? then classes[column.field] else column.title.toLowerCase().replace(" ", "-")

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
