import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
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

@Component({
  selector: 'app-export',
  templateUrl: './export.component.html',
  styles: [
  ]
})
export class ExportComponent extends BaseComponent implements OnInit {

  showDomainOption = true
  pending = false
  community?: Community
  domain?: Domain
  exportType?: string
  communities: Community[] = []
  domains: Domain[] = []
  allCommunityData = false
  allDomainData = false
  allOrganisationData = false
  showEncryptionKey = false
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
    organisationUsers: false    
  }

  constructor(
    public dataService: DataService,
    private communityService: CommunityService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.resetSettings(true)
    if (this.dataService.isSystemAdmin) {
      // Get communities
      this.communityService.getCommunities([], true)
      .subscribe((data) => {
        this.communities = data
      })
      // Get domains
      this.conformanceService.getDomains()
      .subscribe((data) => {
        this.domains = data
      })
    }
    this.routingService.exportBreadcrumbs()
  }

  resetIncludes() {
    this.allCommunityData = false
    this.allDomainData = false
    this.allOrganisationData = false
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
      organisationUsers: false    
    }
  }

  resetSettings(full?:boolean) {
    this.resetIncludes()
    this.settings.encryptionKey = undefined
    if (this.dataService.isSystemAdmin) { 
      this.domain = undefined
      this.community = undefined
    }    
    if (full) {
      if (this.dataService.isCommunityAdmin) {
        this.community = this.dataService.community
        if (this.dataService.community!.domain != undefined) {
            this.domain = this.dataService.community!.domain
            this.exportType = undefined
        } else {
            this.exportType = 'community'
            this.showDomainOption = false
        }
        this.domain = this.dataService.community!.domain
      } else if (this.dataService.isSystemAdmin) { 
        this.exportType = undefined
      }    
    }
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
  }

  disableAllIncludes() {
    if (this.exportType == 'community') {
      return this.allCommunityData && this.allOrganisationData && (!this.showDomainOption || this.allDomainData)
    } else {
      return this.allDomainData
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
      this.settings.organisationUsers)
  }

  exportDisabled() {
    return !((this.exportType == 'domain' && this.domain != undefined || this.exportType == 'community' && this.community != undefined) && this.textProvided(this.settings.encryptionKey))
  }

  export() {
    this.pending = true
    let exportResult: Observable<ArrayBuffer>
    if (this.exportType == 'domain') {
      exportResult = this.conformanceService.exportDomain(this.domain!.id, this.settings)
    } else {
      exportResult = this.communityService.exportCommunity(this.community!.id, this.settings)
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

}
