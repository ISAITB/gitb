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

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
    selector: 'app-specification-group-details',
    templateUrl: './specification-group-details.component.html',
    standalone: false
})
export class SpecificationGroupDetailsComponent extends BaseComponent implements OnInit {

  group: Partial<SpecificationGroup> = {}
  domainId!: number
  groupId!: number
  deletePending = false
  savePending = false
  loaded = false

  constructor(
    public readonly dataService: DataService,
    private readonly specificationService: SpecificationService,
    private readonly popupService: PopupService,
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) { super() }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.groupId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_GROUP_ID))
    this.specificationService.getSpecificationGroup(this.groupId)
    .subscribe((data) => {
      this.group = data
      this.routingService.specificationGroupBreadcrumbs(this.domainId, this.groupId, this.group.sname!)
    }).add(() => {
      this.loaded = true
    })
  }

  deleteGroup(withSpecs: boolean) {
    let message: string
    if (withSpecs) {
      message = "Are you sure you want to delete this "+this.dataService.labelSpecificationLower()+" and any included "+this.dataService.labelSpecificationInGroupsLower()+"?"
    } else {
      message = "Are you sure you want to delete this "+this.dataService.labelSpecificationGroupLower()+" ("+this.dataService.labelSpecificationInGroupsLower()+" will not be deleted)?"
    }
		this.confirmationDialogService.confirmedDangerous("Confirm delete", message, "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.specificationService.deleteSpecificationGroup(this.groupId, withSpecs)
      .subscribe(() => {
        this.back()
        this.popupService.success(this.dataService.labelSpecificationGroup()+' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

	saveDisabled() {
    return !(this.textProvided(this.group?.sname) && this.textProvided(this.group?.fname))
  }

  saveGroupChanges() {
    this.savePending = true
		this.specificationService.updateSpecificationGroup(this.groupId, this.group.sname!, this.group.fname!, this.group.description, this.group.reportMetadata)
		.subscribe(() => {
			this.popupService.success(this.dataService.labelSpecificationGroup()+' updated.')
      this.dataService.breadcrumbUpdate({id: this.groupId, type: BreadcrumbType.specificationGroup, label: this.group.sname!})
    }).add(() => {
      this.savePending = false
    })
  }

  back() {
    this.routingService.toDomain(this.domainId)
  }

}
