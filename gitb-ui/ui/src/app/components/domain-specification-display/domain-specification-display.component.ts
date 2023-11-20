import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { DomainSpecification } from 'src/app/types/domain-specification';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
  selector: 'app-domain-specification-display',
  templateUrl: './domain-specification-display.component.html',
  styleUrls: [ './domain-specification-display.component.less' ]
})
export class DomainSpecificationDisplayComponent implements OnInit {

  @Input() spec!: DomainSpecification
  @Input() groups: SpecificationGroup[] = []
  @Input() first = false
  @Input() last = false
  @Output() selectSpec = new EventEmitter<DomainSpecification>()
  @Output() removeSpec = new EventEmitter<[number, number]>()
  @Output() moveSpec = new EventEmitter<[number, number|undefined, number]>()
  @Output() copySpec = new EventEmitter<[number, number|undefined, number]>()
  @Output() moveUp = new EventEmitter<DomainSpecification>()
  @Output() moveDown = new EventEmitter<DomainSpecification>()
  Constants = Constants

  removePending = false
  movePending = false
  copyPending = false

  constructor(
    public dataService: DataService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
  }

  removeFromGroup() {
    if (!this.spec.removePending) {
      this.spec.removePending = true
      this.removeSpec.emit([this.spec.id, this.spec.groupId!])
    }
  }

  moveToGroup(groupId: number) {
    if (!this.spec.movePending) {
      this.spec.movePending = true
      this.moveSpec.emit([this.spec.id, this.spec.groupId, groupId])
    }
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

  doMoveUp(event: UIEvent) {
    if (event.currentTarget && (event.currentTarget as any).blur) {
      (event.currentTarget as any).blur()
    }
    this.propagateUp(this.spec)
  }

  doMoveDown(event: UIEvent) {
    if (event.currentTarget && (event.currentTarget as any).blur) {
      (event.currentTarget as any).blur()
    }
    this.propagateDown(this.spec)
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

  propagateUp(event: DomainSpecification) {
    this.moveUp.emit(event)
  }

  propagateDown(event: DomainSpecification) {
    this.moveDown.emit(event)
  }

  propagateSelect(event: DomainSpecification) {
    this.selectSpec.emit(event)
  }

}
