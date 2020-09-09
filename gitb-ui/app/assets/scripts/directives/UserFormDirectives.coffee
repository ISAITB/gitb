@directives.directive 'tbUserForm', ['DataService'
  (DataService)->
    scope:
      tbUser: '='
      tbNew: '='
      tbAdmin: '='
      tbRoles: '='
      sso: '='
    template: ''+
      '<div class="form-group" ng-if="sso && !tbNew">'+
        '<label class="col-xs-3 control-label" for="name">Name:</label>'+
        '<div class="col-xs-6"><input id="name" ng-readOnly="true" ng-model="tbUser.name" class="form-control" type="text"></div>'+
        '<div tb-tooltip="The name of the user retrieved from her EU Login account."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!sso">'+
        '<label class="col-xs-4 control-label" for="name">* Name:</label>'+
        '<div class="col-xs-6"><input id="name" ng-model="tbUser.name" class="form-control" type="text"></div>'+
        '<div tb-tooltip="The name of the user. This is used for display purposes when listing users."></div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="email"><span ng-if="tbNew">* </span>Email:</label>'+
        '<div class="col-xs-6"><input id="email" ng-model="tbUser.email" class="form-control" type="text" ng-readOnly="!tbNew"></div>'+
        '<div ng-if="sso" tb-tooltip="The email address linked to the user\'s EU Login account."></div>'+
        '<div ng-if="!sso" tb-tooltip="An email-formatted username for this user that is used for logging in. No emails are ever sent to this."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="tbAdmin && !tbNew">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="role"><span ng-if="tbNew">* </span>Role:</label>'+
        '<div class="col-xs-6"><input id="role" ng-model="tbUser.role.label" class="form-control" type="text" ng-readOnly="!tbNew"></div>'+
        '<div tb-tooltip="The user\'s assigned role."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbAdmin">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="role">* Role:</label>'+
        '<div class="col-xs-6"><select id="role" class="form-control" ng-model="tbUser.role" ng-options="role.label for role in tbRoles track by role.id"></select></div>'+
        '<div tb-tooltip="The user\'s assigned role. A \'user\' can view information and execute test sessions whereas an \'administrator\' can also manage users and edit test configuration."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbNew">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="status">Status:</label>'+
        '<div class="col-xs-6"><input id="status" ng-model="tbUser.ssoStatusText" class="form-control" type="text" ng-readOnly="true"></div>'+
        '<div tb-tooltip="The activation status of the user\'s account."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbAdmin && !tbNew">'+
        '<label ng-class="{\'col-xs-3\': sso, \'col-xs-4\': !sso}" class="control-label" for="organization"><span ng-if="!sso">* </span>{{DataService.labelOrganisation()}}:</label>'+
        '<div class="col-xs-6"><input id="organization" ng-model="tbUser.organization.fname" class="form-control" type="text" ng-readOnly="true"></div>'+
        '<div tb-tooltip="The {{DataService.labelOrganisationLower()}} this user is a member of."></div>'+
      '</div>'+
      '<div class="form-group" ng-if="!tbNew && !sso">'+
        '<label class="col-xs-4 control-label" for="changePassword">Set one-time password?</label>'+
        '<div class="col-xs-6"><input id="changePassword" ng-model="tbUser.changePassword" class="form-check" type="checkbox" class="form-check" ng-change="setPasswordClicked()"></div>'+
      '</div>'+
      '<div ng-if="!sso" uib-collapse="!tbNew && !tbUser.changePassword">' +
        '<div class="form-group">'+
          '<label class="col-xs-4 control-label" for="password">* One-time password:</label>'+
          '<div class="col-xs-6"><input id="password" ng-model="tbUser.password" class="form-control" type="password"></div>'+
          '<div tb-tooltip="This password can only be used once at first login and will then need to be changed."></div>'+
        '</div>'+
        '<div class="form-group">'+
          '<label class="col-xs-4 control-label" for="cpassword">* Confirm one-time password:</label>'+
          '<div class="col-xs-6"><input id="cpassword" ng-model="tbUser.cpassword" class="form-control" type="password"></div>'+
          '<div tb-tooltip="Repeat the password to ensure it is correctly provided."></div>'+
        '</div>'+
      '</div>'
    restrict: 'A'
    link: (scope, element, attrs) ->
      scope.DataService = DataService
      scope.setPasswordClicked = () ->
        if scope.tbUser.changePassword
          scope.DataService.focus('password', 200)

]

