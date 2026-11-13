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

import { Component, EventEmitter, Input, OnInit, Output, ChangeDetectionStrategy } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { FileData } from 'src/app/types/file-data.type';
import { saveAs } from 'file-saver'
import { OrganisationService } from 'src/app/services/organisation.service';
import { SystemService } from 'src/app/services/system.service';
import { Observable } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ValidationState } from 'src/app/types/validation-state';
import { Utils } from 'src/app/common/utils';

@Component({
    selector: 'app-custom-property-form',
    templateUrl: './custom-property-form.component.html',
    styleUrls: ['./custom-property-form.component.less'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class CustomPropertyFormComponent implements OnInit {

  @Input() tbProperties?:CustomProperty[]
  @Input() tbReadonly = false
  @Input() tbForceEditable = false
  @Input() tbShowFormHeader = true
  @Input() tbShowRequiredAsterisks = true
  @Input() tbAdmin?: boolean
  @Input() tbSelfRegistration: boolean = false
  @Input() tbPropertyType!: 'organisation'|'system'|'statement'
  @Input() tbOwner?: number
  @Input() tbExpandable = false
  @Input() tbCollapsed: boolean|undefined
  @Input() tbSetDefaults = false
  @Input() tbDocumentation?: string
  @Input() refresh?: EventEmitter<{props?: CustomProperty[], asterisks: boolean}>
  @Input() validation?: ValidationState
  @Input() fieldClass? = 'col-8 col-md-6'
  @Output() collapseChange = new EventEmitter<boolean>()

  Constants = Constants
  isAdmin = false
  isReadonly = true
  private hasPrerequisites = false
  private propertyMap: Record<string, CustomProperty> = {}
  resetMap: Record<number, EventEmitter<void>> = {}
  editorFocus: Record<number, boolean> = {}
  private propertiesInvolvedInPrerequisites: string[] = []
  private propertiesInvolvedInPrerequisitesMap: Record<string, boolean> = {}

  constructor(
    private readonly dataService: DataService,
    private readonly organisationService: OrganisationService,
    private readonly systemService: SystemService
  ) { }

  ngOnInit(): void {
    if (this.tbExpandable) {
      if (this.tbCollapsed == undefined) {
        this.tbCollapsed = true
      }
    } else {
      this.tbCollapsed = false
    }
    if (this.tbAdmin === undefined) {
      this.isAdmin = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
    } else {
      this.isAdmin = this.tbAdmin
    }
    if (this.tbForceEditable) {
      this.isReadonly = false
    } else {
      if (this.isAdmin) {
        this.isReadonly = this.tbReadonly
      } else {
        this.isReadonly = this.tbReadonly || this.dataService.isVendorUser
      }
    }
    if (this.tbProperties) {
      for (let prop of this.tbProperties) {
        if (this.tbSetDefaults && this.tbProperties != undefined && Utils.isSimpleLikeKind(prop.kind)) {
          if (prop.defaultValue != undefined) {
            prop.value = prop.defaultValue
          }
        }
        if (prop.kind == "BINARY") {
          this.resetMap[prop.id] = new EventEmitter<void>()
        }
      }
    }
    if (this.refresh) {
      this.refresh.subscribe((newValues) => {
        this.tbProperties = newValues.props
        this.tbShowRequiredAsterisks = newValues.asterisks
        this.init()
      })
    }
    this.init()
  }

  checkPrerequisite(property: CustomProperty): boolean {
    if (!property.checkedPrerequisites) {
      if (property.dependsOn) {
        const target = this.propertyMap[property.dependsOn]
        property.prerequisiteOk = this.checkPrerequisite(target) && this.prerequisiteValueMatches(target, property.dependsOnValue)
      } else {
        property.prerequisiteOk = true
      }
      property.checkedPrerequisites = true
    }
    return property.prerequisiteOk!
  }

  /**
   * An exact match of the property's value, except for checkboxes: if these have options any one of the selected
   * values may match, and if they don't (a single checkbox) the value is true or false (false if not checked).
   */
  private prerequisiteValueMatches(target: CustomProperty, expectedValue: string|undefined): boolean {
    if (target.kind == 'CHECKBOX') {
      if (Utils.isLoneCheckboxProperty(target.kind, target.allowedValues)) {
        return (target.value ?? 'false') == expectedValue
      } else {
        return Utils.checkboxPropertyValues(target.value).some(value => value == expectedValue)
      }
    }
    return target.value == expectedValue
  }

  isLoneCheckbox(property: CustomProperty): boolean {
    return Utils.isLoneCheckboxProperty(property.kind, property.allowedValues)
  }

  radioChanged(property: CustomProperty, value: string|undefined) {
    property.value = value
    this.checkPrerequisites(property, true)
  }

  checkboxChanged(property: CustomProperty, values: string[]) {
    property.selectedValues = values
    property.value = values.length > 0 ? values.join(',') : undefined
    this.checkPrerequisites(property, true)
  }

  loneCheckboxChanged(property: CustomProperty, checked: boolean) {
    property.checked = checked
    property.value = checked ? 'true' : 'false'
    this.checkPrerequisites(property, true)
  }

  checkPrerequisites(property?: CustomProperty, callInit?: boolean): void {
    if (property === undefined || this.propertiesInvolvedInPrerequisitesMap[property.testKey] !== undefined) {
      if (this.hasPrerequisites) {
        for (let propertyKey of this.propertiesInvolvedInPrerequisites) {
          this.propertyMap[propertyKey].checkedPrerequisites = undefined
        }
        for (let propertyKey of this.propertiesInvolvedInPrerequisites) {
          this.checkPrerequisite(this.propertyMap[propertyKey])
        }
      }
    }
    if (callInit != undefined && callInit) {
      this.init();
    }
  }

  presetValueLabel(property: CustomProperty): string|undefined {
    if (property.kind == 'CHECKBOX' && property.presetValues) {
      // The labels of all selected values.
      return Utils.checkboxPropertyValues(property.value)
        .map(selected => property.presetValues!.find(v => v.value == selected)?.label ?? selected)
        .join(', ')
    }
    if (property.presetValues) {
      for (let v of property.presetValues) {
        if (v.value == property.value)
          if (v.label === undefined) {
            return v.value
          } else {
            return v.label
          }
      }
    }
    return property.value
  }

  hasVisibleProperties(): boolean {
    let result = false
    if (this.hasPrerequisites) {
      this.checkPrerequisites()
    }
    if (this.tbProperties !== undefined && this.tbProperties.length > 0) {
        for (let prop of this.tbProperties) {
          if (prop.prerequisiteOk && (this.isAdmin || !prop.hidden)) {
            result = true
          }
        }
    }
    return result
  }

  removeFile(property: CustomProperty)  {
    delete property.value
    delete property.file
    property.configured = false
    this.checkPrerequisites(property)
    if (this.resetMap[property.id]) {
      this.resetMap[property.id].emit()
    }
  }

  onFileSelect(property: CustomProperty, file: FileData): void {
    property.file = file
    property.value = "PATH"
    this.checkPrerequisites(property)
  }

  codeEditorFocus(focused: boolean, propertyId: number) {
    this.editorFocus[propertyId] = focused
  }

  fileName(property: CustomProperty): string {
    let name = ''
    if (property.file !== undefined) {
      name = property.file.name
    } else {
      if (property.value !== undefined) {
        const extension = this.dataService.extensionFromMimeType(property.mimeType)
        name = property.testKey + extension
      }
    }
    return name
  }

  downloadProperty(property: CustomProperty): void {
    if (property.file !== undefined) {
      saveAs(property.file.file!, property.file.name)
    } else {
      let fn: Observable<ArrayBuffer>
      if (this.tbPropertyType == 'organisation') {
        fn = this.organisationService.downloadOrganisationParameterFile(this.tbOwner!, property.id)
      } else if (this.tbPropertyType == 'system') {
        fn = this.systemService.downloadSystemParameterFile(this.tbOwner!, property.id)
      } else {
        fn = this.systemService.downloadEndpointConfigurationFile(this.tbOwner!, property.id)
      }
      fn.subscribe((data) => {
        const blobData = new Blob([data], {type: property.mimeType})
        const extension = this.dataService.extensionFromMimeType(property.mimeType)
        saveAs(blobData, property.testKey+extension)
      })
    }
  }

  init() {
    this.hasPrerequisites = false
    if (this.tbProperties) {
      this.propertyMap = {}
      this.propertiesInvolvedInPrerequisites = []
      this.propertiesInvolvedInPrerequisitesMap = {}
      for (let property of this.tbProperties) {
        this.propertyMap[property.testKey] = property
        if (property.dependsOn != undefined) {
          if (this.propertiesInvolvedInPrerequisitesMap[property.dependsOn] == undefined) {
            this.propertiesInvolvedInPrerequisites.push(property.dependsOn)
            this.propertiesInvolvedInPrerequisitesMap[property.dependsOn] = true
          }
          if (this.propertiesInvolvedInPrerequisitesMap[property.testKey] == undefined) {
            this.propertiesInvolvedInPrerequisites.push(property.testKey)
            this.propertiesInvolvedInPrerequisitesMap[property.testKey] = true
          }
          property.prerequisiteOk = false
        } else {
          property.prerequisiteOk = true
        }
        if (Utils.isSimpleLikeKind(property.kind)) {
          property.hasPresetValues = false
          if (property.allowedValues !== undefined) {
            property.presetValues = JSON.parse(property.allowedValues)
            if (property.presetValues && property.presetValues.length > 0) {
              property.hasPresetValues = true
            }
          }
          if (property.kind == 'CHECKBOX') {
            if (property.hasPresetValues) {
              property.selectedValues = Utils.checkboxPropertyValues(property.value)
            } else {
              property.checked = property.value == 'true'
            }
          }
        }
      }
      this.hasPrerequisites = this.propertiesInvolvedInPrerequisites.length > 0
    }
  }

  checkToExpand() {
    if (this.tbExpandable) {
      this.tbCollapsed = !this.tbCollapsed
      this.collapseChange.emit(this.tbCollapsed)
    }
  }
}
