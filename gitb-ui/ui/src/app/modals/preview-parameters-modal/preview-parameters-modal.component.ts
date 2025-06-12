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
    public dataService: DataService,
    private modalRef: BsModalRef
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