@directives.directive 'tbSelfRegForm', ['Constants', 'CommunityService', 'DataService'
  (Constants, CommunityService, DataService)->
    scope:
      model: '='
      selfRegOptions: '='
    template: ''+
      '<div>'+
        '<div ng-if="selfRegOptions.length == 0">'+
            '<div ng-class="{\'link-account-separator\': sso}">'+
                '<div class="bg-info div-rounded div-padded">'+
                    '<span>There are currently no communities available that allow self-registration.</span>'+
                '</div>'+
            '</div>'+
        '</div>'+
        '<div ng-if="selfRegOptions.length > 0">'+
          '<div ng-class="{\'link-account-separator\': sso, \'bottom-margin\': !sso}">'+
              '<div class="bg-info div-rounded div-padded">'+
                  '<p>Select the community you want to register for and provide the requested information. If your community is not listed you need to request from an administrator to register you on your behalf.</p>'+
                  '<span ng-if="sso"><b>Privacy note:</b> By registering you grant your consent to link your name, email and EU Login user ID to your new administrator account.</span>'+
                  '<span ng-if="!sso"><b>Privacy note:</b> By registering you grant your consent to link the provided information to your new administrator account.</span>'+
              '</div>'+
          '</div>'+
          '<div ng-class="{\'form-separator-popup\': sso, \'form-separator form-separator-top-padding\': !sso}">'+
              '<h4 class="title">Community</h4>'+
          '</div>'+
          '<div table-directive row-style="optionRowStyle" ng-class="{\'self-reg-option-table-popup\': sso}" class="self-reg-option-table" columns="communityColumns" data="selfRegOptions" on-select="communitySelected"></div>'+
          '<div uib-collapse="!model.selfRegOption.communityId">' +
            '<div ng-if="model.selfRegOption.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN">'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="token">* Registration token:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="token" ng-model="model.selfRegToken" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div tb-tooltip="A community-specific token needs to be provided to allow registration. You need to request this from the community\'s administrator."></div>'+
              '</div>'+
              '<div class="form-group" ng-if="model.selfRegOption.selfRegTokenHelpText" style="margin-bottom: -20px;">'+
                  '<div class="col-xs-offset-3 col-xs-8">'+
                      '<div class="form-control-static inline-form-text"><div ng-bind-html="model.selfRegOption.selfRegTokenHelpText"></div></div>'+
                  '</div>'+
              '</div>'+
            '</div>'+
            '<div ng-class="{\'form-separator-popup\': sso, \'form-separator\': !sso}" class="form-separator-top-padding">'+
                '<h4 class="title">{{DataService.labelOrganisation()}} details <span uib-tooltip="This information defines the member of the selected community through which will you be testing for conformance"><i class="fa fa-question-circle"></i></span></h4>'+
            '</div>'+
            '<div class="form-group">'+
                '<label class="col-xs-3 control-label" for="orgShortName">* Short name:</label>'+
                '<div class="col-xs-7">'+
                    '<input id="orgShortName" ng-model="model.orgShortName" class="form-control" type="text"/>'+
                '</div>'+
                '<div tb-tooltip="A short name for your {{DataService.labelOrganisationLower()}} to appear in reports."></div>'+
            '</div>'+
            '<div class="form-group">'+
                '<label class="col-xs-3 control-label" for="orgFullName">* Full name:</label>'+
                '<div class="col-xs-7">'+
                    '<input id="orgFullName" ng-model="model.orgFullName" class="form-control" type="text"/>'+
                '</div>'+
                '<div tb-tooltip="The complete name of your {{DataService.labelOrganisationLower()}}."></div>'+
            '</div>'+
            '<div class="form-group" ng-if="model.selfRegOption.templates.length > 0">'+
                '<label class="col-xs-3 control-label" for="template"><span ng-show="model.selfRegOption.forceTemplateSelection">* </span>Configuration:</label>'+
                '<div class="col-xs-7">'+
                    '<select id="template" class="form-control" ng-model="model.template" ng-options="template as template.name for template in model.selfRegOption.templates"><option value=""></option></select>'+
                '</div>'+
                '<div tb-tooltip="Predefined test configuration(s) curated by the community administrator."></div>'+
            '</div>'+

            '<div class="row" ng-if="model.selfRegOption.organisationProperties.length > 0">'+
              '<div class="col-xs-12">'+
                '<div ng-class="{\'form-separator-popup selfreg\': sso, \'form-separator\': !sso}"></div>'+
              '</div>'+
            '</div>'+

            '<div tb-custom-properties-form tb-properties="model.selfRegOption.organisationProperties" tb-show-form-header="false" tb-form-padded="false" tb-col-input-less="1" tb-show-required-asterisks="model.selfRegOption.forceRequiredProperties"></div>'+
            '<div ng-if="!sso">'+
              '<div class="form-separator form-separator-top-padding">'+
                  '<h4 class="title">Administrator account details <span uib-tooltip="Through your administrator account you can manage your {{DataService.labelOrganisationLower()}}  users, configure your conformance statements and execute tests"><i class="fa fa-question-circle"></i></span></h4>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminName">* Name:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminName" ng-model="model.adminName" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div tb-tooltip="This may be your name or a description in case this account will be shared by multiple people."></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminEmail">* Email:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminEmail" ng-model="model.adminEmail" class="form-control" type="text"/>'+
                  '</div>'+
                  '<div tb-tooltip="This is used only during login as an email-formatted username (no emails will ever be sent to this address)."></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminPassword">* Password:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminPassword" ng-model="model.adminPassword" class="form-control" type="password"/>'+
                  '</div>'+
                  '<div tb-tooltip="The account\'s password, to be provided with the email when logging in."></div>'+
              '</div>'+
              '<div class="form-group">'+
                  '<label class="col-xs-3 control-label" for="adminPasswordConfirm">* Confirm password:</label>'+
                  '<div class="col-xs-7">'+
                      '<input id="adminPasswordConfirm" ng-model="model.adminPasswordConfirm" class="form-control" type="password"/>'+
                  '</div>'+
                  '<div tb-tooltip="Provide again the account\'s password to ensure it is correct."></div>'+
              '</div>'+
            '</div>'+
          '</div>'+
        '</div>'+
      '</div>'
    restrict: 'A'
    link: (scope, element, attrs) ->
      scope.Constants = Constants
      scope.DataService = DataService
      scope.sso = DataService.configuration['sso.enabled']
      scope.communityColumns = [
        {
          field: 'communityName',
          title: 'Name'
        }
        {
          field: 'communityDescription',
          title: 'Description'
        }
      ]
      scope.DataService.setupLabels()
      scope.communitySelected = (option) =>
        scope.model.selfRegOption = option
        scope.communityChanged()

      scope.setFormFocus = () =>
        if scope.model?.selfRegOption?
          if scope.model.selfRegOption.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN
            scope.DataService.focus('token', 200)
          else 
            scope.DataService.focus('orgShortName', 200)
      if scope.selfRegOptions == undefined
        CommunityService.getSelfRegistrationOptions()
        .then((data) =>
          scope.selfRegOptions = data
          if data.length == 1
            scope.model.selfRegOption = data[0]
            scope.DataService.setupLabels(scope.model.selfRegOption.labels)
            scope.setFormFocus()
        )
      else
        if scope.selfRegOptions.length == 1
          scope.model.selfRegOption = scope.selfRegOptions[0]
          scope.DataService.setupLabels(scope.model.selfRegOption.labels)
          scope.setFormFocus()
      
      scope.communityChanged = () =>
        if scope.model?.selfRegOption?
          scope.DataService.setupLabels(scope.model.selfRegOption.labels)
          scope.setFormFocus()

      scope.optionRowStyle = () =>
        if scope.selfRegOptions.length > 1
          ""
        else
          "selected"

]

