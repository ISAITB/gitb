/*
 * Copyright (C) 2026 European Union
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
import {NavigationTarget} from '../../types/navigation-target';
import {Utils} from '../../common/utils';

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

  mainNavigationItem?: { label?: string, target?: NavigationTarget }
  extraNavigationItems: Array<{ label?: string, target?: NavigationTarget }> = []

  constructor(
    private readonly routingService: RoutingService,
    private readonly dataService: DataService) {
  }

  ngOnInit(): void {
    this.processConfig()
    // Statement
    if (this.statementNavigable) {
      this.extraNavigationItems.push({ label: "View statement", target: this.statementTarget() })
    }
    // Party information
    if (this.systemNavigable) {
      this.extraNavigationItems.push({ label: `View ${this.dataService.labelSystemLower()}`, target: this.systemTarget() })
    }
    if (this.organisationNavigable) {
      this.extraNavigationItems.push({ label: `View ${this.dataService.labelOrganisationLower()}`, target: this.organisationTarget() })
    }
    if (this.communityNavigable) {
      this.extraNavigationItems.push({ label: "View community", target: this.communityTarget() })
    }
    // Specification information
    if (this.domainNavigable || this.specificationNavigable || this.actorNavigable) {
      this.addSeparatorIfNeeded()
      if (this.actorNavigable) {
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelActorLower()}`, target: this.actorTarget() })
      }
      if (this.specificationNavigable) {
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelSpecificationLower()}`, target: this.specificationTarget() })
      }
      if (this.domainNavigable) {
        this.extraNavigationItems.push({ label: `View ${this.dataService.labelDomainLower()}`, target: this.domainTarget() })
      }
    }
    // Test case information
    if (this.testSuiteNavigable || this.testCaseNavigable) {
      this.addSeparatorIfNeeded()
      if (this.testCaseNavigable) {
        this.extraNavigationItems.push({ label: "View test case", target: this.testCaseTarget() })
      }
      if (this.testSuiteNavigable) {
        this.extraNavigationItems.push({ label: "View test suite", target: this.testSuiteTarget() })
      }
    }
    // Keep the first item as the main one.
    this.mainNavigationItem = this.extraNavigationItems.shift()
    // If the first item is a separator, remove it.
    if (this.extraNavigationItems.length > 0 && this.extraNavigationItems[0].label == undefined) {
      this.extraNavigationItems.shift()
    }
  }

  /**
   * Records the current page as the "return target" before navigating, so that the target page's
   * Back/Cancel control can bring the user back here (with its state restored). Guarded to a plain
   * (unmodified, primary-button) click: a ctrl/cmd/shift/alt or middle click opens the destination
   * in a new tab/window rather than navigating away from the current one, so it must not overwrite
   * the return target recorded for this tab.
   */
  recordReturnTarget(event: MouseEvent) {
    if (Utils.isPlainNavigationClick(event)) {
      this.routingService.recordViewReturnTarget()
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
    this.domainNavigable = this.isNavigable(this.config.domainId) && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin)
    this.specificationNavigable = this.isNavigable(this.config.domainId) && this.isNavigable(this.config.specificationId) && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin)
    this.actorNavigable = this.specificationNavigable && this.isNavigable(this.config.actorId)
    this.testSuiteNavigable = this.specificationNavigable && this.isNavigable(this.config.testSuiteId)
    this.testCaseNavigable = this.testSuiteNavigable && this.isNavigable(this.config.testCaseId)
    this.communityNavigable = this.isNavigable(this.config.communityId) && this.config.communityId != Constants.DEFAULT_COMMUNITY_ID && (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin)
    this.organisationNavigable = this.isNavigable(this.config.organisationId) && (this.isOwnOrganisation() || this.communityNavigable)
    this.systemNavigable = this.organisationNavigable && this.isNavigable(this.config.systemId)
    this.statementNavigable = this.config.organisationId != undefined
      && this.config.systemId != undefined
      && this.config.actorId != undefined
      && (this.isOwnOrganisation() || this.config.communityId != undefined)
      && (this.config.showStatement == undefined || this.config.showStatement)
  }

  private isNavigable(identifier: number|undefined): boolean {
    return identifier != undefined && identifier > 0
  }

  private systemTarget(): NavigationTarget|undefined {
    if (this.systemNavigable) {
      if (this.isOwnOrganisation()) {
        // This is the user's own organisation
        return this.routingService.linkToOwnSystemDetails(this.config.systemId!)
      } else {
        return this.routingService.linkToSystemDetails(this.config.communityId!, this.config.organisationId!, this.config.systemId!)
      }
    }
    return undefined
  }

  private statementTarget(): NavigationTarget|undefined {
    if (this.statementNavigable) {
      if (this.isOwnOrganisation()) {
        return this.routingService.linkToOwnConformanceStatement(this.config.organisationId!, this.config.systemId!, this.config.actorId!, this.config.snapshotId, this.config.snapshotLabel)
      } else {
        return this.routingService.linkToConformanceStatement(this.config.organisationId!, this.config.systemId!, this.config.actorId!, this.config.communityId!, this.config.snapshotId, this.config.snapshotLabel)
      }
    }
    return undefined
  }

  private organisationTarget(): NavigationTarget|undefined {
    if (this.organisationNavigable) {
      if (this.isOwnOrganisation()) {
        // This is the user's own organisation
        return this.routingService.linkToOwnOrganisationDetails()
      } else {
        // Another organisation
        return this.routingService.linkToOrganisationDetails(this.config.communityId!, this.config.organisationId!)
      }
    }
    return undefined
  }

  private communityTarget(): NavigationTarget|undefined {
    if (this.communityNavigable) {
      return this.routingService.linkToCommunity(this.config.communityId!)
    }
    return undefined
  }

  private domainTarget(): NavigationTarget|undefined {
    if (this.domainNavigable) {
      return this.routingService.linkToDomain(this.config.domainId!)
    }
    return undefined
  }

  private specificationTarget(): NavigationTarget|undefined {
    if (this.specificationNavigable) {
      return this.routingService.linkToSpecification(this.config.domainId!, this.config.specificationId!)
    }
    return undefined
  }

  private actorTarget(): NavigationTarget|undefined {
    if (this.actorNavigable) {
      return this.routingService.linkToActor(this.config.domainId!, this.config.specificationId!, this.config.actorId!)
    }
    return undefined
  }

  private testSuiteTarget(): NavigationTarget|undefined {
    if (this.testSuiteNavigable) {
      return this.routingService.linkToTestSuite(this.config.domainId!, this.config.specificationId!, this.config.testSuiteId!)
    }
    return undefined
  }

  private testCaseTarget(): NavigationTarget|undefined {
    if (this.testCaseNavigable) {
      return this.routingService.linkToTestCase(this.config.domainId!, this.config.specificationId!, this.config.testSuiteId!, this.config.testCaseId!)
    }
    return undefined
  }

  protected readonly Constants = Constants;
}
