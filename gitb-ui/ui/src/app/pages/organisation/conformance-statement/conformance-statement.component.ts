/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import {Component, ElementRef, EventEmitter, NgZone, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {saveAs} from 'file-saver';
import {map} from 'lodash';
import {BsModalService} from 'ngx-bootstrap/modal';
import {finalize, forkJoin, mergeMap, Observable, of, tap} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {Counters} from 'src/app/components/test-status-icons/counters';
import {MissingConfigurationAction} from 'src/app/modals/missing-configuration-modal/missing-configuration-action';
import {MissingConfigurationModalComponent} from 'src/app/modals/missing-configuration-modal/missing-configuration-modal.component';
import {CommunityService} from 'src/app/services/community.service';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {PopupService} from 'src/app/services/popup.service';
import {ReportSupportService} from 'src/app/services/report-support.service';
import {ReportService} from 'src/app/services/report.service';
import {RoutingService} from 'src/app/services/routing.service';
import {SystemService} from 'src/app/services/system.service';
import {TestService} from 'src/app/services/test.service';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {EndpointParameter} from 'src/app/types/endpoint-parameter';
import {LoadingStatus} from 'src/app/types/loading-status.type';
import {OrganisationParameter} from 'src/app/types/organisation-parameter';
import {SystemParameter} from 'src/app/types/system-parameter';
import {ConformanceTestCase} from './conformance-test-case';
import {ConformanceTestSuite} from './conformance-test-suite';
import {ConfigurationPropertyVisibility} from 'src/app/types/configuration-property-visibility';
import {CustomProperty} from 'src/app/types/custom-property.type';
import {ValidationState} from 'src/app/types/validation-state';
import {share} from 'rxjs/operators';
import {TestCaseFilterState} from '../../../components/test-case-filter/test-case-filter-state';
import {TestCaseFilterOptions} from '../../../components/test-case-filter/test-case-filter-options';
import {TestCaseFilterApi} from '../../../components/test-case-filter/test-case-filter-api';
import {NavigationControlsConfig} from '../../../components/navigation-controls/navigation-controls-config';
import {BaseTabbedComponent} from '../../base-tabbed-component';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {TestCaseSearchCriteria} from '../../../types/test-case-search-criteria';
import {ConformanceStatus} from '../../../types/conformance-status';
import {PagingPlacement} from '../../../components/paging-controls/paging-placement';
import {PagingControlsApi} from '../../../components/paging-controls/paging-controls-api';
import {TestSuiteMinimalInfo} from '../../../types/test-suite-minimal-info';
import {FilterUpdate} from '../../../components/test-filter/filter-update';
import {TestSuiteDisplayComponentApi} from '../../../components/test-suite-display/test-suite-display-component-api';

@Component({
    selector: 'app-conformance-statement',
    templateUrl: './conformance-statement.component.html',
    styleUrls: ['./conformance-statement.component.less'],
    standalone: false
})
export class ConformanceStatementComponent extends BaseTabbedComponent implements OnInit {

  @ViewChild('testCaseResultFilter') testCaseResultFilter?: TestCaseFilterApi
  @ViewChild("pagingControls") pagingControls?: PagingControlsApi
  @ViewChildren("testSuiteDisplayComponent") testSuiteDisplayComponents?: QueryList<TestSuiteDisplayComponentApi>
  @ViewChild('conformanceDetailPage') conformanceDetailPage?: ElementRef
  @ViewChild('statusInfoContainer') statusInfoContainer?: ElementRef
  @ViewChild('resultsContainer') resultsContainer?: ElementRef

  communityId?: number
  communityIdOfStatement!: number
  snapshotId?: number
  organisationId!: number
  systemId!: number
  domainId?: number
  specId?: number
  actorId!: number
  updatingTests = true
  loadingTests: LoadingStatus = {status: Constants.STATUS.NONE}
  loadingConfiguration: LoadingStatus = {status: Constants.STATUS.NONE}
  Constants = Constants
  hasTests = false
  unfilteredTestCaseCount = 0
  unfilteredTestSuiteCount = 0
  displayedTestSuites: ConformanceTestSuite[] = []
  statusCounters?: Counters
  lastUpdate?: string
  conformanceStatus = ''
  allTestsSuccessful = false
  deletePending = false
  exportPending = false
  updateConfigurationPending = false
  collapsedDetails = false
  collapsedDetailsFinished = false
  hasBadge = false
  hasDisabledTests = false
  hasOptionalTests = false
  canEditOrganisationConfiguration = false
  canEditSystemConfiguration = false
  canEditStatementConfiguration = false
  navigationConfig?: NavigationControlsConfig
  protected readonly PagingPlacement = PagingPlacement;

  testSuiteSelectionConfig = {
    name: "testSuiteChoice",
    singleSelection: true,
    singleSelectionClearable: true,
    singleSelectionPersistent: false,
    textField: "sname",
    filterLabel: "Show test suite...",
    loader: () => this.loadStatementTestSuites()
  }
  selectedTestSuite: TestSuiteMinimalInfo|undefined

  testCaseSearchCriteria: TestCaseSearchCriteria = {
    succeeded: true,
    failed: true,
    incomplete: true,
    optional: true,
    disabled: false,
    testSuiteId: undefined,
    testCaseFilterText: undefined
  }

  testCaseFilterOptions?: TestCaseFilterOptions
  testCaseFilterState: TestCaseFilterState = {
    showSuccessful: this.testCaseSearchCriteria.succeeded,
    showFailed: this.testCaseSearchCriteria.failed,
    showIncomplete: this.testCaseSearchCriteria.incomplete,
    showOptional: this.testCaseSearchCriteria.optional,
    showDisabled: this.testCaseSearchCriteria.disabled
  }

  executionModeSequential = "backgroundSequential"
  executionModeParallel = "backgroundParallel"
  executionModeInteractive = "interactive"
  executionModeLabelSequential = "Sequential background execution"
  executionModeLabelParallel = "Parallel background execution"
  executionModeLabelInteractive = "Interactive execution"

  executionMode = this.executionModeInteractive
  executionModeButton = this.executionModeLabelInteractive

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

  resizeObserver!: ResizeObserver
  resultsWrapped = false
  refreshPending = false
  refreshCounters = new EventEmitter<Counters>()

  constructor(
    public readonly dataService: DataService,
    route: ActivatedRoute,
    router: Router,
    private readonly conformanceService: ConformanceService,
    private readonly modalService: BsModalService,
    private readonly systemService: SystemService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly reportService: ReportService,
    private readonly testService: TestService,
    private readonly popupService: PopupService,
    private readonly organisationService: OrganisationService,
    private readonly routingService: RoutingService,
    private readonly reportSupportService: ReportSupportService,
    private readonly communityService: CommunityService,
    private readonly zone: NgZone
  ) {
    super(router, route)
    const navigation = router.getCurrentNavigation()
    if (navigation?.extras?.state) {
      this.snapshotLabel = navigation.extras.state[Constants.NAVIGATION_PATH_PARAM.SNAPSHOT_LABEL]
    }
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit()
    this.pagingControls?.updateStatus(1, this.unfilteredTestCaseCount)
    setTimeout(() => {
      this.updateTestCaseFilterOptions()
    })
    this.resizeObserver = new ResizeObserver(() => {
      this.zone.run(() => {
        this.calculateWrapping()
      })
    })
    if (this.conformanceDetailPage) {
      this.resizeObserver.observe(this.conformanceDetailPage.nativeElement)
    }
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
    this.loadInitialData()
  }

  private loadInitialData(): Observable<any> {
    // Load conformance statement and its results.
    const statementsLoaded = this.conformanceService.getConformanceStatement(this.systemId, this.actorId, this.snapshotId)
    const snapshotLabelLoaded = this.retrieveSnapshotLabel(this.snapshotId, this.snapshotLabel)
    const obs$ = forkJoin([statementsLoaded, snapshotLabelLoaded]).pipe(
      tap((results) => {
        const statementData = results[0]
        const status = statementData.results.data[0]
        let snapshotLabel: string|undefined
        if (results[1]) {
          snapshotLabel = results[1]
        }
        // Party definition.
        this.systemName = statementData.system.fname
        this.organisationName = statementData.organisation.fname
        this.communityIdOfStatement = statementData.organisation.community
        if (this.dataService.isSystemAdmin && this.communityIdOfStatement == Constants.DEFAULT_COMMUNITY_ID) {
          const communityIdForActor = this.route.snapshot.data[Constants.NAVIGATION_DATA.IMPLICIT_COMMUNITY_ID] as number|undefined
          if (communityIdForActor != undefined) {
            this.communityIdOfStatement = communityIdForActor
          }
        }
        // Statement definition.
        this.prepareStatement(statementData.statement)
        this.statement = statementData.statement
        this.routingService.conformanceStatementBreadcrumbs(this.organisationId, this.systemId, this.actorId, this.communityId, this.breadcrumbLabel(), this.organisationName, this.systemName, this.snapshotId, snapshotLabel)
        // IDs.
        this.domainId = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN)!.id
        this.specId = this.findByType([this.statement]!, Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION)!.id
        this.prepareNavigationConfig()
        this.hasBadge = status.summary.hasBadge
        this.canEditOrganisationConfiguration = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && (this.dataService.community!.allowPostTestOrganisationUpdates || !this.hasTests))
        this.canEditSystemConfiguration = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && (this.dataService.community!.allowPostTestSystemUpdates || !this.hasTests))
        this.canEditStatementConfiguration = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && (this.dataService.community!.allowPostTestStatementUpdates || !this.hasTests))
        // Test results.
        this.processTestResults(status)
        this.unfilteredTestCaseCount = statementData.results.count
        this.unfilteredTestSuiteCount = status.testSuiteCount
        this.pagingControls?.updateStatus(1, statementData.results.count)
      }),
      finalize(() => {
        this.updatingTests = false
        this.loadingTests.status = Constants.STATUS.FINISHED
      }),
      share()
    )
    obs$.subscribe()
    return obs$
  }

  private processTestResults(status: ConformanceStatus) {
    this.hasDisabledTests = status.summary.hasDisabled
    this.hasOptionalTests = status.summary.completedOptional > 0 || status.summary.failedOptional > 0 || status.summary.undefinedOptional > 0
    for (let testSuite of status.testSuites) {
      testSuite.testCaseGroupMap = this.dataService.toTestCaseGroupMap(testSuite.testCaseGroups)
    }
    this.displayedTestSuites = status.testSuites
    this.displayedTestSuites.forEach(testSuite => {
      testSuite.expanded = true
    })
    this.statusCounters = {
      completed: status.summary.completed,
      failed: status.summary.failed,
      other: status.summary.undefined,
      completedOptional: status.summary.completedOptional,
      failedOptional: status.summary.failedOptional,
      otherOptional: status.summary.undefinedOptional,
      completedToConsider: status.summary.completedToConsider,
      failedToConsider: status.summary.failedToConsider,
      otherToConsider: status.summary.undefinedToConsider
    }
    this.lastUpdate = status.summary.updateTime
    if (this.lastUpdate) {
      this.hasTests = true
    }
    this.conformanceStatus = status.summary.result
    this.allTestsSuccessful = this.conformanceStatus == Constants.TEST_CASE_RESULT.SUCCESS
    this.updateTestCaseFilterOptions()
  }

  private updateTestCaseFilterOptions() {
    this.testCaseFilterOptions = {
      initialState: {
        showOptional: true,
        showDisabled: false,
        showSuccessful: true,
        showFailed: true,
        showIncomplete: true
      },
      showOptional: this.hasOptionalTests,
      showDisabled: this.hasDisabledTests
    }
    this.testCaseResultFilter?.refreshOptions(this.testCaseFilterOptions, true)
  }

  loadTab(tabIndex: number) {
    if (tabIndex == Constants.TAB.CONFORMANCE_STATEMENT.CONFIGURATION) {
      this.showConfigurationTab()
    }
  }

  testSuitePageNavigation(pagingInfo: PagingEvent) {
    this.loadConformanceTestsInternal(pagingInfo)
  }

  private loadConformanceTests() {
    return this.loadConformanceTestsInternal({ targetPage: 1, targetPageSize: Constants.TABLE_PAGE_SIZE })
  }

  private loadConformanceTestsInternal(pagingInfo: PagingEvent): Observable<any> {
    this.updatingTests = true
    const obs$ = this.conformanceService.getConformanceStatementTests(this.systemId, this.actorId, this.snapshotId, this.testCaseSearchCriteria, pagingInfo.targetPage, pagingInfo.targetPageSize).pipe(
      tap((data) => {
        this.processTestResults(data.data[0])
        setTimeout(() => {
          this.testSuiteDisplayComponents?.forEach((component) => {
            component.refresh()
          })
          if (this.statusCounters) {
            this.refreshCounters.emit(this.statusCounters)
          }
          this.pagingControls?.updateStatus(pagingInfo.targetPage, data.count)
        })
      }),
      finalize(() => {
        this.updatingTests = false
      }),
      share()
    )
    obs$.subscribe()
    return obs$
  }

  private prepareNavigationConfig() {
    this.navigationConfig = {
      systemId: this.systemId,
      organisationId: this.organisationId,
      communityId: this.communityIdOfStatement,
      actorId: this.actorId,
      specificationId: this.specId,
      domainId: this.domainId,
      showStatement: false
    }
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

  resultFilterUpdated(choices: TestCaseFilterState) {
    this.testCaseFilterState = choices
    this.testCaseSearchCriteria.succeeded = choices.showSuccessful
    this.testCaseSearchCriteria.failed = choices.showFailed
    this.testCaseSearchCriteria.incomplete = choices.showIncomplete
    this.testCaseSearchCriteria.optional = choices.showOptional
    this.testCaseSearchCriteria.disabled = choices.showDisabled
    this.applySearchFilters()
  }

  applySearchFilters() {
    this.loadConformanceTests()
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
        this.popupService.success('Started test session.')
      } else {
        this.popupService.success('Started '+testCaseIds.length+' test sessions.')
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
        const searchCriteria = {...this.testCaseSearchCriteria}
        searchCriteria.disabled = false
        searchCriteria.testSuiteId = testSuite.id
        this.conformanceService.getConformanceStatementTests(this.systemId, this.actorId, this.snapshotId, searchCriteria, 1, 1000000)
          .subscribe((data) => {
            const testsToExecute: ConformanceTestCase[] = []
            data.data[0].testSuites.forEach(testSuite => {
              testsToExecute.push(...testSuite.testCases)
            })
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
          })
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
    const orgParams = this.canEditOrganisationConfiguration?this.organisationProperties:undefined
    const sysParams = this.canEditSystemConfiguration?this.systemProperties:undefined
    const stmParams = this.canEditStatementConfiguration?this.statementProperties:undefined
    this.conformanceService.updateStatementConfiguration(this.systemId, this.actorId, orgParams, sysParams, stmParams)
    .subscribe((error) => {
      if (!error) {
        this.popupService.success('Configuration updated.')
      }
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
    let testCaseCount = 0
    if (this.statusCounters) {
      testCaseCount = this.statusCounters.completed + this.statusCounters.failed + this.statusCounters.other
    }
    this.reportSupportService.handleConformanceStatementReport(this.communityIdOfStatement, this.actorId, this.systemId, this.snapshotId, format, false, testCaseCount)
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

  protected calculateWrapping() {
    if (this.statusInfoContainer && this.resultsContainer) {
      this.resultsWrapped = this.statusInfoContainer.nativeElement.getBoundingClientRect().top != this.resultsContainer.nativeElement.getBoundingClientRect().top
    }
  }

  refresh() {
    this.refreshPending = true
    this.loadConformanceTests().subscribe(() => {
        this.refreshPending = false
    })
  }

  private loadStatementTestSuites(): Observable<TestSuiteMinimalInfo[]> {
    return this.conformanceService.getConformanceStatementTestSuitesForFiltering(this.systemId, this.actorId, this.snapshotId)
  }

  selectedTestSuiteChanged(event: FilterUpdate<TestSuiteMinimalInfo>) {
    if (event.values.active.length == 0) {
      this.testCaseSearchCriteria.testSuiteId = undefined
    } else {
      this.testCaseSearchCriteria.testSuiteId = event.values.active[0].id
    }
    this.applySearchFilters()
  }

  onTestCaseOptionsOpened(testCase: ConformanceTestCase) {
    this.testSuiteDisplayComponents?.forEach((component) => {
      component.closeOptions(testCase)
    })
  }

}
