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
import { BsModalService } from 'ngx-bootstrap/modal';
import { PreviewBadgeModalComponent } from 'src/app/modals/preview-badge-modal/preview-badge-modal.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';

@Component({
    selector: 'app-view-badge-button',
    templateUrl: './view-badge-button.component.html',
    styleUrls: ['./view-badge-button.component.less'],
    standalone: false
})
export class ViewBadgeButtonComponent implements OnInit {

  @Input() systemId!: number
  @Input() actorId!: number
  @Input() snapshotId?: number

  copyBadgePending = false

  constructor(
    private conformanceService: ConformanceService,
    private dataService: DataService,
    private modalService: BsModalService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
  }

  copyBadgeURL() {
    this.copyBadgePending = true
    this.conformanceService.conformanceBadgeUrl(this.systemId, this.actorId, this.snapshotId)
    .subscribe((data) => {
      if (data) {
        if (data.startsWith('/')) {
          // Relative URL.
          let prefix = window.location.origin
          if (prefix.endsWith('/')) {
            prefix = prefix.substring(0, prefix.length - 1)
          }
          data = prefix + data
        }
        this.dataService.copyToClipboard(data).subscribe(() => {
          this.popupService.success('Badge URL copied.')
        })
      }
    }).add(() => {
      this.copyBadgePending = false
    })
  }

  previewBadge() {
    this.modalService.show(PreviewBadgeModalComponent, {
      initialState: {
        config: {
          systemId: this.systemId,
          actorId: this.actorId,
          snapshotId: this.snapshotId
        }
      }
    })
  }

}
