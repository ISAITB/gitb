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

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {OptionalCustomPropertyFormData} from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {SystemService} from 'src/app/services/system.service';
import {System} from 'src/app/types/system';
import {OrganisationTab} from '../../organisation/organisation-details/OrganisationTab';
import {Constants} from 'src/app/common/constants';
import {CommunityService} from 'src/app/services/community.service';

@Component({
    selector: 'app-create-system',
    templateUrl: './create-system.component.html',
    styles: [],
    standalone: false
})
export class CreateSystemComponent extends BaseComponent implements OnInit {

  organisationId!: number
  communityId!: number
  fromCommunityManagement!: boolean
  system: Partial<System> = {}
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'system'
  }
  savePending = false
  loaded = false

  constructor(
    public readonly dataService: DataService,
    private readonly systemService: SystemService,
    private readonly route: ActivatedRoute,
    private readonly popupService: PopupService,
    private readonly routingService: RoutingService,
    private readonly communityService: CommunityService
  ) { super() }

  ngOnInit(): void {
    this.fromCommunityManagement = this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)
    if (this.fromCommunityManagement) {
      this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    } else {
      this.organisationId = this.dataService.vendor!.id
      this.communityId = this.dataService.community!.id
    }
    const onlyPublicProperties = !this.dataService.isSystemAdmin && !this.dataService.isCommunityAdmin
    this.communityService.getSystemParameters(this.communityId, false, onlyPublicProperties).subscribe((data) => {
      this.propertyData.properties = data
    }).add(() => {
      this.loaded = true
    })
  }

  saveEnabled() {
    return this.textProvided(this.system.sname) && this.textProvided(this.system.fname)
  }

  create() {
    if (this.saveEnabled()) {
      this.savePending = true
      this.systemService.registerSystemWithOrganisation(this.system.sname!, this.system.fname!, this.system.description, this.system.version, this.organisationId, this.system.otherSystems, this.propertyData.edit, this.propertyData.properties, this.system.copySystemParameters, this.system.copyStatementParameters)
      .subscribe(() => {
        this.cancel()
        this.popupService.success(this.dataService.labelSystem() + ' created.')
      }).add(() => {
        this.savePending = false
      })
    }
  }

  cancel() {
    if (this.fromCommunityManagement) {
      this.routingService.toOrganisationDetails(this.communityId, this.organisationId, OrganisationTab.systems)
    } else {
      this.routingService.toOwnOrganisationDetails(OrganisationTab.systems)
    }
  }

}
