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

import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { AfterViewInit, Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { Parameter } from 'src/app/types/parameter';
import { ParameterReference } from 'src/app/types/parameter-reference';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-parameter-form',
    templateUrl: './parameter-form.component.html',
    styles: [],
    standalone: false
})
export class ParameterFormComponent implements OnInit, AfterViewInit {

  @Input() nameLabel!: string
  @Input() parameter!: Partial<Parameter>
  @Input() hasKey!: boolean
  @Input() hideInExport!: boolean
  @Input() hideInRegistration!: boolean
  @Input() existingValues!: ParameterReference[]
  @Input() validation!: ValidationState

  Constants = Constants

  dependsOnTargets: ParameterReference[] = []
  dependsOnTargetsMap: {[key: string]: ParameterReference} = {}
  draggingPreset = false

  constructor(
    public readonly dataService: DataService
  ) { }

  ngAfterViewInit(): void {
    this.dataService.focus('parameterName')
  }

  ngOnInit(): void {
		for (let v of this.existingValues) {
      if ((this.parameter.id == undefined || this.parameter.id != v.id) && v.kind == 'SIMPLE') {
        this.dependsOnTargets.push(v)
        this.dependsOnTargetsMap[v.key] = v
      }
    }
  }

  addPresetValue() {
    this.parameter.presetValues!.push({value: '', label:''})
    this.dataService.focus('presetValue-'+(this.parameter.presetValues!.length - 1))
  }

  removePresetValue(index: number) {
    this.parameter.presetValues!.splice(index, 1)
  }

  dependsOnChanged() {
    if (this.parameter.dependsOn == undefined || this.parameter.dependsOn == '') {
      delete this.parameter.dependsOnValue
    }
  }

  dropPreset(event: CdkDragDrop<any>) {
    if (event.currentIndex != event.previousIndex && this.parameter.presetValues) {
      this.parameter.presetValues.splice(event.currentIndex, 0, this.parameter.presetValues.splice(event.previousIndex, 1)[0]);
    }
  }
}
