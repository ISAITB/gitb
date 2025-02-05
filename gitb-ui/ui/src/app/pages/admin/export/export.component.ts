import { Component, EventEmitter, OnInit } from '@angular/core';
import { forkJoin, mergeMap, Observable, of } from 'rxjs';
import { CommunityService } from 'src/app/services/community.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { Community } from 'src/app/types/community';
import { Domain } from 'src/app/types/domain';
import { BaseComponent } from '../../base-component.component';
import { ExportSettings } from '../../../types/export-settings';
import { PopupService } from 'src/app/services/popup.service';
import { saveAs } from 'file-saver'
import { RoutingService } from 'src/app/services/routing.service';
import { MultiSelectConfig } from 'src/app/components/multi-select-filter/multi-select-config';
import { FilterUpdate } from 'src/app/components/test-filter/filter-update';
import { Constants } from 'src/app/common/constants';

@Component({
    selector: 'app-export',
    templateUrl: './export.component.html',
    styleUrls: ['./export.component.less'],
    standalone: false
})
export class ExportComponent extends BaseComponent implements OnInit {

  showDomainOption = true
  includeDomainInCommunityExport = true
  showSystemSettingsOption = true
  pending = false
  community?: Community
  domain?: Domain
  exportType?: string
  communities: Community[] = []
  domains: Domain[] = []
  allCommunityData = false
  allDomainData = false
  allOrganisationData = false
  allSystemSettingData = false
  formCollapsed = true
  loaded = false
  domainsForDeletion: string[] = []
  addExtraDomainsForDeletion = false
  extraDomainsForDeletion: string[] = []
  extraDomainKey?: string
  communitiesForDeletion: string[] = []
  addExtraCommunitiesForDeletion = false
  extraCommunitiesForDeletion: string[] = []
  extraCommunityKey?: string
  settings: ExportSettings = {
    landingPages: false,
    errorTemplates: false,
    triggers: false,
    resources: false,
    legalNotices: false,
    certificateSettings: false,
    customLabels: false,
    customProperties: false,
    organisations: false,
    organisationPropertyValues: false,
    systems: false,
    systemPropertyValues: false,
    statements: false,
    statementConfigurations: false,
    domain: false,
    domainParameters: false,
    specifications: false,
    actors: false,
    endpoints: false,
    testSuites: false,
    communityAdministrators: false,
    organisationUsers: false,
    themes: false,
    defaultLandingPages: false,
    defaultLegalNotices: false,
    defaultErrorTemplates: false,
    systemAdministrators: false,
    systemConfigurations: false
  }
  domainsToDeleteConfig?: MultiSelectConfig<Domain>
  communitiesToDeleteConfig?: MultiSelectConfig<Community>
  Constants = Constants

