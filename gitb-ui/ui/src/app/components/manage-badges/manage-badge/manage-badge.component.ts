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
import { BadgeInfo } from '../badge-info';
import { FileData } from 'src/app/types/file-data.type';
import { Constants } from 'src/app/common/constants';
import { BsModalService } from 'ngx-bootstrap/modal';
import { PreviewBadgeModalComponent } from 'src/app/modals/preview-badge-modal/preview-badge-modal.component';
import { PreviewByFile } from 'src/app/modals/preview-badge-modal/preview-by-file';
import { PreviewForStatus } from 'src/app/modals/preview-badge-modal/preview-for-status';

@Component({
    selector: 'app-manage-badge',
    templateUrl: './manage-badge.component.html',
    standalone: false
})
export class ManageBadgeComponent implements OnInit {

  @Input() badge!: BadgeInfo
  @Input() badgeType!: string
  @Input() specificationId?: number
  @Input() actorId?: number
  @Input() forReport? = false

  acceptedFileTypes: string[] = ['image/png', 'image/jpeg', 'image/gif', 'image/svg+xml' ]
  pathForBadge?: string
  Constants = Constants

  constructor(
    private modalService: BsModalService
  ) { }

  ngOnInit(): void {
    if (!this.forReport) {
      this.acceptedFileTypes.push('image/svg+xml')
    }
  }

  previewBadge() {
    let config: PreviewByFile|PreviewForStatus
    if (this.badge.file?.file) {
      config = {
        badgeFile: this.badge
      }
    } else {
      config = {
        specificationId: this.specificationId!,
        actorId: this.actorId,
        status: this.badgeType
      }
    }
    this.modalService.show(PreviewBadgeModalComponent, {
      initialState: {
        config: config,
        forReport: this.forReport
      }
    })
  }

  removeBadge() {
    this.badge.enabled = false
    this.badge.file = undefined
    this.badge.nameToShow = undefined
  }

  selectBadge(file: FileData) {
    this.badge.file = file
    this.badge.enabled = true
  }

}