@directives.directive 'tbTooltip', [
  () ->
    scope:
      tbTooltip: '@'
      tbInline: '<?'
      tbInlineType: '@?'
    restrict: 'A'
    template: '<div ng-class="{\'form-control-static\': (!tbInline || tbInlineType == \'button\'), \'checkLabel\': (tbInline && tbInlineType == \'checkLabel\'), \'checkLabelText\': (tbInline && tbInlineType == \'checkLabelText\'), \'check\': (tbInline && tbInlineType == \'check\'), \'radio\': (tbInline && tbInlineType == \'radio\')}"><span uib-tooltip="{{tbTooltip}}"><i class="fa fa-question-circle"></i></span></div>'
    link: (scope, element, attrs) ->
      if scope.tbInline == undefined
        scope.tbInline = false
      if scope.tbInlineType == undefined
        scope.tbInlineType = 'check'
      if scope.tbInline
        angular.element(element).addClass('inline-tooltip');
]

@directives.directive 'tbImportItems', ['Constants'
  (Constants) ->
    scope:
      tbImportItems: '='
      tbDefaultActionFn: '='
      tbTypeLabelFn: '='
      tbOpen: '@?'
    restrict: 'A'
    template: ''+
      '<div class="import-item-root">'+
        '<div class="import-item-root-item" ng-class="{\'padded\': !$last}" ng-repeat="group in groups" tb-import-item-group="group" tb-default-action-fn="tbDefaultActionFn" tb-type-label-fn="tbTypeLabelFn">'+
        '</div>'+
      '</div>'
    link: (scope, element, attrs) ->
      if scope.tbOpen == undefined
        scope.tbOpen == false
      scope.groups = []
      groupMap = {}
      for item in scope.tbImportItems
        if groupMap[item.type] == undefined
          groupMap[item.type] = {}
          groupMap[item.type].typeLabel = scope.tbTypeLabelFn(item.type)
          groupMap[item.type].open = scope.tbOpen
          groupMap[item.type].items = []
          scope.groups.push(groupMap[item.type])
        groupMap[item.type].items.push(item)
]

