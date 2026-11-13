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

import {CdkDragDrop} from '@angular/cdk/drag-drop';
import {Component, Input, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {Utils} from 'src/app/common/utils';
import {DataService} from 'src/app/services/data.service';
import {Parameter} from 'src/app/types/parameter';
import {ParameterReference} from 'src/app/types/parameter-reference';
import {ValidationState} from 'src/app/types/validation-state';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-parameter-form',
    templateUrl: './parameter-form.component.html',
    styles: [],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ParameterFormComponent implements OnInit {

  @Input() nameLabel!: string
  @Input() parameter!: Partial<Parameter>
  @Input() hasKey!: boolean
  @Input() hideInExport!: boolean
  @Input() hideInRegistration!: boolean
  @Input() existingValues!: ParameterReference[]
  @Input() validation!: ValidationState

  Constants = Constants
  Utils = Utils
  defaultValues: string[] = []
  readonly defaultValueTooltip = 'The default value to assign for this property for new instances if no value is provided.'

  dependsOnTargets: ParameterReference[] = []
  dependsOnTargetsMap: {[key: string]: ParameterReference} = {}
  draggingPreset = false

  constructor(
    public readonly dataService: DataService
  ) { }

  ngOnInit(): void {
		for (let v of this.existingValues) {
      if ((this.parameter.id == undefined || this.parameter.id != v.id) && Utils.isSimpleLikeKind(v.kind)) {
        this.dependsOnTargets.push(v)
        this.dependsOnTargetsMap[v.key] = v
      }
    }
    this.defaultValues = Utils.checkboxPropertyValues(this.parameter.defaultValue)
  }

  /**
   * Radio buttons always have preset values.
   */
  radioButtonSelected() {
    this.parameter.hasPresetValues = true
    if (this.parameter.presetValues == undefined) {
      this.parameter.presetValues = []
    }
    if (this.parameter.presetValues.length == 0) {
      this.addPresetValue()
    }
  }

  presetValuesTooltip(): string {
    if (this.parameter.kind == 'RADIO_BUTTON') {
      return 'The values to select from. The configured values will be displayed in the defined order as radio buttons.'
    } else if (this.parameter.kind == 'CHECKBOX') {
      return 'Check this to define a set of values to select from. The configured values will be displayed in the defined order as checkboxes and one or more may be selected. If no values are defined the property is a single checkbox that is true if checked and false otherwise.'
    } else {
      return 'Check this to define a set of allowed values for this property. The configured values will be displayed in the defined order as a single-selection dropdown list.'
    }
  }

  /**
   * A single checkbox (a checkbox property without preset values) has a true/false value.
   */
  get loneCheckbox(): boolean {
    return this.parameter.kind == 'CHECKBOX' && !this.parameter.hasPresetValues
  }

  loneCheckboxDefaultChanged(checked: boolean) {
    this.parameter.defaultValue = checked ? 'true' : undefined
  }

  get defaultValuesListSize(): number {
    return Math.min(Math.max(this.parameter.presetValues?.length ?? 0, 2), 5)
  }

  get defaultValueTooltipText(): string {
    if (this.parameter.kind == 'CHECKBOX' && this.parameter.hasPresetValues) {
      return this.defaultValueTooltip + ' Use Ctrl (or Cmd) and click to select several values or to clear the selection.'
    }
    return this.defaultValueTooltip
  }

  checkboxDefaultsChanged(values: string[]) {
    this.defaultValues = values
    this.parameter.defaultValue = values.length > 0 ? values.join(',') : undefined
  }

  /**
   * A single checkbox target is expected to be true or false.
   */
  isLoneCheckboxTarget(target: ParameterReference): boolean {
    return target.kind == 'CHECKBOX' && !target.hasPresetValues
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

  mouseDownOnDragButton(pop: NgbTooltip) {
    pop.close()
    pop.disableTooltip = true
  }

  mouseOutOnDragButton(pop: NgbTooltip) {
    setTimeout(() => {
      pop.disableTooltip = false
    }, 50)
  }

}
