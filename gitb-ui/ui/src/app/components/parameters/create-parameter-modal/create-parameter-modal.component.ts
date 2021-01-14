import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { Parameter } from 'src/app/types/parameter';
import { ErrorService } from 'src/app/services/error.service';
import { BaseParameterModalComponent } from '../base-parameter-modal.component';

@Component({
  selector: 'app-create-parameter-modal',
  templateUrl: './create-parameter-modal.component.html',
  styles: [
  ]
})
export class CreateParameterModalComponent extends BaseParameterModalComponent implements OnInit {

  @Output() created = new EventEmitter<Parameter>()

  modalTitle: string = 'Create parameter'

  constructor(
    dataService: DataService,
    errorService: ErrorService,
    modalInstance: BsModalRef
  ) { super(dataService, errorService, modalInstance) }

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
    if (!this.saveDisabled() && this.validName(this.parameter.name!) && this.validKey(this.parameter.testKey!)) {
      this.created.emit(this.parameter as Parameter)
      this.modalInstance.hide()
    }
  }

}
