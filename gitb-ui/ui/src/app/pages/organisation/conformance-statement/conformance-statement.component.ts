import { AfterViewInit, Component, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { SystemService } from 'src/app/services/system.service';
import { TestService } from 'src/app/services/test.service';
import { ConformanceConfiguration } from './conformance-configuration';
import { ConformanceEndpoint } from './conformance-endpoint';
import { ConformanceTestCase } from './conformance-test-case';
import { ConformanceTestSuite } from './conformance-test-suite';
import { EndpointRepresentation } from './endpoint-representation';
import { cloneDeep, find, map, remove } from 'lodash'
import { ParameterPresetValue } from 'src/app/types/parameter-preset-value';
import { SystemConfigurationParameter } from 'src/app/types/system-configuration-parameter';
import { Observable, forkJoin, mergeMap, of } from 'rxjs'
import { MissingConfigurationModalComponent } from 'src/app/modals/missing-configuration-modal/missing-configuration-modal.component';
import { EditEndpointConfigurationModalComponent } from 'src/app/modals/edit-endpoint-configuration-modal/edit-endpoint-configuration-modal.component';
import { RoutingService } from 'src/app/services/routing.service';
import { TabsetComponent } from 'ngx-bootstrap/tabs';
import { ConformanceStatementTab } from './conformance-statement-tab';
import { LoadingStatus } from 'src/app/types/loading-status.type';
import { MissingConfigurationAction } from 'src/app/components/missing-configuration-display/missing-configuration-action';
import { Counters } from 'src/app/components/test-status-icons/counters';
import { saveAs } from 'file-saver'
import { CheckboxOption } from 'src/app/components/checkbox-option-panel/checkbox-option';
import { CheckboxOptionState } from 'src/app/components/checkbox-option-panel/checkbox-option-state';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { ReportSupportService } from 'src/app/services/report-support.service';

@Component({
  selector: 'app-conformance-statement',
  templateUrl: './conformance-statement.component.html',
  styleUrls: ['./conformance-statement.component.less']
})
export class ConformanceStatementComponent implements OnInit, AfterViewInit {

  communityId?: number
  communityIdOfStatement!: number
  snapshotId?: number
  organisationId!: number
  systemId!: number
  domainId?: number
  specId?: number
  actorId!: number
  loadingTests = true
  loadingConfiguration: LoadingStatus = {status: Constants.STATUS.NONE}
  Constants = Constants
  hasTests = false
  displayedTestSuites: ConformanceTestSuite[] = []
  testSuites: ConformanceTestSuite[] = []
  statusCounters?: Counters  
  lastUpdate?: string
  conformanceStatus = ''
  allTestsSuccessful = false
  endpoints: ConformanceEndpoint[] = []
  configurations: ConformanceConfiguration[] = []
  endpointRepresentations: EndpointRepresentation[] = []
  hasEndpoints = false
  hasMultipleEndpoints = false
  runTestClicked = false
  deletePending = false
  exportPending = false
  tabToShow = ConformanceStatementTab.tests
  @ViewChild('tabs', { static: false }) tabs?: TabsetComponent;
  collapsedDetails = false
  collapsedDetailsFinished = false
  hasBadge = false
  
  hasDisabledTests = false
  hasOptionalTests = false

  showResults = new Set<string>([Constants.TEST_CASE_RESULT.SUCCESS, Constants.TEST_CASE_RESULT.FAILURE, Constants.TEST_CASE_RESULT.UNDEFINED]);
  showOptional = true
  showDisabled = false

  executionModeSequential = "backgroundSequential"
  executionModeParallel = "backgroundParallel"
  executionModeInteractive = "interactive"
  executionModeLabelSequential = "Sequential background execution"
  executionModeLabelParallel = "Parallel background execution"
  executionModeLabelInteractive = "Interactive execution"
  
  executionMode = this.executionModeInteractive
  executionModeButton = this.executionModeLabelInteractive
  testCaseFilter?: string
  private static SHOW_SUCCEEDED = '0'
  private static SHOW_FAILED = '1'
  private static SHOW_INCOMPLETE = '2'
  private static SHOW_OPTIONAL = '3'
  private static SHOW_DISABLED = '4'
  testDisplayOptions!: CheckboxOption[][]
  refreshDisplayOptions = new EventEmitter<CheckboxOption[][]>()

  statement?: ConformanceStatementItem
  systemName?: string
  organisationName?: string
  snapshotLabel?: string
  isReadonly!: boolean

  constructor(
    public dataService: DataService,
    private route: ActivatedRoute,
    router: Router,
    private conformanceService: ConformanceService,
    private modalService: BsModalService,
    private systemService: SystemService,
    private confirmationDialogService: ConfirmationDialogService,
    private reportService: ReportService,
    private testService: TestService,
    private popupService: PopupService,
    private organisationService: OrganisationService,
    private routingService: RoutingService,
    private reportSupportService: ReportSupportService
  ) { 
    // Access the tab to show via router state to have it cleared upon refresh.
    const navigation = router.getCurrentNavigation()
    if (navigation?.extras?.state) {
      const tabParam = navigation.extras.state[Constants.NAVIGATION_PATH_PARAM.TAB]
      if (tabParam != undefined) {
        this.tabToShow = ConformanceStatementTab[tabParam as keyof typeof ConformanceStatementTab]
      }
      this.snapshotLabel = navigation.extras.state[Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_LABEL]
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.tabToShow == ConformanceStatementTab.configuration) {
        this.showConfigurationTab()
      }
    })
  }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID))
    this.actorId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ACTOR_ID))
    this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    }
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_ID)) {
      this.snapshotId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_ID))
    }
    this.isReadonly = this.snapshotId != undefined
    this.prepareTestFilter()
    // Load conformance statement and its results.
    const statementsLoaded = this.conformanceService.getConformanceStatement(this.systemId, this.actorId, this.snapshotId)
    const snapshotLabelLoaded = this.retrieveSnapshotLabel(this.snapshotId, this.snapshotLabel)
    forkJoin([statementsLoaded, snapshotLabelLoaded]).subscribe((results) => {
      const statementData = results[0]
      let snapshotLabel: string|undefined
      if (results[1]) {
        snapshotLabel = results[1]
      }
      // Party definition.
      this.systemName = statementData.system.fname
      this.organisationName = statementData.organisation.fname
      this.communityIdOfStatement = statementData.organisation.community
      // Statement definition.
      this.prepareStatement(statementData.statement)
      this.statement = statementData.statement
      this.routingService.conformanceStatementBreadcrumbs(this.organisationId, this.systemId, this.actorId, this.communityId, this.breadcrumbLabel(), this.organisationName, this.systemName, this.snapshotId, snapshotLabel)
      // IDs.
      this.domainId = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN)!.id
      this.specId = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION)!.id
      // Test results.
      for (let testSuite of statementData.results.testSuites) {
        testSuite.hasDisabledTestCases = find(testSuite.testCases, (testCase) => testCase.disabled) != undefined
        testSuite.hasOptionalTestCases = find(testSuite.testCases, (testCase) => testCase.optional) != undefined
        if (!this.hasDisabledTests && testSuite.hasDisabledTestCases) {
          this.hasDisabledTests = true
        }
        if (!this.hasOptionalTests && testSuite.hasOptionalTestCases) {
          this.hasOptionalTests = true
        }
      }
      this.testSuites = statementData.results.testSuites
      this.displayedTestSuites = this.testSuites
      this.statusCounters = { 
        completed: statementData.results.summary.completed, failed: statementData.results.summary.failed, other: statementData.results.summary.undefined,
        completedOptional: statementData.results.summary.completedOptional, failedOptional: statementData.results.summary.failedOptional, otherOptional: statementData.results.summary.undefinedOptional
      }
      this.lastUpdate = statementData.results.summary.updateTime
      if (this.lastUpdate) {
        this.hasTests = true
      }
      this.conformanceStatus = statementData.results.summary.result
      this.allTestsSuccessful = this.conformanceStatus == Constants.TEST_CASE_RESULT.SUCCESS
      this.hasBadge = statementData.results.summary.hasBadge
      this.prepareTestFilter()
      this.applySearchFilters()
    }).add(() => {
      this.loadingTests = false
    })
  }

  private retrieveSnapshotLabel(snapshotId: number|undefined, labelFromNavigation: string|undefined): Observable<string|null> {
    let snapshotLabelLoaded: Observable<string|null>
    if (snapshotId != undefined) {
      if (labelFromNavigation == undefined) {
        snapshotLabelLoaded = this.conformanceService.getConformanceSnapshot(snapshotId).pipe(mergeMap((snapshot) => of(snapshot.label)))
      } else {
        snapshotLabelLoaded = of(labelFromNavigation)
      }
    } else {
      snapshotLabelLoaded = of(null)
    }
    return snapshotLabelLoaded
  }

  private appendToLabel(label: string, newPart: ConformanceStatementItem|undefined): string {
    if (newPart && !newPart.hidden) {
      if (label.length > 0) label += ' - '
      label += newPart.name
    }
    return label
  }

  private breadcrumbLabel(): string {
    let label = ''
    if (this.statement) {
      const domainItem = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN)
      const specGroupItem = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP)
      const specItem = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION)
      const actorItem = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.ACTOR)
      label = this.appendToLabel(label, domainItem)
      label = this.appendToLabel(label, specGroupItem)
      label = this.appendToLabel(label, specItem)
      label = this.appendToLabel(label, actorItem)
    }
    return label
  }

  private showConfigurationTab() {
    this.loadConfigurations()
    if (this.tabs) {
      this.tabs.tabs[1].active = true
    }
  }

  private prepareTestFilter(): void {
    this.testDisplayOptions = [[
        {key: ConformanceStatementComponent.SHOW_SUCCEEDED, label: 'Succeeded tests', default: true, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)},
        {key: ConformanceStatementComponent.SHOW_FAILED, label: 'Failed tests', default: true, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)},
        {key: ConformanceStatementComponent.SHOW_INCOMPLETE, label: 'Incomplete tests', default: true, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)}
    ]]
    if (this.hasOptionalTests) {
      this.testDisplayOptions.push([{key: ConformanceStatementComponent.SHOW_OPTIONAL, label: 'Optional tests', default: true}])
    }
    if (this.hasDisabledTests) {
      this.testDisplayOptions.push([{key: ConformanceStatementComponent.SHOW_DISABLED, label: 'Disabled tests', default: false}])
    }
    this.refreshDisplayOptions.emit(this.testDisplayOptions)
  }

  private findByType(items: ConformanceStatementItem[], itemType: number): ConformanceStatementItem|undefined {
    if (items) {
      for (let item of items) {
        if (item.itemType == itemType) {
          return item;
        } else if (item.items) {
          return this.findByType(item.items, itemType)
        }
      }
      return undefined
    } else {
      return undefined
    }
  }

  private prepareStatement(statement: ConformanceStatementItem) {
    if (statement.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN) {
      // Hide the domain unless the user has access to any domain.
      statement.hidden = this.dataService.community?.domain != undefined
    }
  }

  loadConfigurations() {
    if (this.loadingConfiguration.status == Constants.STATUS.NONE) {
      this.loadingConfiguration.status = Constants.STATUS.PENDING
      this.conformanceService.getSystemConfigurations(this.actorId, this.systemId)
      .subscribe((data) => {
        const endpointsTemp: ConformanceEndpoint[] = []
        const configurations: ConformanceConfiguration[] = []
        for (let endpointConfig of data) {
          let endpoint: ConformanceEndpoint = {
            id: endpointConfig.id,
            name: endpointConfig.name,
            description: endpointConfig.description,
            parameters: []
          }
          for (let parameterConfig of endpointConfig.parameters) {
            endpoint.parameters.push(parameterConfig)
            if (parameterConfig.configured) {
              configurations.push({
                system: this.systemId,
                value: parameterConfig.value,
                endpoint: endpointConfig.id,
                parameter: parameterConfig.id,
                mimeType: parameterConfig.mimeType,
                configured: parameterConfig.configured
              })
            }
          }
          if (endpoint.parameters.length > 0) {
            endpointsTemp.push(endpoint)
          }
        }
        this.endpoints = endpointsTemp
        this.configurations = configurations
        this.constructEndpointRepresentations()
      }).add(() => {
        this.loadingConfiguration.status = Constants.STATUS.FINISHED
      })
    }
  }

  checkPrerequisite(parameterMap: {[key: string]: SystemConfigurationParameter}, repr: SystemConfigurationParameter): boolean {
    if (repr.checkedPrerequisites == undefined) {
      if (repr.dependsOn != undefined) {
        let otherPrerequisites = this.checkPrerequisite(parameterMap, parameterMap[repr.dependsOn])
        let valueCheck = parameterMap[repr.dependsOn].value == repr.dependsOnValue
        repr.prerequisiteOk = otherPrerequisites && valueCheck
      } else {
        repr.prerequisiteOk = true
      }
      repr.checkedPrerequisites = true
    }
    return repr.prerequisiteOk!
  }

  constructEndpointRepresentations() {
    this.endpointRepresentations = []
    for (let endpoint of this.endpoints) {
      const endpointRepr: EndpointRepresentation = {
        id: endpoint.id,
        name: endpoint.name,
        description: endpoint.description,
        parameters: []
      }
      const parameterMap: {[key: string]: SystemConfigurationParameter} = {}
      for (let parameter of endpoint.parameters) {
        const repr = cloneDeep(parameter)
        const relevantConfig = find(this.configurations, (config) => {
          return Number(parameter.id) == Number(config.parameter) && Number(parameter.endpoint) == Number(config.endpoint)
        })!
        if (relevantConfig != undefined) {
          repr.value = relevantConfig.value
          repr.configured = relevantConfig.configured
        } else {
          repr.configured = false
          repr.value = undefined
        }
        if (repr.configured) {
          if (parameter.kind == 'BINARY') {
            repr.fileName = parameter.testKey
            if (relevantConfig.mimeType != undefined) {
              repr.fileName += this.dataService.extensionFromMimeType(relevantConfig.mimeType)
            }
            repr.mimeType = relevantConfig.mimeType
          } else if (parameter.kind == 'SECRET') {
            repr.value = '*****'
          } else if (parameter.kind == 'SIMPLE') {
            if (parameter.allowedValues != undefined) {
              const presetValues: ParameterPresetValue[] = JSON.parse(parameter.allowedValues)
              if (presetValues != undefined && presetValues.length > 0) {
                const foundPresetValue = find(presetValues, (v) => { return v.value == repr.value } )
                if (foundPresetValue != undefined) {
                  repr.valueToShow = foundPresetValue.label
                }
              }
            }
          }
        }
        parameterMap[parameter.name] = repr
        if (!parameter.hidden || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
          endpointRepr.parameters.push(repr)
        }
      }
      let hasVisibleParameters = false
      for (let p of endpointRepr.parameters) {
        if (this.checkPrerequisite(parameterMap, p)) {
          hasVisibleParameters = true
        }
      }
      if (hasVisibleParameters) {
        this.endpointRepresentations.push(endpointRepr)
      }
    }
    this.hasEndpoints = this.endpointRepresentations.length > 0
    this.hasMultipleEndpoints = this.endpointRepresentations.length > 1
  }
  
  onExpand(testSuite: ConformanceTestSuite) {
    if (!this.runTestClicked) {
      testSuite.expanded = !testSuite.expanded
    }
    this.runTestClicked = false
  }

  resultFilterUpdated(choices: CheckboxOptionState) {
    this.showOptional = choices[ConformanceStatementComponent.SHOW_OPTIONAL]
    this.showDisabled = choices[ConformanceStatementComponent.SHOW_DISABLED]
    if (choices[ConformanceStatementComponent.SHOW_SUCCEEDED]) {
      this.showResults.add(Constants.TEST_CASE_RESULT.SUCCESS)
    } else {
      this.showResults.delete(Constants.TEST_CASE_RESULT.SUCCESS)
    }
    if (choices[ConformanceStatementComponent.SHOW_FAILED]) {
      this.showResults.add(Constants.TEST_CASE_RESULT.FAILURE)
    } else {
      this.showResults.delete(Constants.TEST_CASE_RESULT.FAILURE)
    }
    if (choices[ConformanceStatementComponent.SHOW_INCOMPLETE]) {
      this.showResults.add(Constants.TEST_CASE_RESULT.UNDEFINED)
    } else {
      this.showResults.delete(Constants.TEST_CASE_RESULT.UNDEFINED)
    }
    this.applySearchFilters()
  }

  applySearchFilters() {
    let testCaseFilter = this.testCaseFilter
    if (testCaseFilter != undefined) {
      testCaseFilter = testCaseFilter.trim()
      if (testCaseFilter.length == 0) {
        testCaseFilter = undefined
      } else {
        testCaseFilter = testCaseFilter.toLocaleLowerCase()
      }
    }
    let filteredTestSuites: ConformanceTestSuite[] = []
    for (let testSuite of this.testSuites) {
      let testCases: ConformanceTestCase[] = []
      for (let testCase of testSuite.testCases) {
        if (this.showResults.has(testCase.result) 
          && (!testCase.optional || this.showOptional)
          && (!testCase.disabled || this.showDisabled)
          && (testCaseFilter == undefined || 
              (testCase.sname.toLocaleLowerCase().indexOf(testCaseFilter) >= 0) || 
              (testCase.description != undefined && testCase.description.toLocaleLowerCase().indexOf(testCaseFilter) >= 0))) {
          testCases.push(testCase)
        }
      }
      if (testCases.length > 0) {
        filteredTestSuites.push({
          id: testSuite.id,
          sname: testSuite.sname,
          result: testSuite.result,
          hasDocumentation: testSuite.hasDocumentation,
          expanded: true,
          description: testSuite.description,
          hasOptionalTestCases: testSuite.hasOptionalTestCases && this.showOptional,
          hasDisabledTestCases: testSuite.hasDisabledTestCases && this.showDisabled,
          testCases: testCases,
          specReference: testSuite.specReference,
          specDescription: testSuite.specDescription,
          specLink: testSuite.specLink
        })
      }
    }
    this.displayedTestSuites = filteredTestSuites
  }

  private executeHeadless(testCases: ConformanceTestCase[]) {
    // Check configurations
    const organisationParameterCheck = this.organisationService.checkOrganisationParameterValues(this.organisationId)
    const systemParameterCheck = this.systemService.checkSystemParameterValues(this.systemId)
    const statementParameterCheck = this.conformanceService.checkConfigurations(this.actorId, this.systemId)
    // Check status once everything is loaded.
    forkJoin([organisationParameterCheck, systemParameterCheck, statementParameterCheck])
    .subscribe((data) => {
      const organisationProperties = data[0]
      const systemProperties = data[1]
      const endpoints = data[2]
      const statementProperties = this.dataService.getEndpointParametersToDisplay(endpoints)
      let organisationConfigurationValid = this.dataService.isMemberConfigurationValid(organisationProperties)
      let systemConfigurationValid = this.dataService.isMemberConfigurationValid(systemProperties)
      let configurationValid = this.dataService.isConfigurationValid(endpoints)
      if (!configurationValid || !systemConfigurationValid || !organisationConfigurationValid) {
        // Missing configuration.
        const modalRef = this.modalService.show(MissingConfigurationModalComponent, {
          class: 'modal-lg',
          initialState: {
            organisationProperties: organisationProperties,
            organisationConfigurationValid: organisationConfigurationValid,
            systemProperties: systemProperties,
            systemConfigurationValid: systemConfigurationValid,
            statementProperties: statementProperties,
            configurationValid: configurationValid
          }
        })
        modalRef.content?.action.subscribe((actionType: MissingConfigurationAction) => {
          if (actionType == MissingConfigurationAction.viewStatement) {
            this.showConfigurationTab()
          } else if (actionType == MissingConfigurationAction.viewOrganisation) {
            if (this.dataService.isVendorUser || this.dataService.isVendorAdmin) {
              this.routingService.toOwnOrganisationDetails(undefined, true)
            } else {
              if (this.dataService.vendor!.id == this.organisationId) {
                this.routingService.toOwnOrganisationDetails(undefined, true)
              } else {
                this.organisationService.getOrganisationBySystemId(this.systemId)
                .subscribe((data) => {
                  this.routingService.toOrganisationDetails(data.community, data.id, undefined, true)
                })
              }
            }
          } else if (actionType == MissingConfigurationAction.viewSystem) {
            if (this.dataService.isVendorUser || this.dataService.isVendorAdmin) {
              this.routingService.toOwnSystemDetails(this.systemId, true)
            } else {
              if (this.dataService.vendor!.id == this.organisationId) {
                this.routingService.toOwnSystemDetails(this.systemId, true)
              } else {
                this.routingService.toSystemDetails(this.communityId!, this.organisationId, this.systemId, true)
              }
            }
          }
        })
      } else {
        // Proceed with execution.
        const testCaseIds = map(testCases, (test) => { return test.id } )
        this.testService.startHeadlessTestSessions(testCaseIds, this.specId!, this.systemId, this.actorId, this.executionMode == this.executionModeSequential)
        .subscribe(() => {
          if (testCaseIds.length == 1) {
            this.popupService.success('Started test session.<br/>Check <b>Test Sessions</b> for progress.')
          } else {
            this.popupService.success('Started '+testCaseIds.length+' test sessions.<br/>Check <b>Test Sessions</b> for progress.')
          }
        })
      }
    })
  }

  onTestSelect(test: ConformanceTestCase) {
    if (this.executionMode == this.executionModeInteractive) {
      this.dataService.setTestsToExecute([test])
      if (this.communityId == undefined) {
        this.routingService.toOwnTestCaseExecution(this.organisationId, this.systemId, this.actorId, test.id)
      } else {
        this.routingService.toTestCaseExecution(this.communityId, this.organisationId, this.systemId, this.actorId, test.id)
      }
    } else {
      this.executeHeadless([test])
    }
  }

  onTestSuiteSelect(testSuite: ConformanceTestSuite) {
    const testsToExecute: ConformanceTestCase[] = []
    for (let testCase of testSuite.testCases) {
      if (!testCase.disabled) {
        testsToExecute.push(testCase)
      }
    }
    if (this.executionMode == this.executionModeInteractive) {
      this.dataService.setTestsToExecute(testsToExecute)
      if (this.communityId == undefined) {
        this.routingService.toOwnTestSuiteExecution(this.organisationId, this.systemId, this.actorId, testSuite.id)
      } else {
        this.routingService.toTestSuiteExecution(this.communityId, this.organisationId, this.systemId, this.actorId, testSuite.id)
      }
    } else {
      this.executeHeadless(testsToExecute)
    }
  }

  onParameterSelect(parameter: SystemConfigurationParameter) {
    const oldConfiguration = find(this.configurations, (configuration) => {
      return parameter.id == configuration.parameter 
        && configuration.configured 
        && Number(configuration.endpoint) == Number(parameter.endpoint)
    })
    const endpoint = find(this.endpoints, (endpoint) => { return Number(parameter.endpoint) == Number(endpoint.id) })
    const modalRef = this.modalService.show(EditEndpointConfigurationModalComponent, {
      class: 'modal-m',
      initialState: {
        endpoint: endpoint,
        parameter: parameter,
        systemId: this.systemId,
        oldConfiguration: oldConfiguration
      }
    })
    modalRef.content?.action.subscribe((result: {operation: number, configuration: ConformanceConfiguration}) => {
      switch (result.operation) {
        case Constants.OPERATION.ADD:
          if (result.configuration.configured) {
            this.configurations.push(result.configuration)
          }
          break
        case Constants.OPERATION.UPDATE:
          if (oldConfiguration != undefined && result.configuration.configured) {
            oldConfiguration.value = result.configuration.value
            oldConfiguration.configured = result.configuration.configured
            oldConfiguration.mimeType = result.configuration.mimeType
          }
          break
        case Constants.OPERATION.DELETE:
          if (oldConfiguration != undefined) {
            remove(this.configurations, (configuration) => {
              return configuration.parameter == oldConfiguration.parameter &&
                Number(configuration.endpoint) == Number(oldConfiguration.endpoint)
            })
          }
          break
        default:
          break;
      }
      this.constructEndpointRepresentations()
    })
  }

  onParameterDownload(parameter: SystemConfigurationParameter) {
    this.systemService.downloadEndpointConfigurationFile(this.systemId, parameter.id, parameter.endpoint)
    .subscribe((data) => {
      const extension = this.dataService.extensionFromMimeType(parameter.mimeType)
      let fileName = parameter.testKey + extension      
      const blobData = new Blob([data], {type: parameter.mimeType})
      saveAs(blobData, fileName)
    })
  }

  canDelete() {
    return !this.isReadonly && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowStatementManagement && (this.dataService.community!.allowPostTestStatementUpdates || !this.hasTests)))
  }

  canEditParameter(parameter: SystemConfigurationParameter) {
    return this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && !parameter.adminOnly && (this.dataService.community!.allowPostTestStatementUpdates || !this.hasTests))
  }

  deleteConformanceStatement() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this conformance statement?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.systemService.deleteConformanceStatement(this.systemId, [this.actorId])
      .subscribe(() => {
        this.back()
        this.popupService.success('Conformance statement deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  onExportConformanceStatement(format: 'xml'|'pdf') {
    this.exportPending = true    
    this.reportSupportService.handleConformanceStatementReport(this.communityIdOfStatement, this.actorId, this.systemId, this.snapshotId, format, false)
    .subscribe(() => {
      this.exportPending = false
    })
  }

  onExportConformanceCertificate() {
    this.exportPending = true
    this.reportService.exportOwnConformanceCertificateReport(this.actorId, this.systemId, this.snapshotId)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "conformance_certificate.pdf");
    }).add(() => {
      this.exportPending = false
    })
  }

  toTestSession(sessionId: string) {
    if (this.organisationId == this.dataService.vendor?.id) {
      this.routingService.toTestHistory(this.organisationId, sessionId)
    } else {
      this.routingService.toSessionDashboard(sessionId)
    }
  }

  toCommunity() {
    this.routingService.toCommunity(this.communityId!)
  }

  toOrganisation() {
    if (this.communityId == undefined || this.organisationId == this.dataService.vendor!.id) {
      this.routingService.toOwnOrganisationDetails()
    } else {
      this.routingService.toOrganisationDetails(this.communityId!, this.organisationId)
    }
  }  

  toSystem() {
    if (this.communityId == undefined || this.organisationId == this.dataService.vendor!.id) {
      this.routingService.toOwnSystemDetails(this.systemId)
    } else {
      this.routingService.toSystemDetails(this.communityId!, this.organisationId, this.systemId)
    }
  }

  toDomain() {
    this.routingService.toDomain(this.domainId!)
  }

  toSpecification() {
    this.routingService.toSpecification(this.domainId!, this.specId!)
  }

  toActor() {
    this.routingService.toActor(this.domainId!, this.specId!, this.actorId)
  }

  showToCommunity() {
    return this.communityId != undefined && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)
  }

  showToDomain() {
    return this.domainId != undefined && this.domainId >= 0 && (
      this.dataService.isSystemAdmin || (
        this.dataService.isCommunityAdmin && this.dataService.community?.domain != undefined
      )
    )
  }

  showToSpecification() {
    return this.showToDomain() && this.specId != undefined && this.specId >= 0
  }

  showToActor() {
    return this.showToSpecification() && this.actorId >= 0
  }

  showSpecificationNavigation() {
    return this.showToDomain() || this.showToSpecification() || this.showToActor()
  }

  toggleOverviewCollapse(value: boolean) {
    setTimeout(() => {
      this.collapsedDetailsFinished = value
    }, 1)
  }

  back() {
    if (this.communityId == undefined) {
      this.routingService.toOwnConformanceStatements(this.organisationId, this.systemId, this.snapshotId)
    } else {
      this.routingService.toConformanceStatements(this.communityId, this.organisationId, this.systemId, this.snapshotId)
    }
  }
}
