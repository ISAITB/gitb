@directives.directive 'tbCommunityForm', ['Constants', 'DataService'
  (@Constants, @DataService)->
    scope:
      tbCommunity: '='
      tbDomains: '='
      tbAdmin: '='
    template: ''+
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="sname">* Short name:</label>'+
        '<div class="col-xs-8"><input id="sname" ng-model="tbCommunity.sname" class="form-control" type="text" required></div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="fname">* Full name:</label>'+
        '<div class="col-xs-8"><input id="fname" ng-model="tbCommunity.fname" class="form-control" type="text" required></div>'+
      '</div>'+
      '<div class="form-group" ng-if="tbAdmin">'+
        '<label class="col-xs-3 control-label" for="domainChoice">{{DataService.labelDomain()}}:</label>'+
        '<div class="col-xs-8">'+
          '<select id="domainChoice" class="form-control" ng-model="tbCommunity.domain" ng-options="domain.sname for domain in tbDomains track by domain.id"><option value="">--Optional--</option></select>'+
        '</div>'+
      '</div>'+
      '<div class="form-group">'+
        '<label class="col-xs-3 control-label" for="email">Support email:</label>'+
        '<div class="col-xs-8"><input id="email" ng-model="tbCommunity.email" class="form-control" type="text"></div>'+
      '</div>'+
      '<div ng-if="selfRegEnabled">'+
        '<div class="form-group">'+
          '<label class="col-xs-3 control-label" for="selfRegType">* Self-registration method:</label>'+
          '<div class="col-xs-8">'+
            '<select id="selfRegType" class="form-control" ng-model="tbCommunity.selfRegType" ng-options="+(type.id) as type.label for type in selfRegTypes" ng-change="selfRegTypeChanged()"></select>'+
          '</div>'+
        '</div>'+
        '<div class="form-group" ng-if="tbCommunity.selfRegType == '+@Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN+' || tbCommunity.selfRegType == '+@Constants.SELF_REGISTRATION_TYPE.TOKEN+'">'+
          '<label class="col-xs-3 control-label" for="selfRegToken">* Self-registration token:</label>'+
          '<div class="col-xs-8"><input id="selfRegToken" ng-model="tbCommunity.selfRegToken" class="form-control" type="text" required></div>'+
        '</div>'+
        '<div class="form-group" ng-if="emailEnabled && tbCommunity.selfRegType != '+@Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED+'">'+
          '<label class="col-xs-3 control-label" for="notifications">Self-registration notifications:</label>'+
          '<div class="col-xs-8"><input id="notifications" ng-model="tbCommunity.selfRegNotification" type="checkbox" class="form-check"></div>'+
        '</div>'+
      '</div>'+
      '<input id="domain" ng-if="!tbAdmin" ng-model="tbCommunity.domain" type="hidden">'
    restrict: 'A'
    link: (scope, element, attrs) =>
      scope.DataService = @DataService
      scope.selfRegEnabled = @DataService.configuration['registration.enabled']
      scope.emailEnabled = @DataService.configuration['email.enabled']
      scope.selfRegTypes = [
        {id: @Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED, label: 'Not supported'}, 
        {id: @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING, label: 'Select from public communities'}, 
        {id: @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN, label: 'Select from public communities and provide token'} 
      ]
      scope.showToken = () =>
        scope.tbCommunity.selfRegType == @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN || scope.tbCommunity.selfRegType == @Constants.SELF_REGISTRATION_TYPE.TOKEN
      scope.selfRegTypeChanged = () =>
        if scope.showToken()
          scope.DataService.focus('selfRegToken')

]

