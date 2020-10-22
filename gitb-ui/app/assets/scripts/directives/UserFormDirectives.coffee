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
                    '<select id="template" class="form-control" ng-disabled="templateReadonly" ng-model="model.template" ng-options="template as template.name for template in model.selfRegOption.templates"><option value=""></option></select>'+
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

      scope.adaptTemplateStatus = () =>
        if scope.model?.selfRegOption?.forceTemplateSelection && scope.model?.selfRegOption?.templates?.length == 1
          scope.model.template = scope.model.selfRegOption.templates[0]
          scope.templateReadonly = true
        else 
          scope.templateReadonly = false

      scope.communitySelected = (option) =>
        scope.model.selfRegOption = option
        scope.communityChanged()

      scope.setFormFocus = () =>
        if scope.model?.selfRegOption?
          if scope.model.selfRegOption.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN
            scope.DataService.focus('token', 200)
          else 
            scope.DataService.focus('orgShortName', 200)
      
      scope.communityChanged = () =>
        if scope.model?.selfRegOption?
          scope.DataService.setupLabels(scope.model.selfRegOption.labels)
          scope.adaptTemplateStatus()
          scope.setFormFocus()

      scope.optionRowStyle = () =>
        if scope.selfRegOptions.length > 1
          ""
        else
          "selected"

      if scope.selfRegOptions == undefined
        CommunityService.getSelfRegistrationOptions()
        .then((data) =>
          scope.selfRegOptions = data
          if data.length == 1
            scope.communitySelected(data[0])
        )
      else
        if scope.selfRegOptions.length == 1
          scope.communitySelected(scope.selfRegOptions[0])

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

@directives.directive 'tbTestFilter', ['DataService', 'Constants', '$q', 'ErrorService'
  (DataService, Constants, $q, ErrorService) ->
    scope:
      filters: '='
      state: '='
      applyFn: '='
      loadDomainsFn: '='
      loadSpecificationsFn: '='
      loadActorsFn: '='
      loadTestSuitesFn: '='
      loadTestCasesFn: '='
      loadCommunitiesFn: '='
      loadOrganisationsFn: '='
      loadSystemsFn: '='
    restrict: 'A'
    template: '
      <div class="panel panel-default">
        <div class="panel-heading">
            <h4 class="title">Filters</h4>
            <div class="btn-toolbar pull-right">
                <button type="button" class="btn btn-default" ng-click="applyFilters()" ng-disabled="state.updatePending"><span class="tab" ng-if="state.updatePending"><i class="fa fa-spinner fa-spin fa-lg fa-fw"></i></span>Refresh</button>
                <toggle id="sessions-toggle-filter" class="btn-group" ng-model="showFiltering" on="Enabled" off="Disabled" ng-change="toggleFiltering()"></toggle>
            </div>
        </div>
        <div class="table-responsive" uib-collapse="!showFiltering">
          <div>
            <table class="table filter-table" ng-if="filterDefined(Constants.FILTER_TYPE.DOMAIN) || filterDefined(Constants.FILTER_TYPE.SPECIFICATION) || filterDefined(Constants.FILTER_TYPE.ACTOR) || filterDefined(Constants.FILTER_TYPE.TEST_SUITE) || filterDefined(Constants.FILTER_TYPE.TEST_CASE) || filterDefined(Constants.FILTER_TYPE.COMMUNITY) || filterDefined(Constants.FILTER_TYPE.ORGANISATION) || filterDefined(Constants.FILTER_TYPE.SYSTEM)">
              <thead>
                <tr>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.DOMAIN)">{{DataService.labelDomain()}}</th>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.SPECIFICATION)">{{DataService.labelSpecification()}}</th>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.ACTOR)">{{DataService.labelActor()}}</th>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.TEST_SUITE)">Test suite</th>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.TEST_CASE)">Test case</th>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.COMMUNITY)">Community</th>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.ORGANISATION)">{{DataService.labelOrganisation()}}</th>
                  <th ng-if="filterDefined(Constants.FILTER_TYPE.SYSTEM)">{{DataService.labelSystem()}}</th>
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
                    <th class="result-filter" ng-if="filterDefined(Constants.FILTER_TYPE.RESULT)">Result</th>
                    <th class="time-filter" ng-if="filterDefined(Constants.FILTER_TYPE.TIME)">Start time</th>
                    <th class="time-filter" ng-if="filterDefined(Constants.FILTER_TYPE.TIME)">End time</th>
                    <th class="session-filter" ng-if="filterDefined(Constants.FILTER_TYPE.SESSION)">Session</th>
                    <th class="filler"></th>
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
                  <td></td>
                </tr>
              </tbody>
            </table>
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
        if filterType == Constants.FILTER_TYPE.DOMAIN || filterType == Constants.FILTER_TYPE.SPECIFICATION || filterType == Constants.FILTER_TYPE.ACTOR
          # TODO ADAPT THE FOLLOWING CALL TO BE BASED ON ACTOR
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
        filters

      scope.applyFilters = () =>
        scope.applyFn(scope.currentFilters())

      scope.clearFilter = (filterType) =>
        if scope.filterDefined(filterType)
          scope.filtering[filterType].selection = []

      scope.clearFilters = () =>
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
        scope.resetFilters()
        scope.applyFilters()

      scope.toggleFiltering = () =>
        if !scope.showFiltering
          DataService.async(scope.clearFilters)

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
        # TODO ADAPT THIS TO BE BASED ON ACTOR OR SPECIFICATION
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

      scope.state.currentFilters = scope.currentFilters

      scope.definedFilters = {}
      for filterType in scope.filters
        scope.definedFilters[filterType] = true

      scope.translation =
        selectAll       : ""
        selectNone      : ""
        reset           : ""
        search          : "Search..."
        nothingSelected : "All"
      scope.showFiltering = false
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

      if scope.filterDefined(Constants.FILTER_TYPE.SESSION)
        scope.sessionState = {
          readonly: true
        }

      $q.all(scope.loadPromises)
      .then () =>
        scope.resetFilters()
        scope.applyFilters()

]