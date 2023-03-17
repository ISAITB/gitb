import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { DomainSpecification } from 'src/app/types/domain-specification';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
  selector: '[app-domain-specification-table-row]',
  templateUrl: './domain-specification-table-row.component.html',
  styleUrls: [ './domain-specification-table-row.component.less' ]
})
export class DomainSpecificationTableRowComponent implements OnInit {

  @Input() spec!: DomainSpecification
  @Input() groups: SpecificationGroup[] = []
  @Output() removeSpec = new EventEmitter<[number, number]>()
  @Output() moveSpec = new EventEmitter<[number, number|undefined, number]>()
  @Output() copySpec = new EventEmitter<[number, number|undefined, number]>()
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
}