@directives.directive 'tbImportItem', ['Constants', 'DataService'
  (Constants, DataService) ->
    scope:
      tbImportItem: '='
      tbDefaultActionFn: '='
      tbTypeLabelFn: '='
    restrict: 'A'
    template: ''+
      '<div class="import-item" ng-class="{\'with-groups\': hasGroups, \'without-groups\': !hasGroups, \'add\': tbImportItem.match == '+Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY+', \'update\': tbImportItem.match == '+Constants.IMPORT_ITEM_MATCH.BOTH+', \'delete\': tbImportItem.match == '+Constants.IMPORT_ITEM_MATCH.DB_ONLY+'}">'+
        '<div class="import-item-icon"><span uib-tooltip="{{iconTooltip}}"><i class="fa" ng-class="{\'fa-check-square\': tbImportItem.match == '+ Constants.IMPORT_ITEM_MATCH.BOTH+', \'fa-plus-square\': tbImportItem.match == '+ Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY+', \'fa-minus-square\': tbImportItem.match == '+ Constants.IMPORT_ITEM_MATCH.DB_ONLY+'}"></i></span></div>'+
        '<div class="import-item-container">'+
          '<div ng-click="toggleItem(); $event.stopPropagation();" class="import-item-title title-holder" ng-class="{\'open\': tbImportItem.open, \'with-groups\': hasGroups, \'without-groups\': !hasGroups}">'+
            '<div class="title-text" ng-class="{\'skip\': isSkipped()}">{{itemName()}}</div>'+
            '<div class="title-action">'+
              '<button ng-if="showExpandAll()" type="button" ng-click="expandItem(tbImportItem); $event.stopPropagation();" class="btn btn-primary btn-sm">Expand all</button>'+
              '<select ng-if="!disableProcessChoice && tbImportItem.process != '+Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT+'" class="control" ng-click="$event.stopPropagation();" ng-model="tbImportItem.selectedProcessOption" ng-options="option.id as option.label for option in processOptions"></select>'+
              '<input type="text" value="Skip" ng-if="disableProcessChoice || tbImportItem.process == '+Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT+'" class="control" ng-click="$event.stopPropagation();" disabled="true"/>'+
            '</div>'+
            '<div class="title-icon" ng-if="hasGroups"><i class="fa" ng-class="{\'fa-chevron-down\': tbImportItem.open, \'fa-chevron-right\': !tbImportItem.open}"></i></div>'+
          '</div>'+
          '<div class="import-item-group-container" uib-collapse="!tbImportItem.open" ng-if="tbImportItem.groups.length > 0">'+
            '<div ng-repeat="group in tbImportItem.groups" tb-import-item-group="group" tb-default-action-fn="tbDefaultActionFn" tb-type-label-fn="tbTypeLabelFn">'+
            '</div>'+
          '</div>'+
        '</div>'+
      '</div>'
    link: (scope, element, attrs) ->
      scope.Constants = Constants
      scope.DataService = DataService

      scope.itemName = () =>
        if scope.tbImportItem.type == scope.Constants.IMPORT_ITEM_TYPE.CUSTOM_LABEL
          scope.Constants.LABEL_TYPE_LABEL[scope.tbImportItem.name]
        else
          scope.tbImportItem.name

      scope.toggleItem = () =>
        if scope.hasGroups
          if scope.tbImportItem.open
            scope.closeItem(scope.tbImportItem)
          else
            scope.tbImportItem.open = true

      scope.closeItem = (item) =>
        if item.groups?
          item.open = false
          for group in item.groups
            group.open = false
            for groupItem in group.items
              scope.closeItem(groupItem)

      scope.applyProcessOption = (item, newOption, force) =>
        if force || item.process != newOption
          optionForSelf = newOption
          processTypeForChildren == undefined

          if newOption == scope.Constants.IMPORT_ITEM_CHOICE.PROCEED && item.process == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
            optionForSelf = item.previousOption
            
          if optionForSelf == scope.Constants.IMPORT_ITEM_CHOICE.SKIP
            processTypeForChildren = scope.Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
          else if optionForSelf == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
            processTypeForChildren = scope.Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
          else if (optionForSelf == scope.Constants.IMPORT_ITEM_CHOICE.PROCEED || optionForSelf == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN) && !(item.process == scope.Constants.IMPORT_ITEM_CHOICE.PROCEED || item.process == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN)
            processTypeForChildren = scope.Constants.IMPORT_ITEM_CHOICE.PROCEED

          if item.process != scope.Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
            # Always keep reference to previous state before being disabled due to parent.
            item.previousOption = item.process
          item.process = optionForSelf
          item.selectedProcessOption = item.process
          if item.groups? && processTypeForChildren?
            for group in item.groups
              for groupItem in group.items
                groupItem.selectedProcessOption = processTypeForChildren

      scope.isSkipped = () =>
        scope.isSkipOption(scope.tbImportItem.process)

      scope.isSkipOption = (processOption) =>
        processOption == scope.Constants.IMPORT_ITEM_CHOICE.SKIP || processOption == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN || processOption == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT

      scope.showExpandAll = () =>
        scope.hasGroups && scope.hasClosedChild(scope.tbImportItem)

      scope.hasClosedChild = (item) =>
        if !item.open
          return true
        else if item.groups?
          for group in item.groups
            if !group.open
              return true
            else 
              for groupItem in group.items
                if scope.hasClosedChild(groupItem)
                  return true
        return false

      scope.expandItem = (item) =>
        item.open = true
        if item.groups?
          for group in item.groups
            group.open = true
            for groupItem in group.items
              scope.expandItem(groupItem)

      scope.hasGroups = scope.tbImportItem.children?.length > 0
      
      if scope.tbImportItem.match == scope.Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY
        scope.iconTooltip = "Data defined in the provided archive for which no match with existing data was found."
      else if scope.tbImportItem.match == scope.Constants.IMPORT_ITEM_MATCH.BOTH
        scope.iconTooltip = "Existing data that was matched by relevant data from the provided archive."
      else 
        scope.iconTooltip = "Existing data for which no match could be found in the provided archive."

      scope.processOptions = []
      if scope.tbImportItem.match == scope.Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY
        scope.processOptions.push({id: scope.Constants.IMPORT_ITEM_CHOICE.PROCEED, label: "Create"})
        scope.processOptions.push({id: scope.Constants.IMPORT_ITEM_CHOICE.SKIP, label: "Skip"})
      else if scope.tbImportItem.match == Constants.IMPORT_ITEM_MATCH.BOTH
        scope.processOptions.push({id: scope.Constants.IMPORT_ITEM_CHOICE.PROCEED, label: "Update"})
        if scope.hasGroups
          scope.processOptions.push({id: scope.Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN, label: "Skip but process children"})
        scope.processOptions.push({id: scope.Constants.IMPORT_ITEM_CHOICE.SKIP, label: "Skip"})
      else 
        scope.processOptions.push({id: scope.Constants.IMPORT_ITEM_CHOICE.PROCEED, label: "Delete"})
        scope.processOptions.push({id: scope.Constants.IMPORT_ITEM_CHOICE.SKIP, label: "Skip"})

      if scope.hasGroups
        scope.tbImportItem.open = false
        scope.tbImportItem.groups = []
        childGroupMap = {}
        for childItem in scope.tbImportItem.children
          if childGroupMap[childItem.type] == undefined
            childGroupMap[childItem.type] = {}
            childGroupMap[childItem.type].typeLabel = scope.tbTypeLabelFn(childItem.type)
            childGroupMap[childItem.type].open = false
            childGroupMap[childItem.type].items = []
            scope.tbImportItem.groups.push(childGroupMap[childItem.type])
          childGroupMap[childItem.type].items.push(childItem)
      else
        scope.tbImportItem.open = true

      if scope.tbImportItem.process == undefined
        defaultAction = scope.tbDefaultActionFn(scope.tbImportItem.match)
        # Don't allow a community admin to add or delete a domain.
        if scope.DataService.isCommunityAdmin && scope.tbImportItem.type == scope.Constants.IMPORT_ITEM_TYPE.DOMAIN && scope.tbImportItem.match != scope.Constants.IMPORT_ITEM_MATCH.BOTH
          defaultAction = scope.Constants.IMPORT_ITEM_CHOICE.SKIP
          scope.disableProcessChoice = true
        if !scope.hasGroups && defaultAction == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN
          defaultAction = scope.Constants.IMPORT_ITEM_CHOICE.SKIP
        if scope.tbImportItem.selectedProcessOption?
          scope.tbImportItem.process = scope.tbImportItem.selectedProcessOption
          if scope.tbImportItem.selectedProcessOption == scope.Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
            scope.tbImportItem.previousOption = defaultAction
        else
          scope.tbImportItem.process = defaultAction
      if scope.tbImportItem.previousOption == undefined
        scope.tbImportItem.previousOption = scope.tbImportItem.process
      if scope.tbImportItem.selectedProcessOption == undefined
        scope.tbImportItem.selectedProcessOption = scope.tbImportItem.process
      scope.applyProcessOption(scope.tbImportItem, scope.tbImportItem.selectedProcessOption, true)
      scope.$watch('tbImportItem.selectedProcessOption', () =>
        scope.applyProcessOption(scope.tbImportItem, scope.tbImportItem.selectedProcessOption, false)
      )
]

