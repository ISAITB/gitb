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

import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { ParameterReference } from 'src/app/types/parameter-reference';
import { ParameterModalOptions } from './parameter-modal-options';
import { find } from 'lodash'
import { Parameter } from 'src/app/types/parameter';
import { Constants } from 'src/app/common/constants';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Component, Input } from '@angular/core';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseParameterModalComponent extends BaseComponent {

  @Input() parameter!: Partial<Parameter>
  @Input() options!: ParameterModalOptions

  nameLabel = 'Name'
  hasKey = false
  modalTitle!: string
  existingValues!: ParameterReference[]
  reservedKeys?: string[]
  hideInExport = false
  hideInRegistration!: boolean
  validation = new ValidationState()

  constructor(
    private readonly dataService: DataService,
    protected readonly modalInstance: BsModalRef
  ) { super() }

  protected onInit(options: ParameterModalOptions) {
    if (options.nameLabel != undefined) this.nameLabel = options.nameLabel
    if (options.hasKey != undefined) this.hasKey = options.hasKey
    if (options.modalTitle != undefined) this.modalTitle = options.modalTitle
    this.existingValues = options.existingValues
		this.reservedKeys = options.reservedKeys
    if (options.hideInExport != undefined) this.hideInExport = options.hideInExport
		this.hideInRegistration = !this.dataService.configuration.registrationEnabled || (options.hideInRegistration != undefined && options.hideInRegistration)
  }

  saveDisabled() {
    return !(this.textProvided(this.parameter.name)
      && this.textProvided(this.parameter.kind)
      && (!this.hasKey || this.textProvided(this.parameter.testKey))
      && this.presetValuesValid()
      && this.dependencyValid()
    )
  }

	presetValuesValid() {
		if (this.parameter.kind == 'SIMPLE' && this.parameter.hasPresetValues) {
      if (this.parameter.presetValues == undefined || this.parameter.presetValues.length == 0) {
        return false
      } else {
        for (let v of this.parameter.presetValues!) {
          if (!this.textProvided(v.value) || !this.textProvided(v.label)) {
            return false
          }
        }
      }
    }
		return true
  }

	dependencyValid() {
    return !this.textProvided(this.parameter.dependsOn) || this.textProvided(this.parameter.dependsOnValue)
  }

	validName(nameValue: string) {
		const finder = (value: string) => {
			return find(this.existingValues, (v) => {
				return (this.parameter.id == undefined || this.parameter.id != v.id) && v.name == value
      })
    }
		if (this.existingValues != undefined && finder.bind(this)(nameValue)) {
      this.validation.invalid('name', 'The provided name is already defined.')
			return false
    } else {
			return true
    }
  }

	validKey(keyValue: string) {
		let result = false
		const finder = (value: string) => {
			return find(this.existingValues, (v) => {
        return (this.parameter.id == undefined || this.parameter.id != v.id) && v.key == value
      })
    }
		const finderReserved = (value: string) => {
			return find(this.reservedKeys, (v) => {
				return v == value
      })
    }
		if (this.hasKey) {
			if (this.existingValues != undefined && finder.bind(this)(keyValue)) {
        this.validation.invalid('key', 'The provided key is already defined.')
      } else if (this.reservedKeys != undefined && finderReserved.bind(this)(keyValue)) {
        this.validation.invalid('key', 'The provided key is reserved.')
      } else if (!Constants.VARIABLE_NAME_REGEX.test(keyValue)) {
        this.validation.invalid('key', 'The provided key is invalid. A key must begin with a character followed by zero or more characters, digits, or one of [\'.\', \'_\', \'-\'].')
      } else {
				result = true
      }
    } else {
			result = true
    }
		return result
  }

  cancel() {
    this.modalInstance.hide()
  }

  validate() {
    this.validation.clearErrors()
    if (!this.saveDisabled()) {
      const validName = this.validName(this.parameter.name!)
      const validKey = this.validKey(this.parameter.testKey!)
      return validName && validKey
    }
    return false
  }

}
