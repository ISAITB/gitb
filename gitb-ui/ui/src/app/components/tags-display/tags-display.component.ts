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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {TagData} from 'src/app/types/tag-data';
import {Constants} from '../../common/constants';
import {CreateEditTagComponent} from '../../modals/create-edit-tag/create-edit-tag.component';
import {Utils} from '../../common/utils';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-tags-display',
    templateUrl: './tags-display.component.html',
    styleUrls: ['./tags-display.component.less'],
    standalone: false
})
export class TagsDisplayComponent implements OnInit {

  @Input() tags?: TagData[] = []
  @Input() editable? = false
  @Input() tooltip?: string
  @Input() max?: number
  @Output() added = new EventEmitter<void>();

  private tagCounter = 0

  constructor(
    private readonly modalService: NgbModal
  ) { }

  ngOnInit(): void {
    if (this.tags == undefined) {
      this.tags = []
    }
    this.tags.sort((a, b) => a.name.localeCompare(b.name));

  }

  private addIdsIfNeeded(): void {
    if (this.tags != undefined && this.tags.length > 0 && this.tags[0].id == undefined) {
      this.tags.forEach(tag => {
        if (tag.id == undefined) {
          tag.id = this.tagCounter++
        }
      });
    }
  }

  createTag() {
    this.addIdsIfNeeded()
    this.openTagModal()
  }

  tagEdited(selectedTag: TagData) {
    this.addIdsIfNeeded()
    this.openTagModal(selectedTag)
  }

  tagDeleted(deletedTag: TagData) {
    this.addIdsIfNeeded();
    Utils.removeFromArray(this.tags, (tag) => tag.id == deletedTag.id)
  }

  private openTagModal(tag?: TagData) {
    const modal = this.modalService.open(CreateEditTagComponent, { size: 'lg' })
    const modalInstance = modal.componentInstance as CreateEditTagComponent
    modalInstance.tag = tag
    modal.closed.subscribe((processedTag: TagData) => {
      if (this.tags == undefined) {
        this.tags = []
      }
      if (processedTag.id == undefined) {
        // Create
        processedTag.id = this.tagCounter++
        this.tags.push(processedTag)
        this.tags.sort((a, b) => a.name.localeCompare(b.name))
        this.added.emit()
      } else {
        // Update
        Utils.removeFromArray(this.tags, (tag) => tag.id == processedTag.id)
        this.tags.push(processedTag)
        this.tags.sort((a, b) => a.name.localeCompare(b.name))
      }
    })
  }

  protected readonly Constants = Constants;
}
