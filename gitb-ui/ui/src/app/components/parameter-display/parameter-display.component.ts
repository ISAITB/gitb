import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { filter } from 'lodash'
import { Parameter } from 'src/app/types/parameter';
import { Constants } from 'src/app/common/constants';

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
  @Input() onlyRequired = false
  @Input() editable = false
  @Input() canEdit?: (p: T) => boolean
  @Input() parameterLabel = 'Property'
  @Input() styleAsNestedTable = false
  @Output() edit = new EventEmitter<T>()
  @Output() download = new EventEmitter<T>()

  Constants = Constants

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
          && (!this.onlyRequired || this.isRequired(parameter))
      })
    }
  }

  isRequired(parameter: T) {
    return parameter.use == 'R'
  }

  downloadBinaryParameter(parameter: T) {
    this.download.emit(parameter)
  }

  onEdit(parameter: T) {
    this.edit.emit(parameter)
  }

}
