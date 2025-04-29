import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { FilterState } from 'src/app/types/filter-state';
import { Observable, forkJoin, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators'
import { map, remove, filter } from 'lodash';
import { BsDatepickerConfig } from 'ngx-bootstrap/datepicker';
import { formatDate } from '@angular/common';
import { Domain } from 'src/app/types/domain';
import { Specification } from 'src/app/types/specification';
import { Actor } from 'src/app/types/actor';
import { TestCase } from 'src/app/types/test-case';
import { TestSuiteWithTestCases } from 'src/app/types/test-suite-with-test-cases';
import { Community } from 'src/app/types/community';
import { Organisation } from 'src/app/types/organisation.type';
import { System } from 'src/app/types/system';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { SystemParameter } from 'src/app/types/system-parameter';
import { CustomProperty } from '../custom-property-filter/custom-property';
import { MultiSelectConfig } from '../multi-select-filter/multi-select-config';
import { IdLabel } from 'src/app/types/id-label';
import { NumberSet } from 'src/app/types/number-set';
import { ConformanceService } from 'src/app/services/conformance.service';
import { TestSuiteService } from 'src/app/services/test-suite.service';
import { ReportService } from 'src/app/services/report.service';
import { CommunityService } from 'src/app/services/community.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { SystemService } from 'src/app/services/system.service';
import { SpecificationGroup } from 'src/app/types/specification-group';
import { SpecificationService } from 'src/app/services/specification.service';
import { FilterValues } from './filter-values';
import { FilterUpdate } from './filter-update';
import { EntityWithId } from 'src/app/types/entity-with-id';

@Component({
    selector: 'app-test-filter',
    templateUrl: './test-filter.component.html',
    styleUrls: ['./test-filter.component.less'],
    standalone: false
})
export class TestFilterComponent implements OnInit {

  @Input() filterState!: FilterState
  @Input() communityId?: number
  @Input() organisationId?: number
  @Input() embedded = false
  @Input() commands?: EventEmitter<number>
  @Input() initialSessionId?: string

  @Input() loadDomainsFn?: () => Observable<Domain[]>
  @Input() loadSpecificationsFn?: () => Observable<Specification[]>
  @Input() loadSpecificationGroupsFn?: () => Observable<SpecificationGroup[]>
  @Input() loadActorsFn?: () => Observable<Actor[]>
  @Input() loadTestSuitesFn?: () => Observable<TestSuiteWithTestCases[]>
  @Input() loadTestCasesFn?: () => Observable<TestCase[]>
  @Input() loadCommunitiesFn?: () => Observable<Community[]>
  @Input() loadOrganisationsFn?: () => Observable<Organisation[]>
  @Input() loadSystemsFn?: () => Observable<System[]>
  @Input() loadOrganisationPropertiesFn?: (_: number) => Observable<OrganisationParameter[]>
  @Input() loadSystemPropertiesFn?: (_: number) => Observable<SystemParameter[]>

  @Output() onApply = new EventEmitter<any>()

  Constants = Constants
  filterValues: { [key: string]: FilterValues<EntityWithId> } = {}
  organisationProperties: Array<CustomProperty> = []
  systemProperties: Array<{id?: number, value?: string, uuid?: number}> = []
  definedFilters: { [key: string]: boolean } = {}
  showFiltering = false
  sessionId?: string
  uuidCounter = 0
  cachedOrganisationProperties: { [key: string]: OrganisationParameter[] } = {}
  cachedSystemProperties: { [key: string]: SystemParameter[] } = {}
  availableOrganisationProperties: OrganisationParameter[] = []
  availableSystemProperties: SystemParameter[] = []
  filterDropdownSettings: {[key: string]: MultiSelectConfig<EntityWithId>} = {}
  datePickerSettings: Partial<BsDatepickerConfig> = {
    adaptivePosition: true,
    rangeInputFormat: 'DD-MM-YYYY',
    containerClass: 'theme-default'
  }

  startDateModel?: Date[]
  endDateModel?: Date[]
  addingOrganisationProperty = false
  addingSystemProperty = false
  loadingOrganisationProperties = false
  loadingSystemProperties = false
  applicableCommunityId?: number
  names: {[key: string]: string} = {}

  initialised = false
  showOrganisationProperties = false
  showSystemProperties = false
  filterCollapsedFinished = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private testSuiteService: TestSuiteService,
    private reportService: ReportService,
    private communityService: CommunityService,
    private organisationService: OrganisationService,
    private systemService: SystemService,
    private specificationService: SpecificationService
  ) { }

  ngOnInit(): void {
    // Set up filter title names
    this.names[Constants.FILTER_TYPE.ACTOR] = this.dataService.labelActor()
    this.names[Constants.FILTER_TYPE.COMMUNITY] = 'Community'
    this.names[Constants.FILTER_TYPE.DOMAIN] = this.dataService.labelDomain()
    this.names[Constants.FILTER_TYPE.END_TIME] = 'End time'
    this.names[Constants.FILTER_TYPE.ORGANISATION] = this.dataService.labelOrganisation()
    this.names[Constants.FILTER_TYPE.ORGANISATION_PROPERTY] = this.dataService.labelOrganisation() + ' properties'
    this.names[Constants.FILTER_TYPE.RESULT] = 'Result'
    this.names[Constants.FILTER_TYPE.SESSION] = 'Session'
    this.names[Constants.FILTER_TYPE.SPECIFICATION] = this.dataService.labelSpecification()
    this.names[Constants.FILTER_TYPE.SPECIFICATION_GROUP] = this.dataService.labelSpecificationGroup()
    this.names[Constants.FILTER_TYPE.START_TIME] = 'Start time'
    this.names[Constants.FILTER_TYPE.SYSTEM] = this.dataService.labelSystem()
    this.names[Constants.FILTER_TYPE.SYSTEM_PROPERTY] = this.dataService.labelSystem() + ' properties'
    this.names[Constants.FILTER_TYPE.TEST_CASE] = 'Test case'
    this.names[Constants.FILTER_TYPE.TEST_SUITE] = 'Test suite'
    if (this.filterState.names != undefined) {
      for (let filter in this.filterState.names) {
        if (this.filterState.names[filter] != undefined) {
          this.names[filter] = this.filterState.names[filter]
        }
      }
    }
    this.resetApplicableCommunityId()
    this.filterState.filterData = this.currentFilters.bind(this)
    for (let filterType of this.filterState.filters) {
      this.definedFilters[filterType] = true
    }
    this.setupDefaultLoadFunctions()
    this.initialiseIfDefined(Constants.FILTER_TYPE.DOMAIN, { name: Constants.FILTER_TYPE.DOMAIN, textField: 'sname', loader: this.loadDomainsFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.SPECIFICATION, { name: Constants.FILTER_TYPE.SPECIFICATION, textField: 'sname', loader: this.loadSpecificationsFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.SPECIFICATION_GROUP, { name: Constants.FILTER_TYPE.SPECIFICATION_GROUP, textField: 'sname', loader: this.loadSpecificationGroupsFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.ACTOR, { name: Constants.FILTER_TYPE.ACTOR, textField: 'name', loader: this.loadActorsFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.TEST_SUITE, { name: Constants.FILTER_TYPE.TEST_SUITE, textField: 'sname', loader: this.loadTestSuitesFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.TEST_CASE, { name: Constants.FILTER_TYPE.TEST_CASE, textField: 'sname', loader: this.loadTestCasesFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.COMMUNITY, { name: Constants.FILTER_TYPE.COMMUNITY, textField: 'sname', loader: this.loadCommunitiesFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.ORGANISATION, { name: Constants.FILTER_TYPE.ORGANISATION, textField: 'sname', loader: this.loadOrganisationsFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.SYSTEM, { name: Constants.FILTER_TYPE.SYSTEM, textField: 'sname', loader: this.loadSystemsFn, clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() })
    this.initialiseIfDefined(Constants.FILTER_TYPE.RESULT, { name: Constants.FILTER_TYPE.RESULT, textField: 'label', loader: this.loadTestResults.bind(this), clearItems: new EventEmitter(), replaceSelectedItems: new EventEmitter() } )
    if (this.commands) {
      this.commands.subscribe((command) => {
        this.handleCommand(command)
      })
    }
    this.filterCollapsedFinished = !this.showFiltering || !this.initialised
    this.sessionId = this.initialSessionId
  }

  private handleCommand(command: number) {
    if (command == Constants.FILTER_COMMAND.TOGGLE) {
      this.clickedHeader()
    } else if (command == Constants.FILTER_COMMAND.REFRESH) {
      this.applyFilters()
    } else if (command == Constants.FILTER_COMMAND.CLEAR) {
      this.clearFilters()
    }
  }

  private setupDefaultLoadFunctions() {
    if (this.filterDefined(Constants.FILTER_TYPE.DOMAIN) && this.loadDomainsFn == undefined) {
      this.loadDomainsFn = (() => {
        return this.conformanceService.getDomains()
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION) && this.loadSpecificationsFn == undefined) {
      this.loadSpecificationsFn = (() => {
        if (this.dataService.isSystemAdmin || this.dataService.community!.domainId == undefined) {
          return this.conformanceService.getSpecificationsWithIds(undefined, this.filterValue(Constants.FILTER_TYPE.DOMAIN), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP))
        } else {
          return this.conformanceService.getSpecificationsWithIds(undefined, [this.dataService.community!.domainId], this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP))
        }
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION_GROUP) && this.loadSpecificationGroupsFn == undefined) {
      this.loadSpecificationGroupsFn = (() => {
        if (this.dataService.isSystemAdmin || this.dataService.community!.domainId == undefined) {
          return this.specificationService.getSpecificationGroupsOfDomains(this.filterValue(Constants.FILTER_TYPE.DOMAIN))
        } else {
          return this.specificationService.getSpecificationGroups(this.dataService.community!.domainId)
        }
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.ACTOR) && this.loadActorsFn == undefined) {
      this.loadActorsFn = (() => {
        if (this.dataService.isSystemAdmin || this.dataService.community!.domainId == undefined) {
          return this.conformanceService.searchActors(this.filterValue(Constants.FILTER_TYPE.DOMAIN), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP))
        } else {
          return this.conformanceService.searchActorsInDomain(this.dataService.community!.domainId, this.filterValue(Constants.FILTER_TYPE.SPECIFICATION), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP))
        }
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.TEST_SUITE) && this.loadTestSuitesFn == undefined) {
      this.loadTestSuitesFn = (() => {
        if (this.dataService.isSystemAdmin || this.dataService.community!.domainId == undefined) {
          return this.testSuiteService.searchTestSuites(this.filterValue(Constants.FILTER_TYPE.DOMAIN), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP), this.filterValue(Constants.FILTER_TYPE.ACTOR))
        } else {
          return this.testSuiteService.searchTestSuitesInDomain(this.dataService.community!.domainId, this.filterValue(Constants.FILTER_TYPE.SPECIFICATION), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP), this.filterValue(Constants.FILTER_TYPE.ACTOR))
        }

      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.TEST_CASE) && this.loadTestCasesFn == undefined) {
      this.loadTestCasesFn = (() => {
        if (this.dataService.isSystemAdmin || this.dataService.community!.domainId == undefined) {
          return this.reportService.searchTestCases(this.filterValue(Constants.FILTER_TYPE.DOMAIN), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP), this.filterValue(Constants.FILTER_TYPE.ACTOR), this.filterValue(Constants.FILTER_TYPE.TEST_SUITE))
        } else {
          return this.reportService.searchTestCasesInDomain(this.dataService.community!.domainId, this.filterValue(Constants.FILTER_TYPE.SPECIFICATION), this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP), this.filterValue(Constants.FILTER_TYPE.ACTOR), this.filterValue(Constants.FILTER_TYPE.TEST_SUITE))
        }
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.COMMUNITY) && this.loadCommunitiesFn == undefined) {
      this.loadCommunitiesFn = (() => {
        return this.communityService.getCommunities()
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.ORGANISATION) && this.loadOrganisationsFn == undefined) {
      this.loadOrganisationsFn = (() => {
        if (this.dataService.isSystemAdmin) {
          return this.organisationService.searchOrganizations(this.filterValue(Constants.FILTER_TYPE.COMMUNITY))
        } else {
          return this.organisationService.getOrganisationsByCommunity(this.dataService.community!.id)
        }
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SYSTEM) && this.loadSystemsFn == undefined) {
      this.loadSystemsFn = (() => {
        if (this.organisationId != undefined) {
          return this.systemService.getSystemsByOrganisation(this.organisationId)
        } else {
          if (this.dataService.isSystemAdmin) {
            return this.systemService.searchSystems(this.filterValue(Constants.FILTER_TYPE.COMMUNITY), this.filterValue(Constants.FILTER_TYPE.ORGANISATION))
          } else {
            return this.systemService.searchSystemsInCommunity(this.dataService.community!.id, this.filterValue(Constants.FILTER_TYPE.ORGANISATION))
          }
        }
      }).bind(this)
    }
    // Custom properties
    const onlyPublicProperties = !this.dataService.isSystemAdmin && !this.dataService.isCommunityAdmin
    if (this.filterDefined(Constants.FILTER_TYPE.ORGANISATION_PROPERTY) && this.loadOrganisationPropertiesFn == undefined) {
      this.loadOrganisationPropertiesFn = (() => {
        return this.communityService.getOrganisationParameters(this.applicableCommunityId!, true, onlyPublicProperties)
      }).bind(this)
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SYSTEM_PROPERTY) && this.loadSystemPropertiesFn == undefined) {
      this.loadSystemPropertiesFn = (() => {
        return this.communityService.getSystemParameters(this.applicableCommunityId!, true, onlyPublicProperties)
      }).bind(this)
    }
  }

  private getRemainingFilterValues<T extends EntityWithId>(currentValues: FilterValues<T>, filterFunctionActive: (item: T) => boolean, filterFunctionOther: (item: T) => boolean): FilterValues<T> {
    if (currentValues) {
      // Active downstream items that match parent active items
      const active = filter(currentValues.active, (itemToCheck) => { return filterFunctionActive(itemToCheck) })
      // Inactive downstream items that match parent active items
      const newlyActive = filter(currentValues.other, (itemToCheck) => { return filterFunctionActive(itemToCheck) })
      // Inactive downstream items that match parent inactive items
      const inactive = filter(currentValues.other, (itemToCheck) => { return filterFunctionOther(itemToCheck) })
      // Active downstream items that match parent inactive items
      const newlyInactive = filter(currentValues.active, (itemToCheck) => { return filterFunctionOther(itemToCheck) })
      return { active: active.concat(newlyActive), other: inactive.concat(newlyInactive) }
    } else {
      return { active: [], other: [] }
    }
  }

  private squashFilterValues<T extends EntityWithId>(values: FilterValues<T>) {
    return values.active.concat(values.other)
  }

  domainsChanged(update: FilterUpdate<Domain>) {
    this.filterValues[Constants.FILTER_TYPE.DOMAIN] = update.values
    if (update.values.active.length > 0 && (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION) || this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION_GROUP))) {
      const ids = this.dataService.asIdSet(update.values.active)
      const otherIds = this.dataService.asIdSet(update.values.other)
      if (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION_GROUP)) {
        const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.SPECIFICATION_GROUP] as FilterValues<SpecificationGroup>, (s) => { return ids[s.domain] }, (s) => { return otherIds[s.domain] })
        this.filterDropdownSettings[Constants.FILTER_TYPE.SPECIFICATION_GROUP].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
      }
      if (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION)) {
        const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.SPECIFICATION] as FilterValues<Specification>, (s) => { return ids[s.domain] }, (s) => { return otherIds[s.domain] })
        this.filterDropdownSettings[Constants.FILTER_TYPE.SPECIFICATION].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
      }
    }
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  specificationGroupsChanged(update: FilterUpdate<SpecificationGroup>) {
    this.filterValues[Constants.FILTER_TYPE.SPECIFICATION_GROUP] = update.values
    if ((update.values.active.length > 0 || update.values.other.length > 0) && this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION) && update.applyFilters) {
      const ids = this.dataService.asIdSet(update.values.active)
      const otherIds = this.dataService.asIdSet(update.values.other)
      const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.SPECIFICATION] as FilterValues<Specification>, (s) => { return s.group == undefined || ids[s.group] }, (s) => { return s.group == undefined || otherIds[s.group] })
      this.filterDropdownSettings[Constants.FILTER_TYPE.SPECIFICATION].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
    }
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  specificationsChanged(update: FilterUpdate<Specification>) {
    this.filterValues[Constants.FILTER_TYPE.SPECIFICATION] = update.values
    if ((update.values.active.length > 0 || update.values.other.length > 0) && (this.filterDefined(Constants.FILTER_TYPE.ACTOR) || this.filterDefined(Constants.FILTER_TYPE.TEST_SUITE))) {
      const ids = this.dataService.asIdSet(update.values.active)
      const otherIds = this.dataService.asIdSet(update.values.other)
      if (this.filterDefined(Constants.FILTER_TYPE.ACTOR)) {
        const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.ACTOR] as FilterValues<Actor>, (a) => { return ids[a.specification] }, (a) => { return otherIds[a.specification] })
        this.filterDropdownSettings[Constants.FILTER_TYPE.ACTOR].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
      }
      if (this.filterDefined(Constants.FILTER_TYPE.TEST_SUITE)) {
        const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.TEST_SUITE] as FilterValues<TestSuiteWithTestCases>,
          // One of the test suite's specifications must be in the set of selected IDs.
          (ts) => {
            for (let tsSpecification of ts.specifications!) {
              if (ids[tsSpecification]) {
                return true
              }
            }
            return false
          },
          (ts) => {
            for (let tsSpecification of ts.specifications!) {
              if (otherIds[tsSpecification]) {
                return true
              }
            }
            return false
          }
        )
        this.filterDropdownSettings[Constants.FILTER_TYPE.TEST_SUITE].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
      }
    }
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  actorsChanged(update: FilterUpdate<Actor>) {
    this.filterValues[Constants.FILTER_TYPE.ACTOR] = update.values
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  testSuitesChanged(update: FilterUpdate<TestSuiteWithTestCases>) {
    this.filterValues[Constants.FILTER_TYPE.TEST_SUITE] = update.values
    if ((update.values.active.length > 0 || update.values.other.length > 0) && this.filterDefined(Constants.FILTER_TYPE.TEST_CASE)) {
      const validTestCaseIds: NumberSet = {}
      for (let testSuite of update.values.active) {
        for (let testCase of testSuite.testCases) {
          validTestCaseIds[testCase.id] = true
        }
      }
      const validOtherTestCaseIds: NumberSet = {}
      for (let testSuite of update.values.other) {
        for (let testCase of testSuite.testCases) {
          validOtherTestCaseIds[testCase.id] = true
        }
      }
      const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.TEST_CASE] as FilterValues<TestCase>, (tc) => { return validTestCaseIds[tc.id] }, (tc) => { return validOtherTestCaseIds[tc.id] })
      this.filterDropdownSettings[Constants.FILTER_TYPE.TEST_CASE].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
    }
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  testCasesChanged(update: FilterUpdate<TestCase>) {
    this.filterValues[Constants.FILTER_TYPE.TEST_CASE] = update.values
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  communitiesChanged(update: FilterUpdate<Community>) {
    this.filterValues[Constants.FILTER_TYPE.COMMUNITY] = update.values
    if ((update.values.active.length > 0 || update.values.other.length > 0) && this.filterDefined(Constants.FILTER_TYPE.ORGANISATION)) {
      const ids = this.dataService.asIdSet(update.values.active)
      const otherIds = this.dataService.asIdSet(update.values.other)
      const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.ORGANISATION] as FilterValues<Organisation>, (o) => { return ids[o.community] }, (o) => { return otherIds[o.community] })
      this.filterDropdownSettings[Constants.FILTER_TYPE.ORGANISATION].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
    }
    // Custom properties
    this.organisationProperties = []
    this.systemProperties = []
    if (update.values.active.length == 1) {
      this.applicableCommunityId = update.values.active[0].id
    } else {
      this.applicableCommunityId = undefined
    }
    this.resetCustomProperties().subscribe(() => {
      this.initialised = true
    })
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  organisationsChanged(update: FilterUpdate<Organisation>) {
    this.filterValues[Constants.FILTER_TYPE.ORGANISATION] = update.values
    if ((update.values.active.length > 0 || update.values.other.length > 0) && this.filterDefined(Constants.FILTER_TYPE.SYSTEM)) {
      const ids = this.dataService.asIdSet(update.values.active)
      const otherIds = this.dataService.asIdSet(update.values.other)
      const remaining = this.getRemainingFilterValues(this.filterValues[Constants.FILTER_TYPE.SYSTEM] as FilterValues<System>, (s) => { return ids[s.owner] }, (s) => { return otherIds[s.owner] })
      this.filterDropdownSettings[Constants.FILTER_TYPE.SYSTEM].replaceSelectedItems!.emit(this.squashFilterValues(remaining))
    }
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  systemsChanged(update: FilterUpdate<System>) {
    this.filterValues[Constants.FILTER_TYPE.SYSTEM] = update.values
    if (update.applyFilters) {
      this.applyFilters()
    }
  }

  resultsChanged(update: FilterUpdate<IdLabel>) {
    this.filterValues[Constants.FILTER_TYPE.RESULT] = update.values
    this.applyFilters()
  }

  private resetApplicableCommunityId() {
    if (this.communityId != undefined) {
      this.applicableCommunityId = this.communityId
    } else {
      this.applicableCommunityId = undefined
    }
  }

  private initialiseIfDefined(filterType: string, config: MultiSelectConfig<EntityWithId>) {
    if (this.filterDefined(filterType)) {
      this.filterDropdownSettings[filterType] = config
    }
  }

  filterDefined(filterType: string) {
    return this.definedFilters[filterType] !== undefined
  }

  filterValue(filterType: string) {
    let values: number[]|undefined
    if (this.filterDefined(filterType) && this.filterValues[filterType] && this.filterValues[filterType].active) {
      values = map(this.filterValues[filterType].active, (item) => {return item.id})
    }
    return values
  }

  private toDateStart(date: Date): Date {
    const newDate = new Date(date.getTime())
    newDate.setHours(0, 0, 0, 0)
    return newDate
  }

  private toDateEnd(date: Date) {
    const newDate = new Date(date.getTime())
    newDate.setHours(23, 59, 59, 999)
    return newDate
  }

  currentFilters() {
    const filters: { [key: string]: any } = {}
    filters[Constants.FILTER_TYPE.DOMAIN] = this.filterValue(Constants.FILTER_TYPE.DOMAIN)
    filters[Constants.FILTER_TYPE.SPECIFICATION] = this.filterValue(Constants.FILTER_TYPE.SPECIFICATION)
    filters[Constants.FILTER_TYPE.SPECIFICATION_GROUP] = this.filterValue(Constants.FILTER_TYPE.SPECIFICATION_GROUP)
    filters[Constants.FILTER_TYPE.ACTOR] = this.filterValue(Constants.FILTER_TYPE.ACTOR)
    filters[Constants.FILTER_TYPE.TEST_SUITE] = this.filterValue(Constants.FILTER_TYPE.TEST_SUITE)
    filters[Constants.FILTER_TYPE.TEST_CASE] = this.filterValue(Constants.FILTER_TYPE.TEST_CASE)
    filters[Constants.FILTER_TYPE.COMMUNITY] = this.filterValue(Constants.FILTER_TYPE.COMMUNITY)
    filters[Constants.FILTER_TYPE.ORGANISATION] = this.filterValue(Constants.FILTER_TYPE.ORGANISATION)
    filters[Constants.FILTER_TYPE.SYSTEM] = this.filterValue(Constants.FILTER_TYPE.SYSTEM)
    const resultValues = this.filterValue(Constants.FILTER_TYPE.RESULT)
    if (resultValues) {
      filters[Constants.FILTER_TYPE.RESULT] = map(resultValues, (value: number) => {
        if (value == 0) return Constants.TEST_CASE_RESULT.SUCCESS
        else if (value == 1) return Constants.TEST_CASE_RESULT.FAILURE
        else return Constants.TEST_CASE_RESULT.UNDEFINED
      })
    }
    if (this.filterDefined(Constants.FILTER_TYPE.START_TIME)) {
      if (this.startDateModel !== undefined) {
        filters.startTimeBegin = this.toDateStart(this.startDateModel[0])
        filters.startTimeBeginStr = formatDate(filters.startTimeBegin, 'dd-MM-YYYY HH:mm:ss', 'en')
        filters.startTimeEnd = this.toDateEnd(this.startDateModel[1])
        filters.startTimeEndStr = formatDate(filters.startTimeEnd, 'dd-MM-YYYY HH:mm:ss', 'en')
      }
    }
    if (this.filterDefined(Constants.FILTER_TYPE.END_TIME)) {
      if (this.endDateModel !== undefined) {
        filters.endTimeBegin = this.toDateStart(this.endDateModel[0])
        filters.endTimeBeginStr = formatDate(filters.endTimeBegin, 'dd-MM-YYYY HH:mm:ss', 'en')
        filters.endTimeEnd = this.toDateEnd(this.endDateModel[1])
        filters.endTimeEndStr = formatDate(filters.endTimeEnd, 'dd-MM-YYYY HH:mm:ss', 'en')
      }
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SESSION)) {
      filters.sessionId = this.sessionId
    }
    if (this.filterDefined(Constants.FILTER_TYPE.ORGANISATION_PROPERTY)) {
      filters.organisationProperties = []
      for (let p of this.organisationProperties) {
        if (p.id !== undefined && p.value !== undefined) {
          filters.organisationProperties.push({
            id: p.id,
            value: p.value
          })
        }
      }
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SYSTEM_PROPERTY)) {
      filters.systemProperties = []
      for (let p of this.systemProperties) {
        if (p.id !== undefined && p.value !== undefined)
          filters.systemProperties.push({
            id: p.id,
            value: p.value
          })
      }
    }
    return filters
  }

  applyFilters() {
    this.filterState.updatePending = true
    this.onApply.emit(this.currentFilters())
  }

  clearFilter(filterType: string) {
    if (this.filterDefined(filterType)) {
      if (this.filterValues[filterType]) {
        this.filterValues[filterType].active = []
        this.filterValues[filterType].other = []
      }
      this.filterDropdownSettings[filterType].clearItems!.emit()
    }
  }

  clearFilters() {
    this.showFiltering = false
    this.clearFilter(Constants.FILTER_TYPE.DOMAIN)
    this.clearFilter(Constants.FILTER_TYPE.SPECIFICATION)
    this.clearFilter(Constants.FILTER_TYPE.SPECIFICATION_GROUP)
    this.clearFilter(Constants.FILTER_TYPE.ACTOR)
    this.clearFilter(Constants.FILTER_TYPE.TEST_SUITE)
    this.clearFilter(Constants.FILTER_TYPE.TEST_CASE)
    this.clearFilter(Constants.FILTER_TYPE.COMMUNITY)
    this.clearFilter(Constants.FILTER_TYPE.ORGANISATION)
    this.clearFilter(Constants.FILTER_TYPE.SYSTEM)
    this.clearFilter(Constants.FILTER_TYPE.RESULT)
    if (this.filterDefined(Constants.FILTER_TYPE.START_TIME)) {
      this.startDateModel = undefined
    }
    if (this.filterDefined(Constants.FILTER_TYPE.END_TIME)) {
      this.endDateModel = undefined
    }
    this.resetApplicableCommunityId()
    this.organisationProperties = []
    this.systemProperties = []
    this.availableOrganisationProperties = []
    this.availableSystemProperties = []
    this.addingOrganisationProperty = false
    this.addingSystemProperty = false
    this.loadingOrganisationProperties = false
    this.loadingSystemProperties = false
    this.applicableCommunityId
    this.resetFilters()
    this.applyFilters()
  }

  clickedHeader() {
    this.showFiltering = !this.showFiltering
    if (!this.initialised) {
      this.resetCustomProperties().subscribe(() => {
        this.initialised = true
      })
    }
  }

  private resetCustomProperties(): Observable<boolean> {
    if (this.applicableCommunityId != undefined) {
      const obs1 = this.loadOrganisationProperties(this.applicableCommunityId)
      const obs2 = this.loadSystemProperties(this.applicableCommunityId)
      return forkJoin([obs1, obs2]).pipe(
        mergeMap((data) => {
          this.showOrganisationProperties = data[0].length > 0
          this.showSystemProperties = data[1].length > 0
          return of(true)
        })
      )
    } else {
      this.showOrganisationProperties = false
      this.showSystemProperties = false
      return of(false)
    }
  }

  applyTimeFiltering() {
    setTimeout(() => {
      this.applyFilters()
    })
  }

  resetFilters() {
    for (let filterType of this.filterState.filters) {
      const filterConfig = this.filterDropdownSettings[filterType]
      if (filterConfig && filterConfig.clearItems) {
        filterConfig.clearItems.emit()
      }
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SESSION)) {
      this.sessionId = undefined
    }
    this.organisationProperties = []
    this.systemProperties = []
    this.startDateModel = undefined
    this.endDateModel = undefined
  }

  private loadTestResults(): Observable<IdLabel[]> {
    return of([
      { id: 0, label: "Success" },
      { id: 1, label: "Failure" },
      { id: 2, label: "Incomplete" }
    ])
  }

  addOrganisationProperty() {
    if (this.applicableCommunityId !== undefined) {
      this.addingOrganisationProperty = true
      this.loadOrganisationProperties(this.applicableCommunityId)
      .subscribe((properties: OrganisationParameter[]) => {
        this.availableOrganisationProperties = properties
        if (properties !== undefined && properties.length > 0) {
          this.uuidCounter = this.uuidCounter + 1
          this.organisationProperties.push({
            uuid: this.uuidCounter
          })
        } else {
          this.addingOrganisationProperty = false
        }
      })
    }
  }

  addSystemProperty() {
    if (this.applicableCommunityId !== undefined) {
      this.addingSystemProperty = true
      this.loadSystemProperties(this.applicableCommunityId)
      .subscribe((properties) => {
        this.availableSystemProperties = properties
        if (properties !== undefined && properties.length > 0) {
          this.uuidCounter = this.uuidCounter + 1
          this.systemProperties.push({
            uuid: this.uuidCounter
          })
        } else {
          this.addingSystemProperty = false
        }
      })
    }
  }

  applyOrganisationProperty() {
    this.addingOrganisationProperty = false
    this.applyFilters()
  }

  applySystemProperty() {
    this.addingSystemProperty = false
    this.applyFilters()
  }

  clearOrganisationProperty(propertyDefinition: CustomProperty) {
    remove(this.organisationProperties, (prop) => prop.uuid == propertyDefinition.uuid)
    this.applyFilters()
  }

  clearSystemProperty(propertyDefinition: CustomProperty) {
    remove(this.systemProperties, (prop) => prop.uuid == propertyDefinition.uuid)
    this.applyFilters()
  }

  cancelOrganisationProperty() {
    this.addingOrganisationProperty = false
    return this.organisationProperties.pop()
  }

  cancelSystemProperty() {
    this.addingSystemProperty = false
    return this.systemProperties.pop()
  }

  loadOrganisationProperties(communityId: number): Observable<OrganisationParameter[]> {
    const cacheKey = 'org_'+communityId
    const cachedData = this.cachedOrganisationProperties[cacheKey]
    if (cachedData !== undefined) {
      return of(cachedData)
    } else {
      this.loadingOrganisationProperties = true
      const result = this.loadOrganisationPropertiesFn!(communityId)
      return result.pipe(
        mergeMap((data) => {
          for (let p of data) {
            if (p.allowedValues !== undefined) {
              p.presetValues = JSON.parse(p.allowedValues)
            }
          }
          this.cachedOrganisationProperties![cacheKey] = data
          this.loadingOrganisationProperties = false
          return of(data)
        })
      )
    }
  }

  loadSystemProperties(communityId: number): Observable<SystemParameter[]> {
    const cacheKey = 'sys_'+communityId
    const cachedData = this.cachedSystemProperties[cacheKey]
    if (cachedData !== undefined) {
      return of(cachedData)
    } else {
      this.loadingSystemProperties = true
      const result = this.loadSystemPropertiesFn!(communityId)
      return result.pipe(
        mergeMap((data) => {
          for (let p of data) {
            if (p.allowedValues !== undefined) {
              p.presetValues = JSON.parse(p.allowedValues)
            }
          }
          this.cachedSystemProperties![cacheKey] = data
          this.loadingSystemProperties = false
          return of(data)
        })
      )
    }
  }

  createDropdownSettings(idField: string, labelField: string) {
    return {
      idField: idField,
      textField: labelField,
      searchPlaceholderText: 'Search...',
      itemsShowLimit: 1,
      allowSearchFilter: true,
      enableCheckAll: true,
      selectAllText: 'Select all',
      unSelectAllText: 'Clear all',
    }
  }

  clearStartRange() {
    this.startDateModel = undefined
  }

  clearEndRange() {
    this.endDateModel = undefined
  }

  toggleFilterCollapsedFinished(value: boolean) {
    setTimeout(() => {
      this.filterCollapsedFinished = value
    }, 1)
  }

}
