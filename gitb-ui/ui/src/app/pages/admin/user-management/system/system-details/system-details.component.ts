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
import {SystemFormData} from '../system-form/system-form-data';
import {OptionalCustomPropertyFormData} from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import {ActivatedRoute} from '@angular/router';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {SystemService} from 'src/app/services/system.service';
import {OrganisationTab} from '../../organisation/organisation-details/OrganisationTab';
import {Constants} from 'src/app/common/constants';
import {BreadcrumbType} from 'src/app/types/breadcrumb-type';
import {forkJoin} from 'rxjs';

@Component({
    selector: 'app-system-details',
    templateUrl: './system-details.component.html',
    styles: [],
    standalone: false
})
export class SystemDetailsComponent extends BaseComponent implements OnInit {

  systemId!: number
  organisationId!: number
  communityId!: number
  system: Partial<SystemFormData> = {}
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'system'
  }
  savePending = false
  deletePending = false
  fromCommunityManagement!: boolean
  readonly!: boolean
  showDelete!: boolean
  loaded = false

  constructor(
    private route: ActivatedRoute,
    private confirmationDialogService: ConfirmationDialogService,
    public dataService: DataService,
    private popupService: PopupService,
    private routingService: RoutingService,
    private systemService: SystemService
  ) { super() }

  ngOnInit(): void {
    this.readonly = this.dataService.isVendorUser || (this.dataService.isVendorAdmin && !this.route.snapshot.data.canEditOwnSystem)
    this.showDelete = !this.readonly && (!this.dataService.isVendorAdmin || this.dataService.community!.allowSystemManagement)
    this.fromCommunityManagement = this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)
    if (this.fromCommunityManagement) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    } else {
      this.communityId = this.dataService.community!.id
      this.organisationId = this.dataService.vendor!.id
    }
    this.systemId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID))
    this.system.id = this.systemId
    const viewPropertiesParam = this.route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.VIEW_PROPERTIES)
    if (viewPropertiesParam != undefined) {
      this.propertyData.edit = Boolean(viewPropertiesParam)
    }
    this.propertyData.owner = this.system.id
    const loadSystem$ = this.systemService.getSystemById(this.systemId)
    const loadProperties$ = this.systemService.getSystemParameterValues(this.system.id)
    forkJoin([loadSystem$, loadProperties$]).subscribe((data) => {
      this.system = data[0]
      if (this.system.owner == this.dataService.vendor?.id) {
        this.routingService.ownSystemBreadcrumbs(this.systemId, this.system.sname!)
      } else {
        this.routingService.systemBreadcrumbs(this.communityId, this.organisationId, undefined, this.systemId, this.system.sname!)
      }
      this.propertyData.properties = data[1]
    }).add(() => {
      this.loaded = true
    })
  }

  saveEnabled() {
    return this.textProvided(this.system.sname) && this.textProvided(this.system.fname)
  }

  update() {
    if (this.saveEnabled()) {
      this.savePending = true
      this.systemService.updateSystem(this.system.id!, this.system.sname!, this.system.fname!, this.system.description, this.system.version, this.organisationId, this.system.otherSystems, this.propertyData.edit, this.propertyData.properties, this.system.copySystemParameters!, this.system.copyStatementParameters!)
      .subscribe(() => {
        this.cancel()
        this.popupService.success(this.dataService.labelSystem() + ' updated.')
        if (this.system.owner == this.dataService.vendor?.id) {
          this.dataService.breadcrumbUpdate({ id: this.system.id!, type: BreadcrumbType.ownSystem, label: this.system.sname })
        } else {
          this.dataService.breadcrumbUpdate({ id: this.system.id!, type: BreadcrumbType.system, label: this.system.sname })
        }
      }).add(() => {
        this.savePending = false
      })
    }
  }

  delete() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this "+ this.dataService.labelSystemLower() + "?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.systemService.deleteSystem(this.system.id!, this.organisationId)
      .subscribe(() => {
        this.cancel()
        this.popupService.success(this.dataService.labelSystem() + ' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  manageSystemTests() {
    if (this.fromCommunityManagement) {
      this.routingService.toConformanceStatements(this.communityId, this.organisationId, this.systemId)
    } else {
      this.routingService.toOwnConformanceStatements(this.organisationId, this.systemId)
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