@directives.directive 'tbImportItemGroup', ['Constants'
  (Constants) ->
    scope:
      tbImportItemGroup: '='
      tbDefaultActionFn: '='
      tbTypeLabelFn: '='
    restrict: 'A'
    template: ''+
      '<div class="import-item-group">'+
        '<div ng-click="toggleGroup(group); $event.stopPropagation();" class="import-item-group-label title-holder" ng-class="{\'open\': group.open}">'+
          '<div class="title-text highlight" ng-class="{\'skip\': !isNotSkipped()}">{{group.typeLabel}} ({{group.items.length}})</div>'+
          '<div class="title-action">'+
            '<button type="button" ng-if="showExpandAll()" ng-click="expandAll(); $event.stopPropagation();" class="btn btn-primary btn-sm">Expand all</button>'+
            '<button type="button" ng-if="showSkipAll()" ng-click="skipAll(); $event.stopPropagation();" class="btn btn-primary btn-sm">Skip all</button>'+
            '<button type="button" ng-if="showProceedAll()" ng-click="proceedAll(); $event.stopPropagation();" class="btn btn-primary btn-sm">Process all</button>'+
          '</div>'+
          '<div class="title-icon"><i class="fa" ng-class="{\'fa-chevron-down\': group.open, \'fa-chevron-right\': !group.open}"></i></div>'+
        '</div>'+
        '<div class="import-item-group-items" uib-collapse="!group.open">'+
          '<div class="import-item-group-item" ng-repeat="groupItem in group.items" tb-import-item="groupItem" tb-default-action-fn="tbDefaultActionFn" tb-type-label-fn="tbTypeLabelFn"></div>'+
        '</div>'+
      '</div>'
    link: (scope, element, attrs) ->
      scope.group = scope.tbImportItemGroup

      scope.toggleGroup = (group) =>
        if group.open
          scope.closeGroup(group)
        else
          group.open = true

      scope.closeGroup = (group) =>
          group.open = false
          for item in group.items
            scope.closeItem(item)

      scope.closeItem = (item) =>
        if item.groups?
          item.open = false
          for group in item.groups
            scope.closeGroup(group)

      scope.showSkipAll = () =>
        if scope.group.items.length > 0
          for item in scope.group.items
            if item.process == Constants.IMPORT_ITEM_CHOICE.PROCEED || item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN
              return true
        false

      scope.showProceedAll = () =>
        if scope.group.items.length > 0
          for item in scope.group.items
            if item.process == Constants.IMPORT_ITEM_CHOICE.SKIP || item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN
              return true
        false

      scope.skipAll = () =>
        for item in scope.group.items
          item.selectedProcessOption = Constants.IMPORT_ITEM_CHOICE.SKIP

      scope.proceedAll = () =>
        for item in scope.group.items
          item.selectedProcessOption = Constants.IMPORT_ITEM_CHOICE.PROCEED

      scope.isNotSkipped = () =>
        for item in scope.group.items
          if item.process == Constants.IMPORT_ITEM_CHOICE.PROCEED || item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN
            return true
        false

      scope.expandAll = () =>
        scope.group.open = true
        for item in scope.group.items
          scope.expandItem(item)

      scope.expandItem = (item) =>
        item.open = true
        if item.groups?
          for group in item.groups
            group.open = true
            for groupItem in group.items
              scope.expandItem(groupItem)

      scope.showExpandAll = () =>
        if !scope.group.open
          return true
        else 
          for groupItem in scope.group.items
            if scope.hasClosedChild(groupItem)
              return true
        return false

      scope.hasClosedChild = (item) =>
        if !item.open
          return true
        else if item.groups?
          for group in item.groups
            if !group.open
              return true
            else 
              for groupItem in group.items
                if scope.hasClosedChild(groupItem)
                  return true
        return false

]

