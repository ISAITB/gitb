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

import {AfterViewInit, Component, EventEmitter, Input, OnInit, ViewChild} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {CommunityService} from 'src/app/services/community.service';
import {DataService} from 'src/app/services/data.service';
import {CustomProperty} from 'src/app/types/custom-property.type';
import {SelfRegistrationModel} from 'src/app/types/self-registration-model.type';
import {SelfRegistrationOption} from 'src/app/types/self-registration-option.type';
import {TableColumnDefinition} from 'src/app/types/table-column-definition.type';
import {ValidationState} from 'src/app/types/validation-state';
import {TableComponent} from '../table/table.component';

@Component({
    selector: 'app-self-registration',
    templateUrl: './self-registration.component.html',
    styles: [
        '.self-reg-option-table.is-invalid { border-color: var(--bs-form-invalid-color); }'
    ],
    standalone: false
})
export class SelfRegistrationComponent extends BaseComponent implements OnInit, AfterViewInit {

  @Input() model: SelfRegistrationModel = {}
  @Input() selfRegOptions?: SelfRegistrationOption[]
  @Input() validation?: ValidationState
  @ViewChild('communityTable') communityTable?: TableComponent;

  sso: boolean = false
  templateReadonly = false
  communityColumns: TableColumnDefinition[] = [
    {
      field: 'communityName',
      title: 'Name'
    },
    {
      field: 'communityDescription',
      title: 'Description'
    }
  ]
  refreshSignal = new EventEmitter<{props?: CustomProperty[], asterisks: boolean}>()
  private viewReady = false
  private dataReady = false

  constructor(
    public readonly dataService: DataService,
    private readonly communityService: CommunityService
  ) { super() }

  ngOnInit(): void {
    this.sso = this.dataService.configuration.ssoEnabled
    this.dataService.setupLabels()
    if (this.selfRegOptions === undefined) {
      this.communityService.getSelfRegistrationOptions().subscribe((data) => {
        this.selfRegOptions = data
        this.dataReady = true
        this.tryToSelectCommunity()
      })
    } else {
      this.dataReady = true
      this.tryToSelectCommunity()
    }
  }

  ngAfterViewInit() {
    this.viewReady = true
    this.tryToSelectCommunity()
  }

  tryToSelectCommunity() {
    if (this.dataReady && this.viewReady && this.selfRegOptions != undefined && this.selfRegOptions.length == 1) {
      setTimeout(() => {
        if (this.communityTable) {
          this.communityTable.select(0)
        }
      })
    }
  }

  adaptTemplateStatus() {
    if (this.model?.selfRegOption?.forceTemplateSelection && this.model?.selfRegOption?.templates?.length == 1) {
      this.model.template = this.model.selfRegOption.templates[0]
      this.templateReadonly = true
    } else {
      this.model.template = undefined
      this.templateReadonly = false
    }
  }

  communitySelected(option: SelfRegistrationOption) {
    this.model!.selfRegOption = option
    this.communityChanged()
  }

  setFormFocus() {
    if (this.model?.selfRegOption !== undefined) {
      if (this.model.selfRegOption.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN) {
        this.dataService.focus('token', 200)
      } else {
        this.dataService.focus('orgShortName', 200)
      }
    }
  }

  communityChanged() {
    if (this.model?.selfRegOption) {
      this.validation?.clear('community')
      this.dataService.setupLabels(this.model.selfRegOption.labels)
      this.adaptTemplateStatus()
      this.setFormFocus()
      this.refreshSignal.emit({props: this.model.selfRegOption.organisationProperties, asterisks: this.model.selfRegOption.forceRequiredProperties})
    }
  }

  optionRowStyle() {
    if (this.selfRegOptions === undefined || this.selfRegOptions.length > 1) {
      return ""
    } else {
      return "selected"
    }
  }

}
