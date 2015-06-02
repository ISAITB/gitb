@directives.directive 'tableDirective', [
	()->
		scope:
			columns: '=' # e.g.: {'sname': 'Short Name', 'fname': 'Full Name'}
			classes: '=' # e.g.: {'sname': 'short-name', 'fname': 'full-name'}
			data: '='
			onSelect: '='
			onDeselect: '='
			operationsVisible: '='
			onDelete: '='
			allowSelect: '='
			allowMultiSelect: '='
			tableCaption: '='
		restrict: 'AE'
		template: ''+
			'<table class="table table-directive">'+
				'<caption ng-if="tableCaptionVisible">{{tableCaption}}</caption>'+
				'<thead>'+
					'<tr>'+
						'<th ng-repeat="column in columns">{{column.title}}</th>'+
						'<th ng-if="operationsVisible">Operation</th>'+
					'</tr>'+
				'</thead>'+
				'<tbody>'+
					'<tr class="table-row-directive" ng-repeat="row in data" ng-click="select($index)" table-row-directive data="row" columns="columns" classes="classes" operations-visible="operationsVisible" on-delete="onDelete"></tr>'+
				'</tbody>'+
			'</table>'
		replace: true
		link: (scope, element, attrs) ->
			scope.tableCaptionVisible = scope.tableCaption?
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
			onDelete: '='
		restrict: 'A'
		template: ''+
			'<td class="{{row.class}}" ng-repeat="row in rows">'+
				'<div ng-if="row.boolean">'+
					'<i class="glyphicon" ng-class="{\'glyphicon-ok\': row.data, \'glyphicon-remove\': !row.data}"></i>'+
				'</div>'+
				'<div ng-if="!row.boolean">'+
					'{{row.data}}'+
				'</div>'+
			'</td>'+
			'<td class="operations" ng-if="operationsVisible">'+
				'<button class="btn btn-default" ng-click="delete()"><i class="fa fa-times"></i></button>'+
			'</td>'
		link: (scope, element, attrs) ->
			scope.rows = _.map scope.columns, (column)->
				row = {}
				row.data = scope.data[column.field]
				row.boolean = _.isBoolean row.data
				row.class = if classes? then classes[column.field] else column.title.toLowerCase().replace(" ", "-")

				row
			scope.delete = ()=>
				if scope.onDelete?
					scope.onDelete(scope.data)
				
]