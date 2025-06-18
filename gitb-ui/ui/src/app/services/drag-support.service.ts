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

import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DragSupportService {

  private dragEnterSource = new Subject<void>()
  public onDragStartChange$ = this.dragEnterSource.asObservable()
  private dragLeaveSource = new Subject<void>()
  public onDragLeaveChange$ = this.dragLeaveSource.asObservable()
  private dragDropSource = new Subject<void>()
  public onDragDropChange$ = this.dragDropSource.asObservable()

  constructor() { }

  dragEnter() {
    this.dragEnterSource.next()
  }

  dragLeave() {
    this.dragLeaveSource.next()
  }

  dragDrop() {
    this.dragDropSource.next()
  }
}
