import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
  selector: 'app-specification-group-details',
  templateUrl: './specification-group-details.component.html',
  styleUrls: [ './specification-group-details.component.less' ]
})
export class SpecificationGroupDetailsComponent extends BaseComponent implements OnInit {

  group: Partial<SpecificationGroup> = {}
  domainId!: number
  groupId!: number
  deletePending = false
  savePending = false

  constructor(
    public dataService: DataService,
    private specificationService: SpecificationService,
    private popupService: PopupService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private confirmationDialogService: ConfirmationDialogService
  ) { super() }

  ngOnInit(): void {
    this.domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.groupId = Number(this.route.snapshot.paramMap.get('group_id'))
    this.specificationService.getSpecificationGroup(this.groupId)
    .subscribe((data) => {
      this.group = data
    })
  }

  deleteGroup(withSpecs: boolean) {
    let message: string
    if (withSpecs) {
      message = "Are you sure you want to delete this "+this.dataService.labelSpecificationLower()+" and any included "+this.dataService.labelSpecificationInGroupsLower()+"?"
    } else {
      message = "Are you sure you want to delete this "+this.dataService.labelSpecificationGroupLower()+" ("+this.dataService.labelSpecificationInGroupsLower()+" will not be deleted)?"
    }
		this.confirmationDialogService.confirmed("Confirm delete", message, "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.specificationService.deleteSpecificationGroup(this.groupId, withSpecs)
      .subscribe(() => {
        this.back()
        this.popupService.success(this.dataService.labelSpecificationGroup()+' deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

	saveDisabled() {
    return !(this.textProvided(this.group?.sname) && this.textProvided(this.group?.fname))
  }

  saveGroupChanges() {
    this.savePending = true
		this.specificationService.updateSpecificationGroup(this.groupId, this.group.sname!, this.group.fname!, this.group.description)
		.subscribe(() => {
			this.popupService.success(this.dataService.labelSpecificationGroup()+' updated.')
    }).add(() => {
      this.savePending = false
    })
  }

  back() {
    this.routingService.toDomain(this.domainId)
  }

}
