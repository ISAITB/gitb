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

import {CdkDragDrop} from '@angular/cdk/drag-drop';
import {Component, EventEmitter, Input, Output, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {RoutingService} from 'src/app/services/routing.service';
import {DomainSpecification} from 'src/app/types/domain-specification';
import {SpecificationGroup} from 'src/app/types/specification-group';
import {BsDropdownDirective} from 'ngx-bootstrap/dropdown';
import {DomainSpecificationDisplayComponentApi} from './domain-specification-display-component-api';

@Component({
    selector: 'app-domain-specification-display',
    templateUrl: './domain-specification-display.component.html',
    styleUrls: ['./domain-specification-display.component.less'],
    standalone: false
})
export class DomainSpecificationDisplayComponent implements DomainSpecificationDisplayComponentApi {

  @Input() spec!: DomainSpecification
  @Input() groups: SpecificationGroup[] = []
  @Input() first = false
  @Input() last = false
  @Input() dragOngoing = false
  @Input() dragEnabled = false
  @Output() selectSpec = new EventEmitter<DomainSpecification>()
  @Output() removeSpec = new EventEmitter<[number, number]>()
  @Output() moveSpec = new EventEmitter<[number, number|undefined, number]>()
  @Output() copySpec = new EventEmitter<[number, number|undefined, number]>()
  @Output() dragging = new EventEmitter<boolean>()
  @Output() controlSelected = new EventEmitter<number>()
  @ViewChild("moveDropdown") moveDropdown?: BsDropdownDirective
  @ViewChild("copyDropdown") copyDropdown?: BsDropdownDirective
  @ViewChildren("childComponent") childComponents?: QueryList<DomainSpecificationDisplayComponentApi>
  Constants = Constants

  copyPending = false

  constructor(
    public readonly dataService: DataService,
    private readonly routingService: RoutingService
  ) { }

  removeFromGroup() {
    if (!this.spec.removePending) {
      this.spec.removePending = true
      this.removeSpec.emit([this.spec.id, this.spec.groupId!])
    }
  }

  otherControlSelected(selectedId: number) {
    if (selectedId != this.spec.id) {
      this.copyDropdown?.hide()
      this.moveDropdown?.hide()
    }
    this.childComponents?.forEach((component) => {
      component.otherControlSelected(selectedId)
    })
  }

  moveExpanded() {
    this.copyDropdown?.hide()
    this.controlSelected.emit(this.spec.id)
  }

  moveToGroup(groupId: number) {
    if (!this.spec.movePending) {
      this.spec.movePending = true
      this.moveSpec.emit([this.spec.id, this.spec.groupId, groupId])
    }
  }

  copyExpanded() {
    this.moveDropdown?.hide()
    this.controlSelected.emit(this.spec.id)
  }

  copyToGroup(groupId: number) {
    if (!this.spec.copyPending) {
      this.spec.copyPending = true
      this.copySpec.emit([this.spec.id, this.spec.groupId, groupId])
    }
  }

  createOption() {
    this.routingService.toCreateSpecification(this.spec.domain, this.spec.id)
  }

  editGroup() {
    this.routingService.toSpecificationGroup(this.spec.domain, this.spec.id)
  }

  isPending():boolean {
    return (this.spec.movePending != undefined && this.spec.movePending)
      || (this.spec.removePending != undefined && this.spec.removePending)
      || (this.spec.copyPending != undefined && this.spec.copyPending)
  }

  doSelect() {
    this.propagateSelect(this.spec)
  }

  propagateRemove(event: [number, number]) {
    this.removeSpec.emit(event)
  }

  propagateMove(event: [number, number|undefined, number]) {
    this.moveSpec.emit(event)
  }

  propagateCopy(event: [number, number|undefined, number]) {
    this.copySpec.emit(event)
  }

  propagateSelect(event: DomainSpecification) {
    this.selectSpec.emit(event)
  }

  dropSpecification(event: CdkDragDrop<any>) {
    if (event.currentIndex != event.previousIndex && this.spec.options) {
      this.spec.options.splice(event.currentIndex, 0, this.spec.options.splice(event.previousIndex, 1)[0]);
    }
  }

  propagateDrag(dragging: boolean) {
    this.dragging.emit(dragging)
  }

  startDrag() {
    this.dragging.emit(true)
  }

  endDrag() {
    this.dragging.emit(false)
  }

  propagateControlSelected(selected: number) {
    this.controlSelected.emit(selected)
  }

}
