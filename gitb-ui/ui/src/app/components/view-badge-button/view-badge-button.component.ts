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

import {Component, Input} from '@angular/core';
import {BsModalService} from 'ngx-bootstrap/modal';
import {ConformanceService} from 'src/app/services/conformance.service';
import {PreviewBadgeModalComponent} from '../../modals/preview-badge-modal/preview-badge-modal.component';

@Component({
    selector: 'app-view-badge-button',
    templateUrl: './view-badge-button.component.html',
    styleUrls: ['./view-badge-button.component.less'],
    standalone: false
})
export class ViewBadgeButtonComponent {

  @Input() systemId!: number
  @Input() actorId!: number
  @Input() snapshotId?: number

  copyBadgePending = false

  constructor(
    private readonly conformanceService: ConformanceService,
    private readonly modalService: BsModalService
  ) { }

  copyBadgeURL() {
    this.copyBadgePending = true
    this.conformanceService.copyBadgeURL(this.systemId, this.actorId, this.snapshotId).subscribe(() => {
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
