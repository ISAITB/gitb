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
import { CommunityService } from 'src/app/services/community.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { TypedLabelConfig } from 'src/app/types/typed-label-config.type';

@Component({
    selector: 'app-community-labels',
    templateUrl: './community-labels.component.html',
    styleUrls: ['./community-labels.component.less'],
    standalone: false
})
export class CommunityLabelsComponent extends BaseComponent implements OnInit {

  communityId!: number
  busy = false
  loaded = false
  Constants = Constants
  labelTypeDescription: {[key:number]: string} = {}
  labels: TypedLabelConfig[] = []

  constructor(
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly communityService: CommunityService,
    private readonly dataService: DataService,
    private readonly popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    this.labelTypeDescription[Constants.LABEL_TYPE.DOMAIN] = "The set of related specifications and test suites the community will be using for testing."
    this.labelTypeDescription[Constants.LABEL_TYPE.SPECIFICATION] = "The specific named or versioned requirements that community members will be selecting to test for. When using specification groups the label will refer to the combination of group and specification."
    this.labelTypeDescription[Constants.LABEL_TYPE.SPECIFICATION_GROUP] = "A group of related specifications. The label in this case refers to when this concept is presented independently from its included specifications."
    this.labelTypeDescription[Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP] = "A specification contained within a group of specifications. The label in this case refers to when this concept is presented independently from its group."
    this.labelTypeDescription[Constants.LABEL_TYPE.ACTOR] = "The specification role that community members' systems will be assigned with during testing."
    this.labelTypeDescription[Constants.LABEL_TYPE.ENDPOINT] = "The set of actor-specific configuration parameters applicable when testing against a specification."
    this.labelTypeDescription[Constants.LABEL_TYPE.ORGANISATION] = "The entity corresponding to a member of the current community."
    this.labelTypeDescription[Constants.LABEL_TYPE.SYSTEM] = "A software component, service or abstract entity that will be the subject of conformance testing."
    this.communityService.getCommunityLabels(this.communityId)
    .subscribe((data) => {
      const labelMap = this.dataService.createLabels(data)
      this.labels.push(labelMap[Constants.LABEL_TYPE.DOMAIN])
      this.labels.push(labelMap[Constants.LABEL_TYPE.SPECIFICATION])
      this.labels.push(labelMap[Constants.LABEL_TYPE.SPECIFICATION_GROUP])
      this.labels.push(labelMap[Constants.LABEL_TYPE.SPECIFICATION_IN_GROUP])
      this.labels.push(labelMap[Constants.LABEL_TYPE.ACTOR])
      this.labels.push(labelMap[Constants.LABEL_TYPE.ENDPOINT])
      this.labels.push(labelMap[Constants.LABEL_TYPE.ORGANISATION])
      this.labels.push(labelMap[Constants.LABEL_TYPE.SYSTEM])
    }).add(() => {
      this.loaded = true
    })
    this.routingService.communityLabelsBreadcrumbs(this.communityId)
  }

  labelTypeLabel(labelType: number) {
    return Constants.LABEL_TYPE_LABEL[labelType]
  }

  customChecked(event: any, label: TypedLabelConfig) {
    if (event.currentTarget.checked) {
      this.dataService.focus('label-singular-'+label.labelType)
    }
  }

  save() {
    this.busy = true
    const labelsToSave: TypedLabelConfig[] = []
    for (let label of this.labels) {
      if (label.custom) {
        labelsToSave.push({
          labelType: label.labelType,
          singularForm: label.singularForm,
          pluralForm: label.pluralForm,
          fixedCase: label.fixedCase
        })
      }
    }
    this.communityService.setCommunityLabels(this.communityId, labelsToSave)
    .subscribe(() => {
      if (this.dataService.isCommunityAdmin || (this.dataService.isSystemAdmin && this.communityId == Constants.DEFAULT_COMMUNITY_ID)) {
        this.dataService.setupLabels(labelsToSave)
      }
      this.cancel()
      this.popupService.success('Community labels updated.')
    }).add(() => {
      this.busy = false
    })
  }

  cancel() {
    this.routingService.toCommunity(this.communityId)
  }

  saveDisabled() {
    if (this.labels) {
      for (let label of this.labels) {
        if (label.custom && (!this.textProvided(label.singularForm) || !this.textProvided(label.pluralForm))) {
          return true
        }
      }
    }
    return false
  }

}
