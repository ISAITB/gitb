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
import {DataService} from 'src/app/services/data.service';
import {Parameter} from 'src/app/types/parameter';
import {BaseParameterModalComponent} from '../base-parameter-modal.component';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-create-parameter-modal',
    templateUrl: './create-parameter-modal.component.html',
    styles: [],
    standalone: false
})
export class CreateParameterModalComponent extends BaseParameterModalComponent implements OnInit {

  modalTitle: string = 'Create parameter'

  constructor(
    dataService: DataService,
    modalInstance: NgbActiveModal
  ) { super(dataService, modalInstance) }

  ngOnInit(): void {
    this.parameter = {
      use: 'O',
      kind: 'SIMPLE',
      notForTests: this.options.notForTests != undefined && this.options.notForTests,
      adminOnly: this.options.adminOnly != undefined && this.options.adminOnly,
      inExports: false,
      inSelfRegistration: false,
      hidden: false,
      hasPresetValues: false,
      presetValues: []
    }
    super.onInit(this.options)
  }

  createParameter() {
    if (this.validate()) {
      this.modalInstance.close(this.parameter as Parameter)
    }
  }

}