@directives.directive 'tbOptionalCustomPropertiesForm', [
  () ->
    scope:
      tbPropertyData: '='
      tbPopup: '<?'
      tbColLabel: '<?'
      tbColOffset: '<?'
      tbReadonly: '<?'
    template: ''+
      '<div ng-if="tbPropertyData.properties.length > 0">'+
        '<div class="row">'+
          '<div ng-class="\'col-xs-\'+(11-tbColOffset)+\' col-xs-offset-\'+tbColOffset">'+
            '<form class="form-horizontal">'+
              '<div class="form-group">'+
                '<label ng-class="\'col-xs-\'+tbColLabel" class="control-label" for="editProperties">Show properties:</label>'+
                '<div ng-class="\'col-xs-\'+(11-tbColLabel)"><input id="editProperties" ng-model="tbPropertyData.edit" type="checkbox" class="form-check"></div>'+
              '</div>'+
            '</form>'+
          '</div>'+
        '</div>'+
        '<div ng-show="tbPropertyData.edit" tb-custom-properties-form tb-properties="tbPropertyData.properties" tb-popup="tbPopup" tb-col-offset="tbColOffset" tb-col-label="tbColLabel" tb-readonly="tbReadonly">'+
        '</div>'+
      '</div>'
    restrict: 'A'
    link: (scope, element, attrs) =>
      if scope.tbPopup == undefined
        scope.tbPopup = false
      if scope.tbReadonly == undefined
        scope.tbReadonly = false
      if scope.tbColOffset == undefined
        scope.tbColOffset = 1
      else
        scope.tbColOffset = Number(scope.tbColOffset)
      if scope.tbColLabel == undefined
        scope.tbColLabel = 3
      else
        scope.tbColLabel = Number(scope.tbColLabel)
]

