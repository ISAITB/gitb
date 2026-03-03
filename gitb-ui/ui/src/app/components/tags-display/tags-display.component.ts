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

import {Component, Input, OnInit} from '@angular/core';
import {TagData} from 'src/app/types/tag-data';
import {Constants} from '../../common/constants';
import {CreateEditTagComponent} from '../../modals/create-edit-tag/create-edit-tag.component';
import {Utils} from '../../common/utils';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TagsDisplayApi} from './tags-display-api';

@Component({
    selector: 'app-tags-display',
    templateUrl: './tags-display.component.html',
    styleUrls: ['./tags-display.component.less'],
    standalone: false
})
export class TagsDisplayComponent implements OnInit, TagsDisplayApi {

  @Input() tags: TagData[] = []
  @Input() editable? = false
  @Input() tooltip?: string

  private tagCounter = 0

  constructor(
    private readonly modalService: NgbModal
  ) { }

  ngOnInit(): void {
    this.tags.sort((a, b) => a.name.localeCompare(b.name));
    this.tags.forEach(tag => {
      if (tag.id == undefined) {
        tag.id = this.tagCounter++
      }
    });
  }

  serializeTags(): string | undefined {
    if (this.tags.length > 0) {
      const cleanTags = this.tags.map(tag => {
        const copy = {...tag} as TagData;
        delete copy.id;
        return copy;
      })
      return JSON.stringify(cleanTags);
    }
    return undefined;
  }

  createTag() {
    this.openTagModal()
  }

  tagEdited(id: number) {
    const selectedTag = this.tags.find((tag) => tag.id == id)
    if (selectedTag) {
      this.openTagModal(selectedTag)
    }
  }

  tagDeleted(id: number) {
    Utils.removeFromArray(this.tags, (tag) => tag.id == id)
  }

  private openTagModal(tag?: TagData) {
    const modal = this.modalService.open(CreateEditTagComponent, { size: 'lg' })
    const modalInstance = modal.componentInstance as CreateEditTagComponent
    modalInstance.tag = tag
    modal.closed.subscribe((processedTag: TagData) => {
      if (processedTag.id == undefined) {
        // Create
        processedTag.id = this.tagCounter++
        this.tags.push(processedTag)
        this.tags.sort((a, b) => a.name.localeCompare(b.name))
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
