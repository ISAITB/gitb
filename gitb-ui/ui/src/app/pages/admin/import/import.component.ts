import { Component, EventEmitter, OnDestroy, OnInit } from '@angular/core';
import { forkJoin, mergeMap, Observable, of } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { CommunityService } from 'src/app/services/community.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Community } from 'src/app/types/community';
import { Domain } from 'src/app/types/domain';
import { FileData } from 'src/app/types/file-data.type';
import { ImportItem } from 'src/app/types/import-item';
import { ImportPreview } from 'src/app/types/import-preview';
import { ImportSettings } from 'src/app/types/import-settings';
import { BaseComponent } from '../../base-component.component';
import { ImportItemState } from './import-item-state';
import { ImportItemStateGroup } from './import-item-state-group';
import { RoutingService } from 'src/app/services/routing.service';
import { ValidationState } from 'src/app/types/validation-state';
import { ErrorDescription } from 'src/app/types/error-description';
import {MultiSelectConfig} from '../../../components/multi-select-filter/multi-select-config';
import {FilterUpdate} from '../../../components/test-filter/filter-update';

@Component({
    selector: 'app-import',
    templateUrl: './import.component.html',
    styles: [],
    standalone: false
})
export class ImportComponent extends BaseComponent implements OnInit, OnDestroy {

  itemId = 0
  pending = false
  cancelPending = false
  community?: Community
  domain?: Domain
  exportType?: string
  communities: Community[] = []
  domains: Domain[] = []
  pendingImportId?: string
  showDomainOption = true
  importStep1 = true
  importStep2 = false
  settings: ImportSettings = {}
  importItemGroups?: ImportItemStateGroup[]
  archiveData?: FileData
  importItemActionLabels: {[key: number]: string} = {}
  importItemTypeLabels: {[key: number]: string} = {}
  newTarget = false
  replaceName = false
  formCollapsed = true
  newTargetAnimated = false
  formAnimated = false
  validation = new ValidationState()
  resetEmitter = new EventEmitter<void>()

  domainSelectionConfig!: MultiSelectConfig<Domain>
  communitySelectionConfig!: MultiSelectConfig<Community>

  constructor(
    private communityService: CommunityService,
    private conformanceService: ConformanceService,
    public dataService: DataService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.resetSettings(true)
    // Initialise selection configs
    this.domainSelectionConfig = {
      name: "domain",
      textField: "fname",
      singleSelection: true,
      singleSelectionPersistent: true,
      filterLabel: `Select ${this.dataService.labelDomainLower()}...`,
      loader: () => of(this.domains)
    }
    this.communitySelectionConfig = {
      name: "community",
      textField: "fname",
      singleSelection: true,
      singleSelectionPersistent: true,
      filterLabel: `Select community...`,
      loader: () => of(this.communities)
    }
    // Get communities and domains
    let communities$: Observable<Community[]>
    if (this.dataService.isSystemAdmin) {
      communities$ = this.communityService.getCommunities([], true)
    } else {
      communities$ = of([this.dataService.community!])
    }
    let domains$: Observable<Domain[]>
    if (this.dataService.isSystemAdmin) {
      domains$ = this.conformanceService.getDomains([], true)
    } else {
      domains$ = this.conformanceService.getCommunityDomains(this.dataService.community!.id)
        .pipe(
          mergeMap((data) => {
            return of(data.domains)
          })
        )
    }
    forkJoin([communities$, domains$]).subscribe((data) => {
      this.communities = data[0]
      this.domains = data[1]
    })
    this.routingService.importBreadcrumbs()
  }

  ngOnDestroy(): void {
    if (this.pendingImportId != undefined) {
      this.cancel()
    }
  }

