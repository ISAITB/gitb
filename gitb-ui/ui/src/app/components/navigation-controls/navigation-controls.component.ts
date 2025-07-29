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

import {Component, Input, OnInit} from '@angular/core';
import {RoutingService} from '../../services/routing.service';
import {DataService} from '../../services/data.service';
import {NavigationControlsConfig} from './navigation-controls-config';
import {Constants} from '../../common/constants';

@Component({
  selector: 'app-navigation-controls',
  standalone: false,
  templateUrl: './navigation-controls.component.html'
})
export class NavigationControlsComponent implements OnInit {

  @Input() config!: NavigationControlsConfig

  configToUse!: NavigationControlsConfig
  mainNavigationItem?: { label?: string, action?: () => void }
  extraNavigationItems: Array<{ label?: string, action?: () => void }> = []

  constructor(
    private readonly routingService: RoutingService,
    private readonly dataService: DataService) {
  }

  ngOnInit(): void {
    this.prepareConfig()
    // Statement
    if ((this.configToUse.showStatement == undefined || this.configToUse.showStatement) && this.configToUse.organisationId != undefined && this.configToUse.systemId != undefined && this.configToUse.communityId != undefined && this.configToUse.actorId != undefined && this.configToUse.specificationId != undefined) {
      this.extraNavigationItems.push({ label: "View statement", action: () => this.toStatement() })
    }
    // Party information
    if (this.configToUse.organisationId != undefined) {
      if (this.configToUse.systemId != undefined) {
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelSystemLower()}`, action: () => this.toSystem() })
      }
      this.extraNavigationItems.push({ label: `View ${this.dataService.labelOrganisationLower()}`, action: () => this.toOrganisation() })
    }
    if (this.configToUse.communityId != undefined && this.configToUse.communityId != Constants.DEFAULT_COMMUNITY_ID && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)) {
      this.extraNavigationItems.push({ label: "View community", action: () => this.toCommunity() })
    }
    // Specification information
    const showDomain = this.configToUse.domainId != undefined && (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.community?.domain != undefined))
    const showSpecification = showDomain && this.configToUse.specificationId != undefined
    if (showDomain) {
      if (this.extraNavigationItems.length > 0 && this.extraNavigationItems[this.extraNavigationItems.length - 1].label != undefined) {
        this.extraNavigationItems.push({})
      }
      if (showSpecification) {
        if (this.configToUse.actorId != undefined) {
          this.extraNavigationItems.push({ label: `View ${this.dataService.labelActorLower()}`, action: () => this.toActor() })
        }
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelSpecificationLower()}`, action: () => this.toSpecification() })
      }
      this.extraNavigationItems.push({ label: `View ${this.dataService.labelDomainLower()}`, action: () => this.toDomain() })
    }
    // Test case information
    if (showSpecification && this.configToUse.testSuiteId != undefined) {
      if (this.extraNavigationItems.length > 0 && this.extraNavigationItems[this.extraNavigationItems.length - 1].label != undefined) {
        this.extraNavigationItems.push({})
      }
      if (this.configToUse.testCaseId != undefined) {
        this.extraNavigationItems.push({ label: "View test case", action: () => this.toTestCase() })
      }
      this.extraNavigationItems.push({ label: "View test suite", action: () => this.toTestSuite() })
    }
    // Keep the first item as the main one.
    this.mainNavigationItem = this.extraNavigationItems.shift()
    // If the first item is a separator, remove it.
    if (this.extraNavigationItems.length > 0 && this.extraNavigationItems[0].label == undefined) {
      this.extraNavigationItems.shift()
    }
  }

  private prepareConfig() {
    this.configToUse = { ...this.config}
    if (this.configToUse.testCaseId != undefined && this.configToUse.testCaseId < 0) this.configToUse.testCaseId = undefined
    if (this.configToUse.testSuiteId != undefined && this.configToUse.testSuiteId < 0) this.configToUse.testSuiteId = undefined
    if (this.configToUse.actorId != undefined && this.configToUse.actorId < 0) this.configToUse.actorId = undefined
    if (this.configToUse.specificationId != undefined && this.configToUse.specificationId < 0) this.configToUse.specificationId = undefined
    if (this.configToUse.domainId != undefined && this.configToUse.domainId < 0) this.configToUse.domainId = undefined
    if (this.configToUse.systemId != undefined && this.configToUse.systemId < 0) this.configToUse.systemId = undefined
    if (this.configToUse.organisationId != undefined && this.configToUse.organisationId < 0) this.configToUse.organisationId = undefined
    if (this.configToUse.communityId != undefined && this.configToUse.communityId < 0) this.configToUse.communityId = undefined
  }

  private toSystem() {
    if (this.configToUse.organisationId! == this.dataService.vendor!.id) {
      // This is the user's own organisation
      this.routingService.toOwnSystemDetails(this.configToUse.systemId!)
    } else {
      this.routingService.toSystemDetails(this.configToUse.communityId!, this.configToUse.organisationId!, this.configToUse.systemId!)
    }
  }

  private toStatement() {
    if (this.configToUse.organisationId! == this.dataService.vendor?.id) {
      this.routingService.toOwnConformanceStatement(this.configToUse.organisationId!, this.configToUse.systemId!, this.configToUse.actorId!)
    } else {
      this.routingService.toConformanceStatement(this.configToUse.organisationId!, this.configToUse.systemId!, this.configToUse.actorId!, this.configToUse.communityId!)
    }
  }

  private toOrganisation() {
    if (this.configToUse.organisationId! == this.dataService.vendor!.id) {
      // This is the user's own organisation
      this.routingService.toOwnOrganisationDetails()
    } else {
      // Another organisation
      this.routingService.toOrganisationDetails(this.configToUse.communityId!, this.configToUse.organisationId!)
    }
  }

  private toCommunity() {
    this.routingService.toCommunity(this.configToUse.communityId!)
  }

  private toDomain() {
    this.routingService.toDomain(this.configToUse.domainId!)
  }

  private toSpecification() {
    this.routingService.toSpecification(this.configToUse.domainId!, this.configToUse.specificationId!)
  }

  private toActor() {
    this.routingService.toActor(this.configToUse.domainId!, this.configToUse.specificationId!, this.configToUse.actorId!)
  }

  private toTestSuite() {
    this.routingService.toTestSuite(this.configToUse.domainId!, this.configToUse.specificationId!, this.configToUse.testSuiteId!)
  }

  private toTestCase() {
    this.routingService.toTestCase(this.configToUse.domainId!, this.configToUse.specificationId!, this.configToUse.testSuiteId!, this.configToUse.testCaseId!)
  }

}