  constructor(
    public dataService: DataService,
    private communityService: CommunityService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.resetSettings(true)
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
      this.domainsToDeleteConfig = {
        name: "domainsToDelete",
        textField: "fname",
        clearItems: new EventEmitter<void>(),
        replaceItems: new EventEmitter<Domain[]>(),
        replaceSelectedItems: new EventEmitter<Domain[]>(),
        filterLabel: `Select ${this.dataService.labelDomainsLower()}...`,
        loader: () => of(this.domains)
      }
      this.communitiesToDeleteConfig = {
        name: "communitiesToDelete",
        textField: "fname",
        clearItems: new EventEmitter<void>(),
        replaceItems: new EventEmitter<Community[]>(),
        replaceSelectedItems: new EventEmitter<Community[]>(),
        filterLabel: `Select communities...`,
        loader: () => of(this.communities)
      }
      this.loaded = true
    })
    this.routingService.exportBreadcrumbs()
  }

  resetIncludes() {
    this.allCommunityData = false
    this.allDomainData = false
    this.allOrganisationData = false
    this.allSystemSettingData = false
    this.settings = {
      encryptionKey: this.settings.encryptionKey,
      landingPages: false,
      errorTemplates: false,
      triggers: false,
      resources: false,
      legalNotices: false,
      certificateSettings: false,
      customLabels: false,
      customProperties: false,
      organisations: false,
      organisationPropertyValues: false,
      systems: false,
      systemPropertyValues: false,
      statements: false,
      statementConfigurations: false,
      domain: false,
      domainParameters: false,
      specifications: false,
      actors: false,
      endpoints: false,
      testSuites: false,
      communityAdministrators: false,
      organisationUsers: false,
      themes: false,
      defaultLandingPages: false,
      defaultLegalNotices: false,
      defaultErrorTemplates: false,
      systemAdministrators: false,
      systemConfigurations: false,
      domainsToDelete: undefined,
      communitiesToDelete: undefined
    }
  }

  resetSettings(full?:boolean) {
    this.resetIncludes()
    this.settings.encryptionKey = undefined
    if (this.dataService.isSystemAdmin) { 
      this.domain = undefined
      this.community = undefined
      this.showSystemSettingsOption = true
      this.includeDomainInCommunityExport = true
    } else if (this.dataService.isCommunityAdmin) {
      this.showSystemSettingsOption = false
      this.community = this.dataService.community
      if (this.dataService.community!.domain == undefined) {
        this.domain = undefined
        this.includeDomainInCommunityExport = false
      } else {
        this.domain = this.dataService.community!.domain
        this.includeDomainInCommunityExport = true
      }
    }
    if (full) {
      this.exportType = undefined
      this.domainsForDeletion = []
      this.communitiesForDeletion = []
      this.addExtraCommunitiesForDeletion = false
      this.addExtraDomainsForDeletion = false
      this.extraDomainKey = undefined
      this.extraCommunityKey = undefined
      this.extraDomainsForDeletion = []
      this.extraCommunitiesForDeletion = []
    }
    setTimeout(() => {
      this.formCollapsed = this.exportType == undefined
    })
  }

  allCommunityDataChanged() {
    if (this.allCommunityData) {
      this.settings.communityAdministrators = !this.dataService.configuration.ssoEnabled && this.allCommunityData
      this.settings.landingPages = this.allCommunityData
      this.settings.legalNotices = this.allCommunityData
      this.settings.errorTemplates = this.allCommunityData
      this.settings.triggers = this.allCommunityData
      this.settings.resources = this.allCommunityData
      this.settings.certificateSettings = this.allCommunityData
      this.settings.customLabels = this.allCommunityData
      this.settings.customProperties = this.allCommunityData
    }
  }

  allOrganisationDataChanged() {
    if (this.allOrganisationData) {
      this.settings.organisationUsers = !this.dataService.configuration.ssoEnabled && this.allOrganisationData
      this.settings.organisations = this.allOrganisationData
      this.settings.organisationPropertyValues = this.allOrganisationData
      this.settings.systems = this.allOrganisationData
      this.settings.systemPropertyValues = this.allOrganisationData
      if (this.showDomainOption) {
        this.settings.statements = this.allOrganisationData
        this.settings.statementConfigurations = this.allOrganisationData
        // Prerequisites
        this.statementConfigurationsChanged()
      }
      this.systemPropertyValuesChanged()
    }
  }

  allDomainDataChanged() {
    if (this.allDomainData && this.showDomainOption) {
      this.settings.domain = this.allDomainData
      this.settings.specifications = this.allDomainData
      this.settings.actors = this.allDomainData
      this.settings.endpoints = this.allDomainData
      this.settings.testSuites = this.allDomainData
      this.settings.domainParameters = this.allDomainData
    }
  }

  allSystemSettingDataChanged() {
    if (this.allSystemSettingData && this.showSystemSettingsOption) {
      this.settings.themes = this.allSystemSettingData
      this.settings.defaultLandingPages = this.allSystemSettingData
      this.settings.defaultLegalNotices = this.allSystemSettingData
      this.settings.defaultErrorTemplates = this.allSystemSettingData
      this.settings.systemAdministrators = this.allSystemSettingData
      this.settings.systemConfigurations = this.allSystemSettingData
    }
  }

  organisationPropertyValuesChanged() {
    if (this.settings.organisationPropertyValues) {
      this.settings.customProperties = true
    }
  }

  systemPropertyValuesChanged() {
    if (this.settings.systemPropertyValues) {
      this.settings.customProperties = true
    }
  }

  statementConfigurationsChanged() {
    if (this.settings.statementConfigurations) {
      this.settings.statements = true
      this.statementsChanged()
      this.settings.endpoints = true
      this.endpointsChanged()
    }
  }

  statementsChanged() {
    if (this.settings.statements) {
      this.settings.actors = true
      this.actorsChanged()
      this.settings.systems = true
      this.systemsChanged()
    }
  }

  specificationsChanged() {
    if (this.settings.specifications) {
      this.settings.domain = true
    }
  }

  testSuitesChanged() {
    if (this.settings.testSuites) {
      this.settings.actors = true
      this.actorsChanged()
      this.settings.specifications = true
      this.specificationsChanged()
    }
  }

  actorsChanged() {
    if (this.settings.actors) {
      this.settings.specifications = true
      this.specificationsChanged()
    }
  }

  endpointsChanged() {
    if (this.settings.endpoints) {
      this.settings.actors = true
      this.actorsChanged()
    }
  }

  domainParametersChanged() {
    if (this.settings.domainParameters) {
      this.settings.domain = true
    }
  }

  organisationUsersChanged() {
    if (this.settings.organisationUsers) {
      this.settings.organisations = true
    }
  }

  systemsChanged() {
    if (this.settings.systems) {
      this.settings.organisations = true
    }
  }

  isPrerequisiteDomain() {
    return this.settings.domainParameters || this.settings.specifications || this.isPrerequisiteSpecifications()
  }

  isPrerequisiteSpecifications() {
    return this.settings.testSuites || this.settings.actors || this.isPrerequisiteActors()
  }

  isPrerequisiteActors() {
    return this.settings.endpoints || this.isPrerequisiteEndpoints() || this.settings.statements || this.isPrerequisiteStatements() || this.settings.testSuites
  }

  isPrerequisiteEndpoints() {
    return this.settings.statementConfigurations
  }

  isPrerequisiteOrganisations() {
    return this.settings.organisationPropertyValues || this.settings.systems || this.isPrerequisiteSystems() || this.settings.organisationUsers
  }

  isPrerequisiteSystems() {
    return this.settings.systemPropertyValues || this.settings.statements || this.isPrerequisiteStatements()
  }

  isPrerequisiteStatements() {
    return this.settings.statementConfigurations
  }

  isPrerequisiteCustomProperties() {
    return this.settings.organisationPropertyValues || this.settings.systemPropertyValues
  }

  clearIncludes() {
    this.resetIncludes()
  }

  allIncludes() {
    this.allOrganisationData = true
    this.allCommunityData = true
    this.allCommunityDataChanged()
    this.allOrganisationDataChanged()
    if (this.showDomainOption) {
      this.allDomainData = true
      this.allDomainDataChanged()
    }
    if (this.showSystemSettingsOption) {
      this.allSystemSettingData = true
      this.allSystemSettingDataChanged()
    }
  }

  disableAllIncludes() {
    if (this.exportType == 'community') {
      return this.allCommunityData && this.allOrganisationData && (!this.showDomainOption || this.allDomainData) && (!this.showDomainOption || this.allSystemSettingData)
    } else {
      return this.allDomainData && (!this.showDomainOption || this.allSystemSettingData)
    }
  }

  disableClearIncludes() {
    return !(this.allCommunityData ||
      this.allDomainData ||
      this.allOrganisationData ||
      this.settings.landingPages ||
      this.settings.errorTemplates ||
      this.settings.legalNotices ||
      this.settings.triggers ||
      this.settings.resources ||
      this.settings.certificateSettings ||
      this.settings.customLabels ||
      this.settings.customProperties ||
      this.settings.organisations ||
      this.settings.organisationPropertyValues ||
      this.settings.systems ||
      this.settings.systemPropertyValues ||
      this.settings.statements ||
      this.settings.statementConfigurations ||
      this.settings.domain ||
      this.settings.domainParameters ||
      this.settings.specifications ||
      this.settings.actors ||
      this.settings.endpoints ||
      this.settings.testSuites ||
      this.settings.communityAdministrators ||
      this.settings.organisationUsers ||
      this.settings.themes ||
      this.settings.defaultLandingPages ||
      this.settings.defaultLegalNotices ||
      this.settings.defaultErrorTemplates ||
      this.settings.systemAdministrators ||
      this.settings.systemConfigurations
    )
  }

  exportDisabled() {
    return !(
      this.textProvided(this.settings.encryptionKey) && 
        (
          (this.exportType == 'domain' && this.domain != undefined) || 
          (this.exportType == 'community' && this.community != undefined) || 
          this.exportType == 'settings' ||
          (this.exportType == 'deletions' && (this.domainsForDeletion.length > 0 || this.communitiesForDeletion.length > 0 || (this.addExtraDomainsForDeletion && this.extraDomainsForDeletion.length > 0) || (this.addExtraCommunitiesForDeletion && this.extraCommunitiesForDeletion.length > 0)))
        )
      )
  }

  export() {
    this.pending = true
    let exportResult: Observable<ArrayBuffer>
    if (this.exportType == 'domain') {
      exportResult = this.conformanceService.exportDomain(this.domain!.id, this.settings)
    } else if (this.exportType == 'community') {
      exportResult = this.communityService.exportCommunity(this.community!.id, this.settings)
    } else if (this.exportType == 'settings') {
      exportResult = this.communityService.exportSystemSettings(this.settings)
    } else {
      this.settings.domainsToDelete = []
      this.settings.domainsToDelete.push(...this.domainsForDeletion)
      if (this.addExtraDomainsForDeletion) {
        this.settings.domainsToDelete.push(...this.extraDomainsForDeletion)
      }
      this.settings.communitiesToDelete = []
      this.settings.communitiesToDelete.push(...this.communitiesForDeletion)
      if (this.addExtraCommunitiesForDeletion) {
        this.settings.communitiesToDelete.push(...this.extraCommunitiesForDeletion)
      }
      exportResult = this.conformanceService.exportDeletions(this.settings)
    }
    exportResult.subscribe((data) => {
      this.popupService.success('Export successful.')
      this.resetSettings(true)
      const blobData = new Blob([data], {type: 'application/zip'});
      saveAs(blobData, "export.zip");
    }).add(() => {
      this.pending = false
    })
  }

  domainsToDeleteChanged(update: FilterUpdate<Domain>) {
    this.domainsForDeletion = []
    update.values.active.forEach(domain => {
      this.domainsForDeletion.push(domain.apiKey!)
    })
  }

  communitiesToDeleteChanged(update: FilterUpdate<Community>) {
    this.communitiesForDeletion = []
    update.values.active.forEach(community => {
      this.communitiesForDeletion.push(community.apiKey!)
    })
  }

  addExtraDomainKey() {
    if (this.textProvided(this.extraDomainKey)) {
      this.extraDomainsForDeletion.push(this.extraDomainKey!.trim())
      this.extraDomainKey = undefined
    }
  }

  addExtraCommunityKey() {
    if (this.textProvided(this.extraCommunityKey)) {
      this.extraCommunitiesForDeletion.push(this.extraCommunityKey!.trim())
      this.extraCommunityKey = undefined
    }
  }

  deleteExtraDomainKey(index: number) {
    this.extraDomainsForDeletion.splice(index, 1)    
  }

  deleteExtraCommunityKey(index: number) {
    this.extraCommunitiesForDeletion.splice(index, 1)    
  }

}