@directives.directive 'tbCustomPropertiesForm', ['DataService', 'ErrorService'
  (@DataService, @ErrorService) ->
    scope:
      tbProperties: '='
      tbPopup: '<?'
      tbColLabel: '<?'
      tbColOffset: '<?'
      tbReadonly: '<?'
    template: ''+
      '<div ng-if="tbProperties.length > 0">'+
        '<div class="row">'+
          '<div class="col-xs-12">'+
            '<div ng-class="{\'form-separator\': !tbPopup, \'form-separator-popup\': tbPopup}">'+
              '<h4 class="title">Additional properties <span uib-tooltip="Properties specific to the community. Required properties will need to be completed before executing tests."><i class="fa fa-question-circle"></i></span></h4>'+
            '</div>'+
          '</div>'+
        '</div>'+
        '<div class="row">'+
          '<div ng-class="\'col-xs-\'+(11-tbColOffset)+\' col-xs-offset-\'+tbColOffset">'+
            '<form class="form-horizontal">'+
              '<div class="form-group" ng-repeat="property in tbProperties">'+
                '<label ng-class="\'col-xs-\'+tbColLabel" class="control-label" ng-attr-for="{{\'prop-\'+property.id}}"><span ng-if="property.use == \'R\'">* </span>{{property.name}}:</label>'+
                '<div ng-class="\'col-xs-\'+(11-tbColLabel)" ng-if="property.kind == \'SIMPLE\'">'+
                  '<p ng-if="isReadonly" class="form-control-static">{{property.value}}<span ng-if="property.desc" ng-style="{\'margin-left\':(property.value?\'20px\':\'0px\')}" uib-tooltip="{{property.desc}}"><i class="fa fa-question-circle"></i></span></p>'+
                  '<input ng-if="!isReadonly" ng-attr-id="{{\'prop-\'+property.id}}" ng-model="property.value" ng-readonly="property.adminOnly && !isAdmin" class="form-control" type="text"/>'+
                '</div>'+
                '<div ng-if="property.kind == \'SECRET\'">'+
                  '<div ng-class="\'col-xs-\'+(9-tbColLabel)">'+
                    '<p ng-if="isReadonly" class="form-control-static">{{property.value}}<span ng-if="property.desc" ng-style="{\'margin-left\':(property.value?\'20px\':\'0px\')}" uib-tooltip="{{property.desc}}"><i class="fa fa-question-circle"></i></span></p>'+
                    '<input ng-if="!isReadonly" ng-attr-id="{{\'prop-\'+property.id}}" ng-model="property.value" ng-readonly="!property.changeValue" class="form-control" ng-attr-type="{{property.showValue?\'text\':\'password\'}}"/>'+
                    '<div class="checkbox" ng-if="property.changeValue" ng-disabled="property.adminOnly && !isAdmin">'+
                      '<label>'+
                        '<input type="checkbox" ng-model="property.showValue">Show'+
                      '</label>'+
                    '</div>'+
                  '</div>'+
                  '<div class="col-xs-2" ng-if="!isReadonly">'+
                    '<span ng-if="property.adminOnly && !isAdmin">&nbsp;</span>'+
                    '<label class="checkbox-inline" ng-if="!property.adminOnly || isAdmin">'+
                      '<input type="checkbox" ng-model="property.changeValue" ng-change="editSecret(property)">Update'+
                    '</label>'+
                  '</div>'+
                '</div>'+
                '<div ng-class="\'col-xs-\'+(11-tbColLabel)" ng-if="property.kind == \'BINARY\'">'+
                  '<div ng-if="isReadonly">'+
                    '<p class="form-control-static"><a ng-if="property.value" href="" ng-click="downloadProperty(property)" style="padding-right:10px;">{{fileName(property)}}</a><span ng-if="isReadonly && property.desc" ng-style="{\'margin-left\':(property.value?\'20px\':\'0px\')}" uib-tooltip="{{property.desc}}"><i class="fa fa-question-circle"></i></span></p>'+
                  '</div>'+
                  '<div ng-if="!isReadonly">'+
                    '<span ng-if="!property.value && property.adminOnly && !isAdmin" class="form-control-static">&nbsp;</span>'+
                    '<p ng-if="property.value && property.adminOnly && !isAdmin" class="form-control-static"><a href="" ng-click="downloadProperty(property)" style="padding-right:10px;">{{fileName(property)}}</a></p>'+
                    '<span ng-if="property.value && (!property.adminOnly || isAdmin)" class="form-control-static"><a href="" ng-click="downloadProperty(property)" style="padding-right:10px;">{{fileName(property)}}</a></span>'+
                    '<button type="button" class="btn btn-default" ng-if="!property.adminOnly || isAdmin" ng-file-select="onFileSelect(property, $files)">Upload</button>'+
                    '<button type="button" class="btn btn-default" ng-if="property.value && (!property.adminOnly || isAdmin)" style="margin-left:5px" ng-click="removeFile(property)">Remove</button>'+
                  '</div>'+
                '</div>'+
                '<div class="form-control-static" ng-if="!isReadonly && property.desc"><span uib-tooltip="{{property.desc}}"><i class="fa fa-question-circle"></i></span></div>'+
              '</div>'+
            '</form>'+
          '</div>'+
        '</div>'+
      '</div>'
    restrict: 'A'
    link: (scope, element, attrs) =>
      if scope.tbPopup == undefined
        scope.tbPopup = false
      if scope.tbReadonly == undefined
        scope.tbReadonly = false
      if scope.tbColOffset == undefined
        scope.tbColOffset = 1
      else
        scope.tbColOffset = Number(scope.tbColOffset)
      if scope.tbColLabel == undefined
        scope.tbColLabel = 3
      else
        scope.tbColLabel = Number(scope.tbColLabel)
      scope.isAdmin = @DataService.isSystemAdmin || @DataService.isCommunityAdmin
      scope.isReadonly = @DataService.isVendorUser || scope.tbReadonly
      if scope.tbProperties?
        for property in scope.tbProperties
          if property.kind == 'SECRET' && property.configured
            property.value = '*****'

      scope.editSecret = (property) =>
        if property.changeValue
          property.value = ''
          property.showValue = false
          @DataService.focus('prop-'+property.id)
        else
          if property.configured
            property.value = '*****'
          else 
            property.value = ''

      scope.removeFile = (property) =>
        delete property.value
        delete property.file

      scope.onFileSelect = (property, files) =>
        file = _.head files
        if file?
          if file.size >= (Number(@DataService.configuration['savedFile.maxSize']) * 1024)
            @ErrorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for files is '+@DataService.configuration['savedFile.maxSize']+' KBs.')
          else
            property.file = file
            reader = new FileReader()
            reader.readAsDataURL property.file
            reader.onload = (event) =>
              property.value = event.target.result
              scope.$apply()
            reader.onerror = (event) =>
              @ErrorService.showErrorMessage(error)

      scope.fileName = (property) =>
        name = ''
        if (property.file?)
          name = property.file.name
        else
          if (property.value?)
            mimeType = @DataService.mimeTypeFromDataURL(property.value)
            extension = @DataService.extensionFromMimeType(mimeType)
            name = property.testKey + extension
        name
      
      scope.downloadProperty = (property) =>
        mimeType = @DataService.mimeTypeFromDataURL(property.value)
        extension = @DataService.extensionFromMimeType(mimeType)
        blob = @DataService.b64toBlob(@DataService.base64FromDataURL(property.value), mimeType)
        if (property.file?)
          saveAs(blob, property.file.name)
        else
          saveAs(blob, property.testKey+extension)

]
