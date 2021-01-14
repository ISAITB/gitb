import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { FilterState } from 'src/app/types/filter-state';
import { Observable, forkJoin, of, EMPTY } from 'rxjs';
import { catchError, map as oMap, mergeMap, share } from 'rxjs/operators'
import { map, find, remove, clone, filter, includes } from 'lodash';
import { IDropdownSettings } from 'ng-multiselect-dropdown';
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
import { ObjectWithId } from './object-with-id';
import { CustomProperty } from '../custom-property-filter/custom-property';

@Component({
  selector: 'app-test-filter',
  templateUrl: './test-filter.component.html',
  styles: [
  ]
})
export class TestFilterComponent implements OnInit {

  @Input() filterState!: FilterState
  @Input() communityId?: number

  @Input() loadDomainsFn?: () => Observable<Domain[]>
  @Input() loadSpecificationsFn?: () => Observable<Specification[]>
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
  filtering: { [key: string]: {all: any[], filter: any[], selection: ObjectWithId[] } } = {}
  loadPromises: Observable<any>[] = []
  organisationProperties: Array<CustomProperty> = []
  systemProperties: Array<{id?: number, value?: string, uuid?: number}> = []
  definedFilters: { [key: string]: boolean } = {}
  topColumnCount = 0
  colWidth!: string
  colStyle!: { width: string }
  enableFiltering = false
  showFiltering = false
  sessionState?: { id?: string, readonly: boolean }
  uuidCounter = 0
  cachedOrganisationProperties: { [key: string]: OrganisationParameter[] } = {}
  cachedSystemProperties: { [key: string]: SystemParameter[] } = {}
  availableOrganisationProperties: OrganisationParameter[] = []
  availableSystemProperties: SystemParameter[] = []
  filterDropdownSettings: {[key: string]: IDropdownSettings} = {}
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
  filterDataLoaded = false

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

  private resetApplicableCommunityId() {
    if (this.communityId != undefined) {
      this.applicableCommunityId = this.communityId
    } else {
      this.applicableCommunityId = undefined
    }
  }

