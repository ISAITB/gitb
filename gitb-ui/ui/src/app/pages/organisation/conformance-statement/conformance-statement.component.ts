import { AfterViewInit, Component, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { saveAs } from 'file-saver';
import { find, map } from 'lodash';
import { BsModalService } from 'ngx-bootstrap/modal';
import { TabsetComponent } from 'ngx-bootstrap/tabs';
import { Observable, forkJoin, mergeMap, of } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { CheckboxOption } from 'src/app/components/checkbox-option-panel/checkbox-option';
import { CheckboxOptionState } from 'src/app/components/checkbox-option-panel/checkbox-option-state';
import { Counters } from 'src/app/components/test-status-icons/counters';
import { MissingConfigurationAction } from 'src/app/modals/missing-configuration-modal/missing-configuration-action';
import { MissingConfigurationModalComponent } from 'src/app/modals/missing-configuration-modal/missing-configuration-modal.component';
import { CommunityService } from 'src/app/services/community.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportSupportService } from 'src/app/services/report-support.service';
import { ReportService } from 'src/app/services/report.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemService } from 'src/app/services/system.service';
import { TestService } from 'src/app/services/test.service';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { EndpointParameter } from 'src/app/types/endpoint-parameter';
import { LoadingStatus } from 'src/app/types/loading-status.type';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { SystemParameter } from 'src/app/types/system-parameter';
import { ConformanceStatementTab } from './conformance-statement-tab';
import { ConformanceTestCase } from './conformance-test-case';
import { ConformanceTestSuite } from './conformance-test-suite';
import { ConfigurationPropertyVisibility } from 'src/app/types/configuration-property-visibility';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { BaseComponent } from '../../base-component.component';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
  selector: 'app-conformance-statement',
  templateUrl: './conformance-statement.component.html',
  styleUrls: ['./conformance-statement.component.less']
})
export class ConformanceStatementComponent extends BaseComponent implements OnInit, AfterViewInit {

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
  runTestClicked = false
  deletePending = false
  exportPending = false
  updateConfigurationPending = false
  tabToShow = ConformanceStatementTab.tests
  @ViewChild('tabs', { static: false }) tabs?: TabsetComponent;
  collapsedDetails = false
  collapsedDetailsFinished = false
  hasBadge = false
  hasDisabledTests = false
  hasOptionalTests = false
  canEditOrganisationConfiguration = false
  canEditSystemConfiguration = false
  canEditStatementConfiguration = false

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

