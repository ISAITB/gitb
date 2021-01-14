import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { filter } from 'lodash'
import { Parameter } from 'src/app/types/parameter';

@Component({
  selector: 'app-parameter-display',
  templateUrl: './parameter-display.component.html',
  styles: [
  ]
})
export class ParameterDisplayComponent<T extends Parameter> implements OnInit {

  @Input() parameters!: T[]
  @Input() showValues = false
  @Input() onlyMissing = false
  @Input() editable = false
  @Input() canEdit?: (p: T) => boolean
  @Input() parameterLabel = 'Parameter'
  @Output() edit = new EventEmitter<T>()

  isAdmin!: boolean
  showActions = false

  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.isAdmin = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
    if (this.editable && this.parameters != undefined) {
      for (let param of this.parameters) {
        if (this.canEdit != undefined && this.canEdit(param)) {
          this.showActions = true
        }
      }
    }
    if (this.parameters != undefined) {
      this.parameters = filter(this.parameters, (parameter) => {
        return (!this.onlyMissing || (parameter.configured == undefined || !parameter.configured)) && (this.isAdmin || !parameter.hidden) && (parameter.prerequisiteOk == undefined || parameter.prerequisiteOk)
      })
    }
  }

  isRequired(parameter: T) {
    return parameter.use == 'R'
  }

  downloadBinaryParameter(parameter: T) {
    const blob = this.dataService.b64toBlob(this.dataService.base64FromDataURL(parameter.value!), parameter.mimeType)
    saveAs(blob, parameter.fileName)
  }

  onEdit(parameter: T) {
    this.edit.emit(parameter)
  }

}
