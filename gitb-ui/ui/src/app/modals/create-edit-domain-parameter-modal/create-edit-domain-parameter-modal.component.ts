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

import { AfterViewInit, Component, EventEmitter, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { DomainParameter } from 'src/app/types/domain-parameter';
import { cloneDeep } from 'lodash'
import { BaseComponent } from 'src/app/pages/base-component.component';
import { FileData } from 'src/app/types/file-data.type';
import { ParameterFormData } from './parameter-form-data';
import { saveAs } from 'file-saver'
import { Constants } from 'src/app/common/constants';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-create-edit-domain-parameter-modal',
    templateUrl: './create-edit-domain-parameter-modal.component.html',
    styles: [],
    standalone: false
})
export class CreateEditDomainParameterModalComponent extends BaseComponent implements OnInit, AfterViewInit {

  @Input() domainParameter!: Partial<DomainParameter>
  @Input() domainId!: number
  public parametersUpdated = new EventEmitter<boolean>()

  pending = false
  savePending = false
  deletePending = false
  initialFileName?: string
  formData: ParameterFormData = {
    showUpdateValue: false,
    updateValue: false
  }
  title!: string
  fileName?: string
  validation = new ValidationState()

  constructor(
    private readonly dataService: DataService,
    private readonly popupService: PopupService,
    private readonly modalInstance: BsModalRef,
    private readonly conformanceService: ConformanceService,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
		this.domainParameter = cloneDeep(this.domainParameter)
		if (this.domainParameter.id == undefined) {
			this.domainParameter.inTests = true
    }
		this.formData.initialKind = this.domainParameter.kind
		this.formData.showUpdateValue = this.domainParameter.id != undefined && this.formData.initialKind == 'HIDDEN'
		this.formData.updateValue = this.formData.initialKind != 'HIDDEN'

		if (this.domainParameter.id != undefined && this.domainParameter.kind == 'BINARY') {
			delete this.domainParameter.value
			const extension = this.dataService.extensionFromMimeType(this.domainParameter.contentType)
			this.initialFileName =  this.domainParameter.name+extension
    }

		if (this.domainParameter.id != undefined) {
			this.title = 'Update parameter'
    } else {
			this.title = 'Create parameter'
    }
  }

  saveAllowed() {
    return this.textProvided(this.domainParameter.name) && this.domainParameter.kind != undefined && (
      (this.domainParameter.kind == 'SIMPLE' && this.textProvided(this.domainParameter.value)) ||
      (this.domainParameter.kind == 'BINARY' && (this.formData.file != undefined || (this.domainParameter.id != undefined && this.initialFileName != undefined))) ||
      (this.domainParameter.kind == 'HIDDEN' && (!this.formData.updateValue || this.textProvided(this.formData.hiddenValue)))
    )
  }

  save() {
    this.validation.clearErrors()
    if (this.saveAllowed()) {
      if (!Constants.VARIABLE_NAME_REGEX.test(this.domainParameter.name!)) {
        this.validation.invalid('name', 'A parameter name must begin with a character followed by zero or more characters, digits, or one of [\'.\', \'_\', \'-\'].')
      } else {
        this.pending = true
        this.savePending = true
        if (this.domainParameter.id != undefined) {
          // Update
          let valueToSave: string|File|undefined
          if (this.domainParameter.kind == 'HIDDEN' && this.formData.updateValue) {
            valueToSave = this.formData.hiddenValue!
          } else if (this.domainParameter.kind == 'BINARY' && this.formData.file != undefined) {
            valueToSave = this.formData.file
          } else if (this.domainParameter.kind == 'SIMPLE') {
            valueToSave = this.domainParameter.value!
          }
          this.conformanceService.updateDomainParameter(this.domainParameter.id, this.domainParameter.name!, this.domainParameter.description, valueToSave, this.domainParameter.kind!, this.domainParameter.inTests, this.domainId)
          .subscribe((data) => {
            if (this.isErrorDescription(data)) {
              this.validation.applyError(data)
            } else {
              this.parametersUpdated.emit(true)
              this.modalInstance.hide()
              this.popupService.success('Parameter updated.')
            }
          }).add(() => {
            this.pending = false
            this.savePending = false
          })
        } else {
          // Create
          let valueToSave: string|File
          if (this.domainParameter.kind == 'HIDDEN') {
            valueToSave = this.formData.hiddenValue!
          } else if (this.domainParameter.kind == 'BINARY') {
            valueToSave = this.formData.file!
          } else {
            valueToSave = this.domainParameter.value!
          }
          this.conformanceService.createDomainParameter(this.domainParameter.name!, this.domainParameter.description, valueToSave, this.domainParameter.kind!, this.domainParameter.inTests, this.domainId)
          .subscribe((data) => {
            if (this.isErrorDescription(data)) {
              this.validation.applyError(data)
            } else {
              this.parametersUpdated.emit(true)
              this.modalInstance.hide()
              this.popupService.success('Parameter created.')
            }
          }).add(() => {
            this.pending = false
            this.savePending = false
          })
        }
      }
    }
  }

  delete() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this parameter?", "Delete", "Cancel")
    .subscribe(() => {
      this.pending = true
      this.deletePending = true
      this.conformanceService.deleteDomainParameter(this.domainParameter.id!, this.domainId)
      .subscribe(() => {
        this.parametersUpdated.emit(true)
        this.modalInstance.hide()
        this.popupService.success('Parameter deleted.')
      }).add(() => {
        this.pending = false
        this.deletePending = false
      })
    })
  }

  cancel() {
    this.parametersUpdated.emit(false)
    this.modalInstance.hide()
  }

  onFileSelect(file: FileData) {
    this.fileName = file.name
    this.formData.file = file.file
  }

  showFileName() {
    return this.fileName != undefined || this.initialFileName != undefined
  }

  getFileName() {
    let name = ''
    if (this.fileName != undefined) {
      name = this.fileName
    } else if (this.initialFileName != undefined) {
      name = this.initialFileName!
    }
    return name
  }

  download() {
    if (this.formData.file) {
      saveAs(this.formData.file, this.getFileName())
    } else {
      this.conformanceService.downloadDomainParameterFile(this.domainId, this.domainParameter.id!)
      .subscribe((data) => {
        const blobData = new Blob([data], {type: this.domainParameter.contentType})
        saveAs(blobData, this.getFileName())
      })
    }
  }

}
