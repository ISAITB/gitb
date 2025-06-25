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

import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { CustomProperty } from 'src/app/types/custom-property.type';

@Component({
    selector: 'app-preview-parameters-modal',
    templateUrl: './preview-parameters-modal.component.html',
    styles: [],
    standalone: false
})
export class PreviewParametersModalComponent implements OnInit {

  @Input() parameters!: CustomProperty[]
  @Input() parameterType!: 'organisation'|'system'
  @Input() modalTitle!: string
  @Input() hasRegistrationCase!: boolean
  mode = 'user'
  parametersForRegistration: CustomProperty[] = []

  constructor(
    public readonly dataService: DataService,
    private readonly modalRef: BsModalRef
  ) { }

  ngOnInit(): void {
		if (!this.dataService.configuration.registrationEnabled) {
			this.hasRegistrationCase = false
    } else {
			for (let param of this.parameters) {
				if (param.inSelfRegistration) {
					this.parametersForRegistration.push(param)
        }
      }
    }
  }

	hasVisibleProperties() {
		let properties = this.parameters
		if (this.mode == 'registration') {
			properties = this.parametersForRegistration
    }
		let result = false
		if (properties?.length > 0) {
			if (this.mode == 'admin') {
				result = true
      } else {
				for (const prop of properties) {
					if (!prop.hidden) {
						result = true
          }
        }
      }
    }
		return result
  }

	close() {
    this.modalRef.hide()
  }
}
