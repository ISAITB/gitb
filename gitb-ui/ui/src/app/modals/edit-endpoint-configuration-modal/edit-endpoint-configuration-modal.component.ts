import { AfterViewChecked, Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { ConformanceConfiguration } from 'src/app/pages/organisation/conformance-statement/conformance-configuration';
import { ConformanceEndpoint } from 'src/app/pages/organisation/conformance-statement/conformance-endpoint';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { SystemService } from 'src/app/services/system.service';
import { FileData } from 'src/app/types/file-data.type';
import { SystemConfigurationParameter } from 'src/app/types/system-configuration-parameter';
import { cloneDeep } from 'lodash'
import { ParameterPresetValue } from 'src/app/types/parameter-preset-value';
import { ErrorService } from 'src/app/services/error.service';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-edit-endpoint-configuration-modal',
  templateUrl: './edit-endpoint-configuration-modal.component.html',
  styles: [
  ]
})
export class EditEndpointConfigurationModalComponent extends BaseComponent implements OnInit, AfterViewChecked {

  @Input() endpoint!: ConformanceEndpoint
  @Input() parameter!: SystemConfigurationParameter
  @Input() systemId!: number
  @Input() oldConfiguration?: ConformanceConfiguration
  @Output() action = new EventEmitter<{operation: number, configuration: ConformanceConfiguration}>()

  file?: FileData
  configuration!: ConformanceConfiguration
  hasPresetValues = false
  presetValues?: ParameterPresetValue[]
  isBinary = false
  isConfigurationSet = false
  deleted = false
  savePending = false
  deletePending = false    

  @ViewChild("value") valueField?: ElementRef;

  constructor(
    private modalRef: BsModalRef,
    private confirmationDialogService: ConfirmationDialogService,
    private systemService: SystemService,
    private dataService: DataService,
    private popupService: PopupService,
    private errorService: ErrorService
  ) { super() }

  ngAfterViewChecked(): void {
    if (!this.isBinary) {
      this.valueField?.nativeElement.focus()
    }
  }

  ngOnInit(): void {
    if (this.oldConfiguration == undefined) {
      this.configuration = {
        system: this.systemId,
        endpoint: this.endpoint.id,
        parameter: this.parameter.id,
        configured: false
      }
    } else {
      this.configuration = cloneDeep(this.oldConfiguration)
    }
    if (this.parameter.allowedValues != undefined) {
      this.presetValues = JSON.parse(this.parameter.allowedValues)
      if (this.presetValues != undefined && this.presetValues.length > 0) {
        this.hasPresetValues = true
      }
    }
    this.isBinary = this.parameter.kind == "BINARY"
    this.isConfigurationSet = this.configuration.configured
  }

  onFileSelect(file: FileData) {
    if (file != undefined) {
      if (file.size >= (Number(this.dataService.configuration.savedFileMaxSize) * 1024)) {
        this.errorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for files is '+this.dataService.configuration.savedFileMaxSize+' KBs.')
      } else {
        this.file = file
      }
    }
  }

  showFileName() {
    return this.file != undefined || this.configuration?.configured
  }

  fileName() {
    let name = ''
    if (this.file != undefined) {
      name = this.file.name
    } else {
      if (this.configuration?.configured) {
        const extension = this.dataService.extensionFromMimeType(this.parameter.mimeType)
        name = this.parameter.testKey + extension
      }
    }
    return name
  }

  download() {
    if (this.file == undefined) {
      this.systemService.downloadEndpointConfigurationFile(this.systemId, this.parameter.id, this.parameter.endpoint)
      .subscribe((data) => {
        const blobData = new Blob([data], {type: this.parameter.mimeType})
        saveAs(blobData, this.fileName())
      })
    } else {
      saveAs(this.file.file!, this.fileName())
    }
  }

  closeDialog() {
    if (this.deleted) {
      this.close(Constants.OPERATION.DELETE)
    } else {
      if (this.oldConfiguration == undefined) {
        this.close(Constants.OPERATION.ADD)
      } else {
        this.close(Constants.OPERATION.UPDATE)
      }
    }
  }

  private close(operation?: number) {
    if (operation != undefined) {
      this.action.emit({operation: operation, configuration: this.configuration})
    }
    this.modalRef.hide()
  }

  delete() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete the value for this parameter?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.systemService.deleteEndpointConfiguration(this.systemId, this.parameter.id, this.endpoint.id)
      .subscribe(() => {
        this.deleted = true
        this.closeDialog()
        this.popupService.success('Parameter deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancel() {
    this.close()
  }

  save() {
    this.configuration.configured = true
    if (this.parameter.kind == "SIMPLE") {
      if (this.configuration.value == undefined) {
        this.configuration.value = ''
      }
      this.savePending = true
      this.systemService.saveEndpointConfiguration(this.endpoint.id, this.configuration)
      .subscribe(() => {
        this.closeDialog()
        this.popupService.success('Parameter updated.')
      }).add(() => { this.savePending = false })
    } else if (this.parameter.kind == "SECRET") {
      let configurationValue = ''
      if (this.configuration.value != undefined) {
        configurationValue = this.configuration.value
      }
      let configurationValueConfirm = ''
      if (this.configuration.valueConfirm != undefined) {
        configurationValueConfirm = this.configuration.valueConfirm
      }
      if (configurationValue != configurationValueConfirm) {
        this.errorService.showSimpleErrorMessage('Invalid value', 'The provided value and its confirmation don\'t match.')
      } else {
        this.savePending = true
        this.systemService.saveEndpointConfiguration(this.endpoint.id, this.configuration)
        .subscribe(() => {
          this.configuration.value = undefined
          this.configuration.valueConfirm = undefined
          this.closeDialog()
          this.popupService.success('Parameter updated.')
        }).add(() => { this.savePending = false })
      }
    } else if (this.parameter.kind == "BINARY") {
      this.configuration.value = ''
      this.savePending = true
      this.systemService.saveEndpointConfiguration(this.endpoint.id, this.configuration, this.file?.file)
      .subscribe((metadata) => {
        if (metadata != undefined) {
          this.configuration.mimeType = metadata.mimeType
        }
        this.closeDialog()
        this.popupService.success('Parameter updated.')
      }).add(() => { this.savePending = false })
    }
  }
  
  saveDisabled() {
    return this.deletePending 
      || (this.parameter.kind == 'BINARY' && this.file == undefined && this.configuration.configured)
      || (this.parameter.kind == 'SIMPLE' && !this.textProvided(this.configuration.value))
      || (this.parameter.kind == 'SECRET' && (!this.textProvided(this.configuration.value) || !this.textProvided(this.configuration.valueConfirm)))
  }

}
