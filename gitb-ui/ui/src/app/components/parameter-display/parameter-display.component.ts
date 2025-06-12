import {Component, Input, OnInit} from '@angular/core';
import {DataService} from 'src/app/services/data.service';
import {filter} from 'lodash';
import {Parameter} from 'src/app/types/parameter';
import {Constants} from 'src/app/common/constants';

@Component({
    selector: 'app-parameter-display',
    templateUrl: './parameter-display.component.html',
    styleUrl: './parameter-display.component.less',
    standalone: false
})
export class ParameterDisplayComponent<T extends Parameter> implements OnInit {

  @Input() title!: string;
  @Input() noMargin = false;
  @Input() parameters!: T[]
  @Input() parameterLabel = 'Property'

  Constants = Constants

  isAdmin!: boolean
  collapsed = false
  hovering = false

  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.isAdmin = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
    if (this.parameters != undefined) {
      this.parameters = filter(this.parameters, (parameter) => {
        return ((parameter.configured == undefined || !parameter.configured)) && (this.isAdmin || !parameter.hidden) && (parameter.prerequisiteOk == undefined || parameter.prerequisiteOk) && this.isRequired(parameter)
      })
    }
  }

  isRequired(parameter: T) {
    return parameter.use == 'R'
  }

}