  private loadFilterData() {
    this.resetApplicableCommunityId()
    this.filterDropdownSettings[Constants.FILTER_TYPE.DOMAIN] = this.createDropdownSettings('id', 'sname')
    this.filterDropdownSettings[Constants.FILTER_TYPE.SPECIFICATION] = this.createDropdownSettings('id', 'sname')
    this.filterDropdownSettings[Constants.FILTER_TYPE.ACTOR] = this.createDropdownSettings('id', 'actorId')
    this.filterDropdownSettings[Constants.FILTER_TYPE.TEST_SUITE] = this.createDropdownSettings('id', 'sname')
    this.filterDropdownSettings[Constants.FILTER_TYPE.TEST_CASE] = this.createDropdownSettings('id', 'sname')
    this.filterDropdownSettings[Constants.FILTER_TYPE.COMMUNITY] = this.createDropdownSettings('id', 'sname')
    this.filterDropdownSettings[Constants.FILTER_TYPE.ORGANISATION] = this.createDropdownSettings('id', 'sname')
    this.filterDropdownSettings[Constants.FILTER_TYPE.SYSTEM] = this.createDropdownSettings('id', 'sname')
    this.filterDropdownSettings[Constants.FILTER_TYPE.RESULT] = this.createDropdownSettings('id', 'id')

    this.filterState.filterData = this.currentFilters.bind(this)
    for (let filterType of this.filterState.filters) {
      this.definedFilters[filterType] = true
    }
    if (this.filterDefined(Constants.FILTER_TYPE.DOMAIN)) this.topColumnCount += 1 
    if (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION)) this.topColumnCount += 1
    if (this.filterDefined(Constants.FILTER_TYPE.ACTOR)) this.topColumnCount += 1
    if (this.filterDefined(Constants.FILTER_TYPE.TEST_SUITE)) this.topColumnCount += 1
    if (this.filterDefined(Constants.FILTER_TYPE.TEST_CASE)) this.topColumnCount += 1
    if (this.filterDefined(Constants.FILTER_TYPE.COMMUNITY)) this.topColumnCount += 1
    if (this.filterDefined(Constants.FILTER_TYPE.ORGANISATION)) this.topColumnCount += 1
    if (this.filterDefined(Constants.FILTER_TYPE.SYSTEM)) this.topColumnCount += 1
    this.colWidth = (100 / this.topColumnCount) + '%'
    this.colStyle = {
      'width': this.colWidth
    }
    this.setupFilter<Domain>(Constants.FILTER_TYPE.DOMAIN, this.loadDomainsFn)
    this.setupFilter<Specification>(Constants.FILTER_TYPE.SPECIFICATION, this.loadSpecificationsFn)
    this.setupFilter<Actor>(Constants.FILTER_TYPE.ACTOR, this.loadActorsFn)
    this.setupFilter<TestSuiteWithTestCases>(Constants.FILTER_TYPE.TEST_SUITE, this.loadTestSuitesFn)
    this.setupFilter<TestCase>(Constants.FILTER_TYPE.TEST_CASE, this.loadTestCasesFn)
    this.setupFilter<Community>(Constants.FILTER_TYPE.COMMUNITY, this.loadCommunitiesFn)
    this.setupFilter<Organisation>(Constants.FILTER_TYPE.ORGANISATION, this.loadOrganisationsFn)
    this.setupFilter<System>(Constants.FILTER_TYPE.SYSTEM, this.loadSystemsFn)
    this.filtering[Constants.FILTER_TYPE.RESULT] = {
      all: this.getAllTestResults(),
      filter: this.getAllTestResults(),
      selection: []
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SESSION)) {
      this.sessionState = {
        readonly: true
      }
    }
    forkJoin(this.loadPromises).subscribe(() => {
      this.filterDataLoaded = true
      this.resetFilters()
      this.applyFilters()
    })
  }

  filterDefined(filterType: string) {
    return this.definedFilters[filterType] !== undefined
  }

  setupFilter<T>(filterType: string, loadFn?: () => Observable<T[]>) {
    this.filtering[filterType] = {
      all: [],
      filter: [],
      selection: []
    }
    if (this.filterDefined(filterType)) {
      const sharedObservable = loadFn!().pipe(
        oMap((data:T[]) => {
          this.filtering[filterType].all = data
        }),
        catchError(() => {
          this.enableFiltering = false
          this.showFiltering = false
          return EMPTY
        }),
        share() // We return a shared observable here otherwise the requests are repeated
      )
      this.loadPromises.push(sharedObservable)
    }
  }

  filterValue(filterType: string) {
    let values: number[]|undefined
    if (this.filterDefined(filterType)) {
      values = map(this.filtering[filterType].selection, (s) => {return s.id})
    }
    return values
  }

  filterItemTicked(filterType: string) {
    if (filterType == Constants.FILTER_TYPE.DOMAIN) {
      this.setSpecificationFilter(<Domain[]>this.filtering[Constants.FILTER_TYPE.DOMAIN].selection, <Domain[]>this.filtering[Constants.FILTER_TYPE.DOMAIN].filter, true)
    }
    if (filterType == Constants.FILTER_TYPE.DOMAIN || filterType == Constants.FILTER_TYPE.SPECIFICATION) {
      this.setActorFilter(<Specification[]>this.filtering[Constants.FILTER_TYPE.SPECIFICATION].selection, <Specification[]>this.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, true)
      this.setTestSuiteFilter(<Specification[]>this.filtering[Constants.FILTER_TYPE.SPECIFICATION].selection, <Specification[]>this.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, true)
    }
    if (filterType == Constants.FILTER_TYPE.DOMAIN || filterType == Constants.FILTER_TYPE.SPECIFICATION || filterType == Constants.FILTER_TYPE.ACTOR || filterType == Constants.FILTER_TYPE.TEST_SUITE) {
      this.setTestCaseFilter(<TestSuiteWithTestCases[]>this.filtering[Constants.FILTER_TYPE.TEST_SUITE].selection, <TestSuiteWithTestCases[]>this.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter, true)
    }
    if (filterType == Constants.FILTER_TYPE.COMMUNITY) {
      this.setOrganizationFilter(<Community[]>this.filtering[Constants.FILTER_TYPE.COMMUNITY].selection, <Community[]>this.filtering[Constants.FILTER_TYPE.COMMUNITY].filter, true)
      // Custom properties
      this.organisationProperties = []
      this.systemProperties = []
      if (this.filtering[Constants.FILTER_TYPE.COMMUNITY].selection.length == 1) {
        this.applicableCommunityId = this.filtering[Constants.FILTER_TYPE.COMMUNITY].selection[0].id      
      } else {
        this.applicableCommunityId = undefined
      }
    }
    if (filterType == Constants.FILTER_TYPE.COMMUNITY || filterType == Constants.FILTER_TYPE.ORGANISATION) {
      this.setSystemFilter(this.filtering[Constants.FILTER_TYPE.ORGANISATION].selection, this.filtering[Constants.FILTER_TYPE.ORGANISATION].filter, true)
    }
    this.applyFilters()
  }

  currentFilters() {
    const filters: { [key: string]: any } = {}
    filters[Constants.FILTER_TYPE.DOMAIN] = this.filterValue(Constants.FILTER_TYPE.DOMAIN)
    filters[Constants.FILTER_TYPE.SPECIFICATION] = this.filterValue(Constants.FILTER_TYPE.SPECIFICATION)
    filters[Constants.FILTER_TYPE.ACTOR] = this.filterValue(Constants.FILTER_TYPE.ACTOR)
    filters[Constants.FILTER_TYPE.TEST_SUITE] = this.filterValue(Constants.FILTER_TYPE.TEST_SUITE)
    filters[Constants.FILTER_TYPE.TEST_CASE] = this.filterValue(Constants.FILTER_TYPE.TEST_CASE)
    filters[Constants.FILTER_TYPE.COMMUNITY] = this.filterValue(Constants.FILTER_TYPE.COMMUNITY)
    filters[Constants.FILTER_TYPE.ORGANISATION] = this.filterValue(Constants.FILTER_TYPE.ORGANISATION)
    filters[Constants.FILTER_TYPE.SYSTEM] = this.filterValue(Constants.FILTER_TYPE.SYSTEM)
    filters[Constants.FILTER_TYPE.RESULT] = this.filterValue(Constants.FILTER_TYPE.RESULT)
    if (this.filterDefined(Constants.FILTER_TYPE.TIME)) {
      if (this.startDateModel !== undefined) {
        filters.startTimeBegin = this.startDateModel[0]
        filters.startTimeBeginStr = formatDate(this.startDateModel[0], 'dd-MM-YYYY HH:mm:ss', 'en')
        filters.startTimeEnd = this.startDateModel[1]
        filters.startTimeEndStr = formatDate(this.startDateModel[1], 'dd-MM-YYYY HH:mm:ss', 'en')
      }
      if (this.endDateModel !== undefined) {
        filters.endTimeBegin = this.endDateModel[0]
        filters.endTimeBeginStr = formatDate(this.endDateModel[0], 'dd-MM-YYYY HH:mm:ss', 'en')
        filters.endTimeEnd = this.endDateModel[1]
        filters.endTimeEndStr = formatDate(this.endDateModel[1], 'dd-MM-YYYY HH:mm:ss', 'en')
      }
    }
    if (this.filterDefined(Constants.FILTER_TYPE.SESSION)) {
      filters.sessionId = this.sessionState?.id
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
    this.onApply.emit(this.currentFilters())
  }

  clearFilter(filterType: string) {
    if (this.filterDefined(filterType)) {
      this.filtering[filterType].selection = []
    }
  }

  clearFilters() {
    this.enableFiltering = false
    this.showFiltering = false
    this.clearFilter(Constants.FILTER_TYPE.DOMAIN)
    this.clearFilter(Constants.FILTER_TYPE.SPECIFICATION)
    this.clearFilter(Constants.FILTER_TYPE.ACTOR)
    this.clearFilter(Constants.FILTER_TYPE.TEST_SUITE)
    this.clearFilter(Constants.FILTER_TYPE.TEST_CASE)
    this.clearFilter(Constants.FILTER_TYPE.COMMUNITY)
    this.clearFilter(Constants.FILTER_TYPE.ORGANISATION)
    this.clearFilter(Constants.FILTER_TYPE.SYSTEM)
    this.clearFilter(Constants.FILTER_TYPE.RESULT)
    if (this.filterDefined(Constants.FILTER_TYPE.TIME)) {
      this.startDateModel = undefined
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
    if (this.enableFiltering) {
      this.showFiltering = !this.showFiltering
    }
  }

  toggleFiltering() {
    if (!this.enableFiltering) {
      this.dataService.async(this.clearFilters.bind(this))
    } else {
      this.showFiltering = true
      if (!this.filterDataLoaded) {
        this.loadFilterData()
      }
    }
  }

  applyTimeFiltering() {
    if (this.enableFiltering && this.filterDataLoaded) {
      this.applyFilters()
    }
  }

  keepApplicableSelections(filterType: string, selectedValues?: ObjectWithId[]) {
    const valuesToKeep:ObjectWithId[] = []
    if (selectedValues) {
      for (let selectedItem of selectedValues) {
        let found = find(this.filtering[filterType].filter, (availableItem: any) => { return availableItem.id == selectedItem.id})
        if (found !== undefined) {
          valuesToKeep.push(selectedItem)
        }
      }
    }
    this.filtering[filterType].selection = valuesToKeep
  }

  private copySelectedValues(filterType: string, keepTick: boolean) {
    if (keepTick) {
      return map(this.filtering[filterType].selection, clone)
    } else {
      return undefined
    }
  }

  private getSelection1Or2<T>(selection1: T[], selection2: T[]) {
    let selection = selection2
    if (selection1 !== undefined && selection1.length > 0) {
      selection = selection1
    }
    return selection
  }

  setDomainFilter() {
    this.filtering[Constants.FILTER_TYPE.DOMAIN].filter = map(this.filtering[Constants.FILTER_TYPE.DOMAIN].all, clone)
  }

  setSpecificationFilter(selection1: Domain[], selection2: Domain[], keepTick: boolean) {
    if (this.filterDefined(Constants.FILTER_TYPE.DOMAIN)) {
      const previousSelectedValues = this.copySelectedValues(Constants.FILTER_TYPE.SPECIFICATION, keepTick)
      let selection = this.getSelection1Or2(selection1, selection2)
      this.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter = map(filter(<Specification[]>this.filtering[Constants.FILTER_TYPE.SPECIFICATION].all, (s) => { return includes(map(selection, (d) => { return d.id }), s.domain) }), clone)
      if (keepTick) {
        this.keepApplicableSelections(Constants.FILTER_TYPE.SPECIFICATION, previousSelectedValues)
      }
    } else {
      this.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter = map(this.filtering[Constants.FILTER_TYPE.SPECIFICATION].all, clone)
    }
  }

  setActorFilter(selection1: Specification[], selection2: Specification[], keepTick: boolean) {
    if (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION)) {
      const previousSelectedValues = this.copySelectedValues(Constants.FILTER_TYPE.ACTOR, keepTick)
      let selection = this.getSelection1Or2(selection1, selection2)
      this.filtering[Constants.FILTER_TYPE.ACTOR].filter = map(filter(<Actor[]>this.filtering[Constants.FILTER_TYPE.ACTOR].all, (a) => { return includes(map(selection, (s) => { return s.id }), a.specification) }), clone)
      if (keepTick) {
        this.keepApplicableSelections(Constants.FILTER_TYPE.ACTOR, previousSelectedValues)
      }
    } else {
      this.filtering[Constants.FILTER_TYPE.ACTOR].filter = map(this.filtering[Constants.FILTER_TYPE.ACTOR].all, clone)
    }
  }

  setTestSuiteFilter(selection1: Specification[], selection2: Specification[], keepTick: boolean) {
    if (this.filterDefined(Constants.FILTER_TYPE.SPECIFICATION)) {
      const previousSelectedValues = this.copySelectedValues(Constants.FILTER_TYPE.TEST_SUITE, keepTick)
      let selection = this.getSelection1Or2(selection1, selection2)
      this.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter = map(filter(<TestSuiteWithTestCases[]>this.filtering[Constants.FILTER_TYPE.TEST_SUITE].all, (t) => { return includes(map(selection, (s) => { return s.id }), t.specification) }), clone)
      if (keepTick) {
        this.keepApplicableSelections(Constants.FILTER_TYPE.TEST_SUITE, previousSelectedValues)
      }
    } else {
      this.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter = map(this.filtering[Constants.FILTER_TYPE.TEST_SUITE].all, clone)
    }
  }

  setTestCaseFilter(selection1: TestSuiteWithTestCases[], selection2: TestSuiteWithTestCases[], keepTick: boolean) {
    if (this.filterDefined(Constants.FILTER_TYPE.TEST_SUITE)) {
      let selection = this.getSelection1Or2(selection1, selection2)
      // Use the 'all' set of test suites to access their test case IDs.
      const selectionToUse = filter(this.filtering[Constants.FILTER_TYPE.TEST_SUITE].all, (ts) => { return find(selection, (selected) => { return ts.id == selected.id }) })
      const previousSelectedValues = this.copySelectedValues(Constants.FILTER_TYPE.TEST_CASE, keepTick)
      let testCasesForFilter = []
      for (let testSuite of selectionToUse) {
        if (testSuite.testCases) {
          for (let testCase of testSuite.testCases) {
            let found = find(<TestCase[]>this.filtering[Constants.FILTER_TYPE.TEST_CASE].all, (tc) => { return tc.id == testCase.id })
            if (found !== undefined) {
              testCasesForFilter.push(found)
            }
          }
        }
      }
      this.filtering[Constants.FILTER_TYPE.TEST_CASE].filter = testCasesForFilter
      if (keepTick) {
        this.keepApplicableSelections(Constants.FILTER_TYPE.TEST_CASE, previousSelectedValues)
      }
    } else {
      this.filtering[Constants.FILTER_TYPE.TEST_CASE].filter = map(this.filtering[Constants.FILTER_TYPE.TEST_CASE].all, clone)
    }
  }

  setCommunityFilter() {
    this.filtering[Constants.FILTER_TYPE.COMMUNITY].filter = map(this.filtering[Constants.FILTER_TYPE.COMMUNITY].all, clone)
  }

  setOrganizationFilter(selection1: Community[], selection2: Community[], keepTick: boolean) {
    if (this.filterDefined(Constants.FILTER_TYPE.COMMUNITY)) {
      const previousSelectedValues = this.copySelectedValues(Constants.FILTER_TYPE.ORGANISATION, keepTick)
      let selection = this.getSelection1Or2(selection1, selection2)
      this.filtering[Constants.FILTER_TYPE.ORGANISATION].filter = map(filter(<Organisation[]>this.filtering[Constants.FILTER_TYPE.ORGANISATION].all, (o) => { return includes(map(selection, (c) => { return c.id }), o.community) }), clone)
      if (keepTick) {
        this.keepApplicableSelections(Constants.FILTER_TYPE.ORGANISATION, previousSelectedValues)
      }
    } else {
      this.filtering[Constants.FILTER_TYPE.ORGANISATION].filter = map(this.filtering[Constants.FILTER_TYPE.ORGANISATION].all, clone)
    }
  }

  setSystemFilter(selection1: any[], selection2: any[], keepTick: boolean) {
    if (this.filterDefined(Constants.FILTER_TYPE.ORGANISATION)) {
      const previousSelectedValues = this.copySelectedValues(Constants.FILTER_TYPE.SYSTEM, keepTick)
      let selection = this.getSelection1Or2(selection1, selection2)
      this.filtering[Constants.FILTER_TYPE.SYSTEM].filter = map(filter(this.filtering[Constants.FILTER_TYPE.SYSTEM].all, (s) => { return includes(map(selection, (o) => { return o.id }), s.owner) }), clone)
      if (keepTick) {
        this.keepApplicableSelections(Constants.FILTER_TYPE.SYSTEM, previousSelectedValues)
      }
    } else {
      this.filtering[Constants.FILTER_TYPE.SYSTEM].filter = map(this.filtering[Constants.FILTER_TYPE.SYSTEM].all, clone)
    }
  }

  resetFilters() {
    this.setDomainFilter()
    this.setCommunityFilter()
    this.setSpecificationFilter(this.filtering[Constants.FILTER_TYPE.DOMAIN].filter, [], false)
    this.setActorFilter(this.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, [], false)
    this.setTestSuiteFilter(this.filtering[Constants.FILTER_TYPE.SPECIFICATION].filter, [], false)
    this.setTestCaseFilter(this.filtering[Constants.FILTER_TYPE.TEST_SUITE].filter, [], false)
    this.setOrganizationFilter(this.filtering[Constants.FILTER_TYPE.COMMUNITY].filter, [], false)
    this.setSystemFilter(this.filtering[Constants.FILTER_TYPE.ORGANISATION].filter, [], false)
    this.filtering[Constants.FILTER_TYPE.RESULT].selection = []
    if (this.filterDefined(Constants.FILTER_TYPE.SESSION)) {
      this.sessionState!.id = undefined
      this.sessionState!.readonly = true
    }
    this.organisationProperties = []
    this.systemProperties = []
    this.startDateModel = undefined
    this.endDateModel = undefined
  }

  getAllTestResults() {
    const results = []
    for (let [key, value] of Object.entries(Constants.TEST_CASE_RESULT)) {
      results.push({id: value})
    }
    return results
  }

  sessionIdClicked() {
    if (this.sessionState!.readonly) {
      this.sessionState!.readonly = false
      if (this.sessionState!.id === undefined) {
        this.sessionState!.id = ''
      }
    }
  }

  applySessionId() {
    if (this.sessionState?.id !== undefined) {
      if (this.sessionState.readonly) {
        // Clear
        this.sessionState.id = undefined
        this.applyFilters()
      } else {
        // Apply
        const trimmed = this.sessionState.id.trim()
        this.sessionState.id = trimmed
        if (this.sessionState.id.length == 0) {
          this.sessionState.id = undefined
        }
        this.sessionState.readonly = true
        this.applyFilters()
      }
    }
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
      enableCheckAll: false
    }
  }

  clearStartRange() {
    this.startDateModel = undefined
  }

  clearEndRange() {
    this.endDateModel = undefined
  }

}
