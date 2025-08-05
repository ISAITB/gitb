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

  statementNavigable!: boolean
  testCaseNavigable!: boolean
  testSuiteNavigable!: boolean
  actorNavigable!: boolean
  specificationNavigable!: boolean
  domainNavigable!: boolean
  systemNavigable!: boolean
  organisationNavigable!: boolean
  communityNavigable!: boolean

  mainNavigationItem?: { label?: string, action?: () => void }
  extraNavigationItems: Array<{ label?: string, action?: () => void }> = []

  constructor(
    private readonly routingService: RoutingService,
    private readonly dataService: DataService) {
  }

  ngOnInit(): void {
    this.processConfig()
    // Statement
    if (this.statementNavigable) {
      this.extraNavigationItems.push({ label: "View statement", action: () => this.toStatement() })
    }
    // Party information
    if (this.systemNavigable) {
      this.extraNavigationItems.push({ label: `View ${this.dataService.labelSystemLower()}`, action: () => this.toSystem() })
    }
    if (this.organisationNavigable) {
      this.extraNavigationItems.push({ label: `View ${this.dataService.labelOrganisationLower()}`, action: () => this.toOrganisation() })
    }
    if (this.communityNavigable) {
      this.extraNavigationItems.push({ label: "View community", action: () => this.toCommunity() })
    }
    // Specification information
    if (this.domainNavigable || this.specificationNavigable || this.actorNavigable) {
      this.addSeparatorIfNeeded()
      if (this.actorNavigable) {
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelActorLower()}`, action: () => this.toActor() })
      }
      if (this.specificationNavigable) {
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelSpecificationLower()}`, action: () => this.toSpecification() })
      }
      if (this.domainNavigable) {
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelDomainLower()}`, action: () => this.toDomain() })
      }
    }
    // Test case information
    if (this.testSuiteNavigable || this.testCaseNavigable) {
      this.addSeparatorIfNeeded()
      if (this.testCaseNavigable) {
        this.extraNavigationItems.push({ label: "View test case", action: () => this.toTestCase() })
      }
      if (this.testSuiteNavigable) {
        this.extraNavigationItems.push({ label: "View test suite", action: () => this.toTestSuite() })
      }
    }
    // Keep the first item as the main one.
    this.mainNavigationItem = this.extraNavigationItems.shift()
    // If the first item is a separator, remove it.
    if (this.extraNavigationItems.length > 0 && this.extraNavigationItems[0].label == undefined) {
      this.extraNavigationItems.shift()
    }
  }

  private addSeparatorIfNeeded() {
    if (this.extraNavigationItems.length > 0 && this.extraNavigationItems[this.extraNavigationItems.length - 1].label != undefined) {
      this.extraNavigationItems.push({})
    }
  }

  private isOwnOrganisation(): boolean {
    return this.config.organisationId == this.dataService.vendor?.id
  }

  private processConfig() {
    this.domainNavigable = this.isNavigable(this.config.domainId) && (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.community?.domain == undefined))
    this.specificationNavigable = this.isNavigable(this.config.domainId) && this.isNavigable(this.config.specificationId) && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin)
    this.actorNavigable = this.specificationNavigable && this.isNavigable(this.config.actorId)
    this.testSuiteNavigable = this.specificationNavigable && this.isNavigable(this.config.testSuiteId)
    this.testCaseNavigable = this.testSuiteNavigable && this.isNavigable(this.config.testCaseId)
    this.communityNavigable = this.isNavigable(this.config.communityId) && this.config.communityId != Constants.DEFAULT_COMMUNITY_ID && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)
    this.organisationNavigable = this.isNavigable(this.config.organisationId) && (this.isOwnOrganisation() || this.communityNavigable)
    this.systemNavigable = this.organisationNavigable && this.isNavigable(this.config.systemId)
    if (this.config.snapshotId == undefined) {
      this.statementNavigable = this.organisationNavigable
        && this.systemNavigable
        && this.actorNavigable
        && (this.isOwnOrganisation() || this.communityNavigable)
    } else {
      this.statementNavigable = this.config.organisationId != undefined
        && this.config.systemId != undefined
        && this.config.actorId != undefined
        && (this.isOwnOrganisation() || this.config.communityId != undefined)
    }
    this.statementNavigable = this.statementNavigable
      && (this.config.showStatement == undefined || this.config.showStatement)
  }

  private isNavigable(identifier: number|undefined): boolean {
    return identifier != undefined && identifier > 0
  }

  private toSystem() {
    if (this.systemNavigable) {
      if (this.isOwnOrganisation()) {
        // This is the user's own organisation
        this.routingService.toOwnSystemDetails(this.config.systemId!)
      } else {
        this.routingService.toSystemDetails(this.config.communityId!, this.config.organisationId!, this.config.systemId!)
      }
    }
  }

  private toStatement() {
    if (this.statementNavigable) {
      if (this.isOwnOrganisation()) {
        this.routingService.toOwnConformanceStatement(this.config.organisationId!, this.config.systemId!, this.config.actorId!, this.config.snapshotId, this.config.snapshotLabel)
      } else {
        this.routingService.toConformanceStatement(this.config.organisationId!, this.config.systemId!, this.config.actorId!, this.config.communityId!, this.config.snapshotId, this.config.snapshotLabel)
      }
    }
  }

  private toOrganisation() {
    if (this.organisationNavigable) {
      if (this.isOwnOrganisation()) {
        // This is the user's own organisation
        this.routingService.toOwnOrganisationDetails()
      } else {
        // Another organisation
        this.routingService.toOrganisationDetails(this.config.communityId!, this.config.organisationId!)
      }
    }
  }

  private toCommunity() {
    if (this.communityNavigable) {
      this.routingService.toCommunity(this.config.communityId!)
    }
  }

  private toDomain() {
    if (this.domainNavigable) {
      this.routingService.toDomain(this.config.domainId!)
    }
  }

  private toSpecification() {
    if (this.specificationNavigable) {
      this.routingService.toSpecification(this.config.domainId!, this.config.specificationId!)
    }
  }

  private toActor() {
    if (this.actorNavigable) {
      this.routingService.toActor(this.config.domainId!, this.config.specificationId!, this.config.actorId!)
    }
  }

  private toTestSuite() {
    if (this.testSuiteNavigable) {
      this.routingService.toTestSuite(this.config.domainId!, this.config.specificationId!, this.config.testSuiteId!)
    }
  }

  private toTestCase() {
    if (this.testCaseNavigable) {
      this.routingService.toTestCase(this.config.domainId!, this.config.specificationId!, this.config.testSuiteId!, this.config.testCaseId!)
    }
  }

}
