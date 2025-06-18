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

import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { Observable, map, of, share } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';
import { SystemConfigurationService } from 'src/app/services/system-configuration.service';
import { FileData } from 'src/app/types/file-data.type';
import { Theme } from 'src/app/types/theme';
import { saveAs } from 'file-saver';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-theme-form',
    templateUrl: './theme-form.component.html',
    standalone: false
})
export class ThemeFormComponent implements OnInit, AfterViewInit {

  @Input() theme!: Theme
  @Input() readonly = false
  @Input() referenceThemeId?: number
  @Input() validation!: ValidationState
  @ViewChild("themeKey") themeKeyField?: ElementRef;

  acceptedFileTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/svg+xml' ]
  Constants = Constants
  headerLogoNameToShow!: string
  footerLogoNameToShow!: string
  faviconNameToShow!: string
  faviconDownloadPending = false
  headerDownloadPending = false
  footerDownloadPending = false

  constructor(
    private systemConfigurationService: SystemConfigurationService,
    private dataService: DataService,
    private htmlService: HtmlService
  ) {}

  ngOnInit(): void {
    this.headerLogoNameToShow = "headerLogo" + this.theme.headerLogoPath.substring(this.theme.headerLogoPath.lastIndexOf("."))
    this.footerLogoNameToShow = "footerLogo" + this.theme.footerLogoPath.substring(this.theme.footerLogoPath.lastIndexOf("."))
    this.faviconNameToShow = "favicon" + this.theme.faviconPath.substring(this.theme.faviconPath.lastIndexOf("."))
  }

  ngAfterViewInit(): void {
    if (this.theme.custom && this.themeKeyField) {
      this.themeKeyField.nativeElement.focus()
    }
  }

  selectHeaderLogo(file: FileData) {
    this.theme.headerLogoFile = file
    this.headerLogoNameToShow = file.name
  }

  selectFooterLogo(file: FileData) {
    this.theme.footerLogoFile = file
    this.footerLogoNameToShow = file.name
  }

  selectFavicon(file: FileData) {
    this.theme.faviconFile = file
    this.faviconNameToShow = file.name
  }

  previewThemeResource(file: FileData|undefined, resourcePath: string, title: string) {
    let resourceBlob: Observable<Blob|undefined>
    if (file) {
      resourceBlob = of(file.file!)
    } else {
      let idToUse = this.referenceThemeId
      if (idToUse == undefined) {
        idToUse = this.theme.id
      }
      resourceBlob = this.dataService.binaryResponseToBlob(this.systemConfigurationService.previewThemeResource(idToUse, resourcePath))
    }
    resourceBlob.subscribe((data) => {
      if (data) {
        const reader = new FileReader();
        reader.onload = () => {
          const pathForResource = reader.result as string
          const previewHtml = "<div class='resourcePreviewContainer'><img class='resourcePreview' src='"+pathForResource+"'></div>"
          this.htmlService.showHtml(title, previewHtml)
        }
        reader.readAsDataURL(data)
      }
    })
  }

  downloadFavicon() {
    this.faviconDownloadPending = true
    this.downloadThemeResource(this.theme.faviconFile, this.theme.faviconPath, this.faviconNameToShow)
    .subscribe(() => {
      this.faviconDownloadPending = false
    })
  }

  downloadHeaderLogo() {
    this.headerDownloadPending = true
    this.downloadThemeResource(this.theme.headerLogoFile, this.theme.headerLogoPath, this.headerLogoNameToShow)
    .subscribe(() => {
      this.headerDownloadPending = false
    })
  }

  downloadFooterLogo() {
    this.footerDownloadPending = true
    this.downloadThemeResource(this.theme.footerLogoFile, this.theme.footerLogoPath, this.footerLogoNameToShow)
    .subscribe(() => {
      this.footerDownloadPending = false
    })
  }

  downloadThemeResource(file: FileData|undefined, resourcePath: string, fileName: string) {
    let resourceBlob: Observable<Blob|undefined>
    if (file) {
      resourceBlob = of(file.file!)
    } else {
      resourceBlob = this.dataService.binaryResponseToBlob(this.systemConfigurationService.previewThemeResource(this.theme.id, resourcePath))
    }
    return resourceBlob.pipe(
      map((data) => {
        if (data) {
          saveAs(data, fileName)
        }
      }),
      share()
    )
  }

}
