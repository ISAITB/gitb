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

import { Directive, HostListener } from '@angular/core';
import { DragSupportService } from '../services/drag-support.service';

@Directive({
    selector: '[appFileDropTarget]',
    standalone: false
})
export class FileDropTargetDirective {

  dragEnterCounter = 0

  constructor(
    private dragSupport: DragSupportService
  ) {
    this.dragSupport.onDragDropChange$.subscribe(() => {
      this.dragEnterCounter = 0
    })
  }

  @HostListener('dragenter', ['$event'])
  onDragEnter(event: DragEvent) {
    event.preventDefault()
    this.dragEnterCounter += 1
    this.dragSupport.dragEnter()
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent) {
    event.preventDefault()
    this.dragEnterCounter -= 1
    if (this.dragEnterCounter == 0) {
      this.dragSupport.dragLeave()
    }
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent) {
    event.preventDefault()
    this.dragSupport.dragDrop()
  }

}
