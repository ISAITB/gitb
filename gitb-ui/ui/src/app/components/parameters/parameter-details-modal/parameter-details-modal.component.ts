/*
 * Copyright (C) 2026 European Union
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
import {Parameter} from 'src/app/types/parameter';
import {BaseParameterModalComponent} from '../base-parameter-modal.component';
import {DataService} from 'src/app/services/data.service';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {Constants} from '../../../common/constants';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-parameter-details-modal',
    templateUrl: './parameter-details-modal.component.html',
    styles: [],
    standalone: false
})
export class ParameterDetailsModalComponent extends BaseParameterModalComponent implements OnInit {

  confirmMessage = 'Are you sure you want to delete this parameter?'
  modalTitle: string = 'Parameter details'

  constructor(
    dataService: DataService,
    modalInstance: NgbActiveModal,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) { super(dataService, modalInstance) }

  ngOnInit(): void {
    this.parameter = structuredClone(this.parameter)
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
      this.modalInstance.close(this.parameter as Parameter)
    }
  }

  deleteParameter() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", this.confirmMessage, "Delete", "Cancel", Constants.BUTTON_ICON.DELETE)
    .subscribe(() => {
      this.validation.clearErrors()
      this.modalInstance.close()
    })
  }

}