  organisationProperties: OrganisationParameter[] = []
  systemProperties: SystemParameter[] = []
  statementProperties: EndpointParameter[] = []
  organisationPropertiesCollapsed = false
  systemPropertiesCollapsed = false
  statementPropertiesCollapsed = false
  organisationPropertyVisibility?: ConfigurationPropertyVisibility
  systemPropertyVisibility?: ConfigurationPropertyVisibility
  statementPropertyVisibility?: ConfigurationPropertyVisibility
  propertyValidation = new ValidationState()

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
    private reportSupportService: ReportSupportService,
    private communityService: CommunityService
  ) { 
    super()
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
      this.canEditOrganisationConfiguration = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && (this.dataService.community!.allowPostTestOrganisationUpdates || !this.hasTests))
      this.canEditSystemConfiguration = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && (this.dataService.community!.allowPostTestSystemUpdates || !this.hasTests))
      this.canEditStatementConfiguration = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && (this.dataService.community!.allowPostTestStatementUpdates || !this.hasTests))
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
      const systemConfiguration = this.systemService.getSystemParameterValues(this.systemId)
      const organisationConfiguration = this.communityService.getOrganisationParameterValues(this.organisationId)
      const statementConfiguration = this.conformanceService.getStatementParameterValues(this.actorId, this.systemId)
      forkJoin([organisationConfiguration, systemConfiguration, statementConfiguration])
      .subscribe((data) => {
        // Organisation properties
        this.organisationProperties = data[0]
        // System properties
        this.systemProperties = data[1]
        // Statement properties
        this.statementProperties = data[2]
        // Determine visibility of properties
        this.organisationPropertyVisibility = this.dataService.checkPropertyVisibility(this.organisationProperties)
        this.systemPropertyVisibility = this.dataService.checkPropertyVisibility(this.systemProperties)
        this.statementPropertyVisibility = this.dataService.checkPropertyVisibility(this.statementProperties)
        // Initialise validation status
        this.organisationProperties.forEach((p) => this.propertyValidation.set('organisation'+p.id))
        this.systemProperties.forEach((p) => this.propertyValidation.set('system'+p.id))
        this.statementProperties.forEach((p) => this.propertyValidation.set('statement'+p.id))
        // Highlight validation issues
        this.applyPropertyValidation()
      }).add(() => {
        this.loadingConfiguration.status = Constants.STATUS.FINISHED
      })
    }
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

  private validateConfiguration(testSuite: ConformanceTestSuite|undefined, testCase: ConformanceTestCase|undefined): Observable<boolean> {
    if (testSuite) {
      testSuite.executionPending = true
    } else if (testCase) {
      testCase.executionPending = true
    }
    // Check configurations
    const organisationParameterCheck = this.organisationService.checkOrganisationParameterValues(this.organisationId)
    const systemParameterCheck = this.systemService.checkSystemParameterValues(this.systemId)
    const statementParameterCheck = this.conformanceService.checkConfigurations(this.actorId, this.systemId)
    // Check status once everything is loaded.
    return forkJoin([organisationParameterCheck, systemParameterCheck, statementParameterCheck]).pipe(
      mergeMap((data) => {
        const organisationProperties = data[0]
        const systemProperties = data[1]
        const endpoints = data[2]
        const statementProperties = this.dataService.getEndpointParametersToDisplay(endpoints)
        let organisationConfigurationValid = this.dataService.isMemberConfigurationValid(organisationProperties)
        let systemConfigurationValid = this.dataService.isMemberConfigurationValid(systemProperties)
        let configurationValid = this.dataService.isConfigurationValid(endpoints)
        if (testSuite) {
          testSuite.executionPending = false
        } else if (testCase) {
          testCase.executionPending = false
        }
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
          modalRef.content!.action.subscribe((action: MissingConfigurationAction) => {
            this.organisationPropertiesCollapsed = !action.viewOrganisationProperties
            this.systemPropertiesCollapsed = !action.viewSystemProperties
            this.statementPropertiesCollapsed = !action.viewStatementProperties
            this.showConfigurationTab()
          })
          return modalRef.onHidden!.pipe(
            mergeMap(() => {
              return of(false)
            })
          )
        } else {
          return of(true)
        }
      })
    )
  }

  private isPropertyValid(property: CustomProperty) {
    if (property.use == "R") {
      return property.configured
    } else {
      return true
    }
  }

  private setPropertyValidation(property: CustomProperty, propertyType: 'organisation'|'system'|'statement') {
    const invalid = !this.isPropertyValid(property)
    property.showAsInvalid = invalid
    this.propertyValidation.update(propertyType+property.id, {
      invalid: invalid,
      feedback: (property.kind == "BINARY")?"Required file missing.":"Required value missing."
    })    
  }

  private applyPropertyValidation() {
    this.organisationProperties.forEach(p => this.setPropertyValidation(p, 'organisation'))
    this.systemProperties.forEach(p => this.setPropertyValidation(p, 'system'))
    this.statementProperties.forEach(p => this.setPropertyValidation(p, 'statement'))
  }

  private executeHeadless(testCases: ConformanceTestCase[]) {
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

  onTestSelect(test: ConformanceTestCase) {
    // Check status once everything is loaded.
    this.validateConfiguration(undefined, test).subscribe((proceed) => {
      if (proceed) {
        // Proceed with execution.
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
    })
  }

  onTestSuiteSelect(testSuite: ConformanceTestSuite) {
    // Check status once everything is loaded.
    this.validateConfiguration(testSuite, undefined).subscribe((proceed) => {
      if (proceed) {
        // Proceed with execution.
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
    })
  }

  updateConfigurationDisabled() {
    return (!this.dataService.customPropertiesValid(this.organisationProperties)) || 
      (!this.dataService.customPropertiesValid(this.systemProperties)) ||
      (!this.dataService.customPropertiesValid(this.statementProperties))
  }

  private updatePropertyConfiguredStatus(property: CustomProperty) {
    if (property.kind == "SIMPLE") {
      property.configured = this.textProvided(property.value)
    } else if (property.kind == "SECRET") {
      property.configured = (property.changeValue == true && this.textProvided(property.value)) || (!property.changeValue && property.configured == true)
    } else {
      property.configured = property.configured || property.file != undefined
    }
  }

  updateConfiguration() {
    this.updateConfigurationPending = true
    this.conformanceService.updateStatementConfiguration(this.systemId, this.actorId, this.organisationProperties, this.systemProperties, this.statementProperties)
    .subscribe(() => {
      this.popupService.success('Configuration updated.')
    }).add(() => {
      this.organisationProperties.forEach(p => this.updatePropertyConfiguredStatus(p))
      this.systemProperties.forEach(p => this.updatePropertyConfiguredStatus(p))
      this.statementProperties.forEach(p => this.updatePropertyConfiguredStatus(p))
      this.applyPropertyValidation()
      this.updateConfigurationPending = false
    })
  }

  canDelete() {
    return !this.isReadonly && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowStatementManagement && (this.dataService.community!.allowPostTestStatementUpdates || !this.hasTests)))
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
