import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { ParameterReference } from 'src/app/types/parameter-reference';
import { ParameterModalOptions } from './parameter-modal-options';
import { find } from 'lodash'
import { ErrorService } from 'src/app/services/error.service';
import { Parameter } from 'src/app/types/parameter';
import { Constants } from 'src/app/common/constants';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Component, Input } from '@angular/core';

@Component({ template: '' })
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

  constructor(
    private dataService: DataService, 
    private errorService: ErrorService,
    protected modalInstance: BsModalRef
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
			const nameToShow = this.nameLabel.toLowerCase()
			this.errorService.showSimpleErrorMessage('Invalid '+nameToShow, 'The provided '+nameToShow+' is already defined.')
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
				this.errorService.showSimpleErrorMessage('Invalid key', 'The provided key is already defined.')
      } else if (this.reservedKeys != undefined && finderReserved.bind(this)(keyValue)) {
				this.errorService.showSimpleErrorMessage('Invalid key', 'The provided key is reserved.')
      } else if (!Constants.VARIABLE_NAME_REGEX.test(keyValue)) {
				this.errorService.showSimpleErrorMessage('Invalid key', 'The provided key is invalid. A key must begin with a character followed by zero or more characters, digits, or one of [\'.\', \'_\', \'-\'].')
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

}
