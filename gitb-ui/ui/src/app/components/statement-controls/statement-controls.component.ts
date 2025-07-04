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

import { Component, Input } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { ConformanceIds } from 'src/app/types/conformance-ids';

@Component({
    selector: 'app-statement-controls',
    templateUrl: './statement-controls.component.html',
    standalone: false
})
export class StatementControlsComponent {

  @Input() conformanceIds!: ConformanceIds
  @Input() communityId!:number
  @Input() organisationId!: number
  @Input() snapshotId!:number|undefined
  @Input() snapshotLabel!:string|undefined
  @Input() hasBadge!: boolean|undefined

  constructor(
    private readonly routingService: RoutingService,
    public readonly dataService: DataService
  ) {}

  toCommunity() {
    this.routingService.toCommunity(this.communityId)
  }

  toOrganisation() {
    if (this.organisationId == this.dataService.vendor!.id) {
      // Own organisation
      this.routingService.toOwnOrganisationDetails()
    } else {
      this.routingService.toOrganisationDetails(this.communityId, this.organisationId)
    }
  }

  showToOrganisation() {
    return this.organisationId != undefined && this.organisationId >= 0
  }

  toSystem() {
    if (this.organisationId == this.dataService.vendor!.id) {
      this.routingService.toOwnSystemDetails(this.conformanceIds.systemId)
    } else {
      this.routingService.toSystemDetails(this.communityId, this.organisationId, this.conformanceIds.systemId)
    }
  }

  showToSystem() {
    return this.showToOrganisation() && this.conformanceIds.systemId != undefined && this.conformanceIds.systemId >= 0
  }

  toStatement() {
    if (this.organisationId == this.dataService.vendor?.id) {
      this.routingService.toOwnConformanceStatement(this.organisationId, this.conformanceIds.systemId, this.conformanceIds.actorId, this.snapshotId, this.snapshotLabel)
    } else {
      this.routingService.toConformanceStatement(this.organisationId, this.conformanceIds.systemId, this.conformanceIds.actorId, this.communityId, this.snapshotId, this.snapshotLabel)
    }
  }

  toDomain() {
    this.routingService.toDomain(this.conformanceIds.domainId)
  }

  showToDomain() {
    return this.conformanceIds.domainId != undefined && this.conformanceIds.domainId >= 0 && (
      this.dataService.isSystemAdmin || (
        this.dataService.isCommunityAdmin && this.dataService.community?.domain != undefined
      )
    )
  }

  toSpecification() {
    this.routingService.toSpecification(this.conformanceIds.domainId, this.conformanceIds.specificationId)
  }

  showToSpecification() {
    return this.showToDomain() && this.conformanceIds.specificationId != undefined && this.conformanceIds.specificationId >= 0
  }

  toActor() {
    this.routingService.toActor(this.conformanceIds.domainId, this.conformanceIds.specificationId, this.conformanceIds.actorId)
  }

  showToActor() {
    return this.showToSpecification() && this.conformanceIds.actorId != undefined && this.conformanceIds.actorId >= 0
  }


}