  resetSettings(full?: boolean) {
    this.validation.clearErrors()
    this.formAnimated = false
    this.itemId = 0
    this.importStep1 = true
    this.importStep2 = false
    // Clear data
    this.archiveData = undefined
    this.pendingImportId = undefined
    this.importItemGroups = undefined
    this.settings = {}
    this.settings.encryptionKey = undefined
    this.settings.createNewData = true
    this.settings.deleteUnmatchedData = true
    this.settings.updateMatchingData = true
    this.settings.shortNameReplacement = undefined
    this.settings.fullNameReplacement = undefined
    this.resetEmitter.emit()
    if (this.dataService.isSystemAdmin) {
      this.domain = undefined
      this.community = undefined
    } else {
      this.community = this.dataService.community
      if (this.dataService.community?.domain == undefined) {
        this.domain = undefined
      } else {
        this.domain = this.dataService.community.domain
      }
    }
    this.newTarget = false
    this.replaceName = false
    if (full) {
      this.exportType = undefined
    }
    if (this.exportType == 'domain' && this.domains.length == 0 || this.exportType == 'community' && this.communities.length == 0) {
      this.newTarget = true
    }
    this.formAnimated = true
    setTimeout(() => {
      this.formCollapsed = this.exportType == undefined
    })
  }

  domainSelected(event: FilterUpdate<Domain>) {
    this.domain = event.values.active[0]
  }

  communitySelected(event: FilterUpdate<Community>) {
    this.community = event.values.active[0]
  }

  uploadArchive(file: FileData) {
    this.archiveData = file
  }

