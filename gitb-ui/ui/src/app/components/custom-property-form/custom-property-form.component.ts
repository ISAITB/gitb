import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { FileData } from 'src/app/types/file-data.type';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-custom-property-form',
  templateUrl: './custom-property-form.component.html'
})
export class CustomPropertyFormComponent implements OnInit {

  @Input() tbProperties?:CustomProperty[]
  @Input() tbPopup = false
  @Input() tbColLabel = 3
  @Input() tbColOffset = 1
  @Input() tbColInputLess = 0
  @Input() tbReadonly = false
  @Input() tbFormPadded = true
  @Input() tbShowFormHeader = true
  @Input() tbShowRequiredAsterisks = true
  @Input() tbAdmin?: boolean

  isAdmin = false
  isReadonly = true
  innerDivStyle = ''
  private hasPrerequisites = false
  private propertyMap: Record<string, CustomProperty> = {}
  private propertiesInvolvedInPrerequisites: string[] = []
  private propertiesInvolvedInPrerequisitesMap: Record<string, boolean> = {}

  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    if (this.tbAdmin === undefined) {
      this.isAdmin = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
    } else {
      this.isAdmin = this.tbAdmin
    }
    if (this.isAdmin) {
      this.isReadonly = false
    } else {
      this.isReadonly = this.dataService.isVendorUser || this.tbReadonly
    }
    if (this.tbFormPadded) {
      this.innerDivStyle = 'col-xs-'+(11-this.tbColOffset)+' col-xs-offset-'+this.tbColOffset
    }
    this.init()
  }

  checkPrerequisite(property: CustomProperty): boolean {
    if (!property.checkedPrerequisites) {
      if (property.dependsOn) {
        property.prerequisiteOk = this.checkPrerequisite(this.propertyMap[property.dependsOn]) && this.propertyMap[property.dependsOn].value == property.dependsOnValue
      } else {
        property.prerequisiteOk = true
      }
      property.checkedPrerequisites = true
    }
    return property.prerequisiteOk!
  }

  checkPrerequisites(property?: CustomProperty): void {
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
  }

  presetValueLabel(property: CustomProperty): string|undefined {
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

  editSecret(property: CustomProperty): void {
    if (property.changeValue) {
      property.value = ''
      property.showValue = false
      this.dataService.focus('prop-'+property.id)
    } else {
      if (property.configured) {
        property.value = '*****'
      } else {
        property.value = ''
      }
    }
  }

  removeFile(property: CustomProperty)  {
    delete property.value
    delete property.file
    this.checkPrerequisites(property)
  }

  onFileSelect(property: CustomProperty, file: FileData): void {
    property.file = file
    property.value = file.data
    this.checkPrerequisites(property)
  }

  fileName(property: CustomProperty): string {
    let name = ''
    if (property.file !== undefined) {
      name = property.file.name
    } else {
      if (property.value !== undefined) {
        const mimeType = this.dataService.mimeTypeFromDataURL(property.value)
        const extension = this.dataService.extensionFromMimeType(mimeType)
        name = property.testKey + extension
      }
    }
    return name  
  }

  downloadProperty(property: CustomProperty): void {
    const mimeType = this.dataService.mimeTypeFromDataURL(property.value!)
    const extension = this.dataService.extensionFromMimeType(mimeType)
    const blob = this.dataService.b64toBlob(this.dataService.base64FromDataURL(property.value!), mimeType)
    if (property.file !== undefined) {
      saveAs(blob, property.file.name)
    } else {
      saveAs(blob, property.testKey+extension)
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
        if (property.kind == 'SECRET' && property.configured) {
          property.value = '*****'
        } else if (property.kind == 'SIMPLE') {
          property.hasPresetValues = false
          if (property.allowedValues !== undefined) {
            property.presetValues = JSON.parse(property.allowedValues)
            if (property.presetValues && property.presetValues.length > 0) {
              property.hasPresetValues = true
            }
          }
        }
      }
      this.hasPrerequisites = this.propertiesInvolvedInPrerequisites.length > 0
    }
  }
}
