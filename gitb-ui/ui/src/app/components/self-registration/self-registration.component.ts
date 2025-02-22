import { Component, EventEmitter, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { CommunityService } from 'src/app/services/community.service';
import { DataService } from 'src/app/services/data.service';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { SelfRegistrationModel } from 'src/app/types/self-registration-model.type';
import { SelfRegistrationOption } from 'src/app/types/self-registration-option.type';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
  selector: 'app-self-registration',
  templateUrl: './self-registration.component.html',
  styles: [
    '.self-reg-option-table.is-invalid { border-color: var(--bs-form-invalid-color); }'
  ]
})
export class SelfRegistrationComponent extends BaseComponent implements OnInit {

  @Input() model: SelfRegistrationModel = {}
  @Input() selfRegOptions?: SelfRegistrationOption[]
  @Input() validation?: ValidationState
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

  constructor(
    public dataService: DataService,
    private communityService: CommunityService
  ) { super() }

  ngOnInit(): void {
    this.sso = this.dataService.configuration.ssoEnabled
    this.dataService.setupLabels()
    if (this.selfRegOptions === undefined) {
      this.communityService.getSelfRegistrationOptions().subscribe((data) => {
        this.selfRegOptions = data
        if (data.length == 1) {
          this.communitySelected(data[0])
        }
      })
    } else {
      if (this.selfRegOptions.length == 1) {
        this.communitySelected(this.selfRegOptions[0])
      }
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
