import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { FileData } from 'src/app/types/file-data.type';
import { saveAs } from 'file-saver'
import { OrganisationService } from 'src/app/services/organisation.service';
import { SystemService } from 'src/app/services/system.service';
import { Observable } from 'rxjs';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: 'app-custom-property-form',
  templateUrl: './custom-property-form.component.html',
  styleUrls: [ './custom-property-form.component.less' ]
})
export class CustomPropertyFormComponent implements OnInit {

  @Input() tbProperties?:CustomProperty[]
  @Input() tbColLabel = 3
  @Input() tbColOffset = 1
  @Input() tbColInputLess = 0
  @Input() tbReadonly = false
  @Input() tbForceEditable = false
  @Input() tbFormPadded = true
  @Input() tbShowFormHeader = true
  @Input() tbShowRequiredAsterisks = true
  @Input() tbAdmin?: boolean
  @Input() tbPropertyType!: 'organisation'|'system'
  @Input() tbOwner?: number
  @Input() tbExpandable = false
  @Input() tbCollapsed: boolean|undefined
  @Input() tbSetDefaults = false
  @Input() refresh?: EventEmitter<{props?: CustomProperty[], asterisks: boolean}>
  @Output() collapseChange = new EventEmitter<boolean>()

  Constants = Constants
  isAdmin = false
  isReadonly = true
  innerDivStyle = ''
  private hasPrerequisites = false
  private propertyMap: Record<string, CustomProperty> = {}
  private propertiesInvolvedInPrerequisites: string[] = []
  private propertiesInvolvedInPrerequisitesMap: Record<string, boolean> = {}

  constructor(
    private dataService: DataService,
    private organisationService: OrganisationService,
    private systemService: SystemService
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
    if (this.tbFormPadded) {
      this.innerDivStyle = 'col-'+(11-this.tbColOffset)+' offset-'+this.tbColOffset
    }
    if (this.tbSetDefaults && this.tbProperties != undefined) {
      for (let prop of this.tbProperties) {
        if (prop.defaultValue != undefined) {
          prop.value = prop.defaultValue
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
        property.prerequisiteOk = this.checkPrerequisite(this.propertyMap[property.dependsOn]) && this.propertyMap[property.dependsOn].value == property.dependsOnValue
      } else {
        property.prerequisiteOk = true
      }
      property.checkedPrerequisites = true
    }
    return property.prerequisiteOk!
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
  }

  onFileSelect(property: CustomProperty, file: FileData): void {
    property.file = file
    property.value = "PATH"
    this.checkPrerequisites(property)
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
      } else {
        fn = this.systemService.downloadSystemParameterFile(this.tbOwner!, property.id)
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
        if (property.kind == 'SIMPLE') {
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

  checkToExpand() {
    if (this.tbExpandable) {
      this.tbCollapsed = !this.tbCollapsed
      this.collapseChange.emit(this.tbCollapsed)
    }
  }
}
