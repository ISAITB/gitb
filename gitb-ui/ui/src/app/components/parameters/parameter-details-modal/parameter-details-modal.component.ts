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

import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Parameter } from 'src/app/types/parameter';
import { cloneDeep } from 'lodash';
import { BaseParameterModalComponent } from '../base-parameter-modal.component';
import { DataService } from 'src/app/services/data.service';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';

@Component({
    selector: 'app-parameter-details-modal',
    templateUrl: './parameter-details-modal.component.html',
    styles: [],
    standalone: false
})
export class ParameterDetailsModalComponent extends BaseParameterModalComponent implements OnInit {

  @Output() updated = new EventEmitter<Parameter>()
  @Output() deleted = new EventEmitter<Parameter>()

  confirmMessage = 'Are you sure you want to delete this parameter?'
  modalTitle: string = 'Parameter details'

  constructor(
    dataService: DataService,
    modalInstance: BsModalRef,
    private confirmationDialogService: ConfirmationDialogService
  ) { super(dataService, modalInstance) }

  ngOnInit(): void {
    this.parameter = cloneDeep(this.parameter)
    if (this.options.confirmMessage != undefined) this.confirmMessage = this.options.confirmMessage
		this.parameter.hasPresetValues = this.parameter.allowedValues != undefined
		if (this.parameter.hasPresetValues) {
			this.parameter.presetValues = JSON.parse(this.parameter.allowedValues!)
    }
		if (this.parameter.presetValues == undefined) {
			this.parameter.presetValues = []
    }
    super.onInit(this.options)
  }

  updateParameter() {
    if (this.validate()) {
      this.updated.emit(this.parameter as Parameter)
      this.modalInstance.hide()
    }
  }

  deleteParameter() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", this.confirmMessage, "Delete", "Cancel")
    .subscribe(() => {
      this.validation.clearErrors()
      this.deleted.emit(this.parameter as Parameter)
      this.modalInstance.hide()
    })
  }

}
