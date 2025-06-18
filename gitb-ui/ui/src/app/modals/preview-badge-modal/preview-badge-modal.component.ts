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

import { Component, Input, OnInit } from '@angular/core';
import { PreviewByIds } from './preview-by-ids';
import { PreviewByFile } from './preview-by-file';
import { PreviewForStatus } from './preview-for-status';
import { ConformanceService } from 'src/app/services/conformance.service';
import { Observable, of } from 'rxjs';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';

@Component({
    selector: 'app-preview-badge-modal',
    templateUrl: './preview-badge-modal.component.html',
    styleUrls: ['./preview-badge-modal.component.less'],
    standalone: false
})
export class PreviewBadgeModalComponent implements OnInit {

  @Input() config!: PreviewByIds|PreviewForStatus|PreviewByFile
  @Input() forReport? = false

  headerText: string = ''
  html?: string

  constructor(
    private conformanceService: ConformanceService,
    private modalRef: BsModalRef,
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.headerText = "Badge preview"
    this.modalRef.setClass("modal-m")
    this.getBadgeBlob().subscribe((data) => {
      if (data) {
        const reader = new FileReader();
        reader.onload = () => {
          const pathForBadge = reader.result as string
          this.html = "<div class='badgePreviewContainer'><img class='badgePreview' src='"+pathForBadge+"'></div>"
        }
        reader.readAsDataURL(data)
      }
    })
  }

  close() {
    this.modalRef.hide()
  }

  private isPreviewForStatus(config: PreviewByIds|PreviewForStatus|PreviewByFile): config is PreviewForStatus {
    return (config as PreviewForStatus).status !== undefined
  }

  private isPreviewByFile(config: PreviewByIds|PreviewForStatus|PreviewByFile): config is PreviewByFile {
    return (config as PreviewByFile).badgeFile !== undefined
  }

  private getBadgeBlob(): Observable<Blob|undefined> {
    if (this.isPreviewByFile(this.config)) {
      return of(this.config.badgeFile.file?.file!)
    } else if (this.isPreviewForStatus(this.config)) {
      return this.dataService.binaryResponseToBlob(this.conformanceService.getBadgeForStatus(this.config.specificationId, this.config.actorId, this.config.status, this.forReport))
    } else { // PreviewByIds
      return this.dataService.binaryResponseToBlob(this.conformanceService.conformanceBadgeByIds(this.config.systemId, this.config.actorId, this.config.snapshotId, this.forReport))
    }
  }

}