  setupImportItemLabels() {
    this.importItemActionLabels = {}
    this.importItemActionLabels[Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY] = 'New'
    this.importItemActionLabels[Constants.IMPORT_ITEM_MATCH.BOTH] = 'Update'
    this.importItemActionLabels[Constants.IMPORT_ITEM_MATCH.DB_ONLY] = 'Delete'
    this.importItemTypeLabels = {}
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.DOMAIN] = this.dataService.labelDomains()
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.DOMAIN_PARAMETER] = this.dataService.labelDomain() + ' parameters'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SPECIFICATION] = this.dataService.labelSpecifications()
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SPECIFICATION_GROUP] = this.dataService.labelSpecificationGroups()
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ACTOR] = this.dataService.labelActors()
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ENDPOINT] = this.dataService.labelEndpoints()
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ENDPOINT_PARAMETER] = this.dataService.labelEndpoint() + ' parameters'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.TEST_SUITE] = 'Test suites'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.COMMUNITY] = 'Communities'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ADMINISTRATOR] = 'Administrators'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.CUSTOM_LABEL] = 'Custom labels'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ORGANISATION_PROPERTY] = 'Custom ' + this.dataService.labelOrganisationLower() + ' properties'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SYSTEM_PROPERTY] = 'Custom ' + this.dataService.labelSystemLower() + ' properties'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.LANDING_PAGE] = 'Landing pages'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.LEGAL_NOTICE] = 'Legal notices'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ERROR_TEMPLATE] = 'Error templates'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.TRIGGER] = 'Triggers'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.RESOURCE] = 'Resources'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ORGANISATION] = this.dataService.labelOrganisations()
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ORGANISATION_USER] = 'Users'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.ORGANISATION_PROPERTY_VALUE] = 'Custom ' + this.dataService.labelOrganisationLower() + ' property values'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SYSTEM] = this.dataService.labelSystems()
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SYSTEM_PROPERTY_VALUE] = 'Custom ' + this.dataService.labelSystemLower() + ' property values'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.STATEMENT] = 'Conformance statements'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.STATEMENT_CONFIGURATION] = 'Conformance statement configurations'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SYSTEM_SETTINGS] = 'System settings'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SYSTEM_RESOURCE] = 'Resources'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.THEME] = 'Themes'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.DEFAULT_LANDING_PAGE] = 'Default landing pages'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.DEFAULT_LEGAL_NOTICE] = 'Default legal notices'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.DEFAULT_ERROR_TEMPLATE] = 'Error templates'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SYSTEM_ADMINISTRATOR] = 'Administrators'
    this.importItemTypeLabels[Constants.IMPORT_ITEM_TYPE.SYSTEM_CONFIGURATION] = 'Configuration settings'
  }

  typeDescription(type: number) {
    return this.importItemTypeLabels[type]
  }

  defaultAction(matchType: number) {
    if (matchType == Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY) {
      if (this.settings.createNewData) {
        return Constants.IMPORT_ITEM_CHOICE.PROCEED
      } else {
        return Constants.IMPORT_ITEM_CHOICE.SKIP
      }
    } else if (matchType == Constants.IMPORT_ITEM_MATCH.BOTH) {
      if (this.settings.updateMatchingData) {
          return Constants.IMPORT_ITEM_CHOICE.PROCEED
      } else {
          return Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN
      }
    } else {
      if (this.settings.deleteUnmatchedData)
        return Constants.IMPORT_ITEM_CHOICE.PROCEED
      else
        return Constants.IMPORT_ITEM_CHOICE.SKIP
    }
  }

  importDisabled() {
    return this.importStep1 && !(
      this.archiveData?.file != undefined
        && this.textProvided(this.settings.encryptionKey)
        && (
          this.exportType == 'domain' && (
              (this.newTarget && (!this.replaceName || (this.textProvided(this.settings.shortNameReplacement) && this.textProvided(this.settings.fullNameReplacement))))
              || (!this.newTarget && this.domain != undefined)
          ) ||
          this.exportType == 'community' && (
              (this.newTarget && (!this.replaceName || (this.textProvided(this.settings.shortNameReplacement) && this.textProvided(this.settings.fullNameReplacement))))
              || (!this.newTarget && this.community != undefined)
          ) ||
          this.exportType == 'settings' ||
          this.exportType == 'deletions'
        )
    )
  }

  private getTargetDomainId() {
    let domainId = -1
    if (this.domain && (this.dataService.isCommunityAdmin || !this.newTarget)) {
      domainId = this.domain.id
    }
    return domainId
  }

  private getTargetCommunityId() {
    let communityId = -1
    if (this.community && (this.dataService.isCommunityAdmin || !this.newTarget)) {
      communityId = this.community.id
    }
    return communityId
  }

  import() {
    this.validation.clearErrors()
    this.pending = true
    let result: Observable<ImportPreview|ErrorDescription>
    if (!this.replaceName) {
      this.settings.shortNameReplacement = undefined
      this.settings.fullNameReplacement = undefined
    }
    if (this.exportType == 'domain') {
      result = this.conformanceService.uploadDomainExport(this.getTargetDomainId(), this.settings, this.archiveData!)
    } else if (this.exportType == 'community') {
      result = this.communityService.uploadCommunityExport(this.getTargetCommunityId(), this.settings, this.archiveData!)
    } else if (this.exportType == 'settings') {
      result = this.communityService.uploadSystemSettingsExport(this.settings, this.archiveData!)
    } else {
      result = this.conformanceService.uploadDeletionsExport(this.settings, this.archiveData!)
    }
    result.subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        this.setupImportItemLabels()
        this.pendingImportId = data.pendingImportId
        let importItemStates: ImportItemStateGroup[]|undefined
        if (data.importItems != undefined) {
          importItemStates = []
          const groupMap:{[key: number]: ImportItemStateGroup} = {}
          for (let item of data.importItems) {
            const state = this.toImportItemState(item)
            if (groupMap[item.type] == undefined) {
              groupMap[item.type] = {
                type: item.type,
                typeLabel: this.typeDescription(item.type),
                open: false,
                items: []
              }
              importItemStates.push(groupMap[item.type])
            }
            groupMap[item.type].items.push(state)

            importItemStates.push()
          }
        }
        this.importItemGroups = importItemStates
        this.archiveData = undefined
        this.importStep1 = false
        this.importStep2 = true
      }
    }).add(() => {
      this.pending = false
    })
  }

  toImportItemState(item: ImportItem): ImportItemState {
    this.itemId += 1
    const state: Partial<ImportItemState> = {
      id: this.itemId,
      name: item.name,
      type: item.type,
      match: item.match,
      target: item.target,
      source: item.source,
      process: item.process,
      disableProcessChoice: false,
      hasGroups: false,
      open: false
    }
    if (item.children != undefined && item.children.length > 0) {
      state.children = []
      state.groups = []
      state.hasGroups = true
      const childGroupMap:{[key: number]: ImportItemStateGroup} = {}
      for (let child of item.children) {
        const childAsState = this.toImportItemState(child)
        state.children.push(childAsState)
        if (childGroupMap[child.type] == undefined) {
          childGroupMap[child.type] = {
            type: child.type,
            typeLabel: this.typeDescription(child.type),
            open: false,
            items: []
          }
          state.groups.push(childGroupMap[child.type])
        }
        childGroupMap[child.type].items.push(childAsState)
      }
    }
    if (item.process == undefined) {
      let defaultAction = this.defaultAction(item.match)
      // Don't allow a community admin to add or delete a domain.
      if (this.dataService.isCommunityAdmin && item.type == Constants.IMPORT_ITEM_TYPE.DOMAIN && item.match != Constants.IMPORT_ITEM_MATCH.BOTH) {
        defaultAction = Constants.IMPORT_ITEM_CHOICE.SKIP
        state.disableProcessChoice = true
      }
      if (!state.hasGroups && defaultAction == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN) {
        defaultAction = Constants.IMPORT_ITEM_CHOICE.SKIP
      }
      if (state.selectedProcessOption != undefined) {
        state.process = state.selectedProcessOption
        if (state.selectedProcessOption == Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT) {
          state.previousOption = defaultAction
        }
      } else {
        state.process = defaultAction
      }
    }
    if (state.previousOption == undefined) {
      state.previousOption = state.process
    }
    if (state.selectedProcessOption == undefined) {
      state.selectedProcessOption = state.process
    }
    return state as ImportItemState
  }

  toImportItem(state: ImportItemState): ImportItem {
    const item: ImportItem = {
      name: state.name,
      type: state.type,
      match: state.match,
      target: state.target,
      source: state.source,
      process: state.process,
      children: []
    }
    if (state.children != undefined && state.children.length > 0) {
      for (let childItem of state.children) {
        item.children.push(this.toImportItem(childItem))
      }
    }
    return item
  }

  cancel() {
    let result: Observable<void>
    if (this.exportType == 'domain') {
      result = this.conformanceService.cancelDomainImport(this.getTargetDomainId(), this.pendingImportId!)
    } else if (this.exportType == 'community') {
      result = this.communityService.cancelCommunityImport(this.getTargetCommunityId(), this.pendingImportId!)
    } else {
      result = this.communityService.cancelSystemSettingsImport(this.pendingImportId!)
    }
    this.cancelPending = true
    result.subscribe(() => {
      this.resetSettings(true)
      this.popupService.success('Import cancelled.')
    }).add(() => {
      this.cancelPending = false
    })
  }

  cleanImportItems() {
    const cleanItems: ImportItem[] = []
    if (this.importItemGroups != undefined) {
      for (let itemGroup of this.importItemGroups) {
        for (let item of itemGroup.items) {
          cleanItems.push(this.toImportItem(item))
        }
      }
    }
    return cleanItems
  }

  confirm() {
    this.pending = true
    let result: Observable<void>
    if (this.exportType == 'domain') {
      result = this.conformanceService.confirmDomainImport(this.getTargetDomainId(), this.pendingImportId!, this.settings!, this.cleanImportItems())
    } else if (this.exportType == 'community') {
      result = this.communityService.confirmCommunityImport(this.getTargetCommunityId(), this.pendingImportId!, this.settings!, this.cleanImportItems())
    } else if (this.exportType == 'settings') {
      result = this.communityService.confirmSystemSettingsImport(this.pendingImportId!, this.settings!, this.cleanImportItems())
    } else {
      result = this.conformanceService.confirmDeletionsImport(this.pendingImportId!, this.settings!, this.cleanImportItems())
    }
    result.subscribe(() => {
      this.resetSettings(true)
      this.popupService.success("Import successful.")
    }).add(() => {
      this.pending = false
    })
  }

}
