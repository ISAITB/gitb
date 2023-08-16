import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { SystemService } from 'src/app/services/system.service';
import { System } from 'src/app/types/system';
import { OrganisationTab } from '../../organisation/organisation-details/OrganisationTab';

@Component({
  selector: 'app-create-system',
  templateUrl: './create-system.component.html',
  styles: [
  ]
})
export class CreateSystemComponent extends BaseComponent implements OnInit {

  organisationId!: number
  communityId!: number
  fromCommunityManagement!: boolean
  system: Partial<System> = {}
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'system'
  }
  savePending = false

  constructor(
    public dataService: DataService,
    private systemService: SystemService,
    private route: ActivatedRoute,
    private popupService: PopupService,
    private routingService: RoutingService
  ) { super() }

  ngOnInit(): void {
    this.fromCommunityManagement = this.route.snapshot.paramMap.has('community_id')
    if (this.fromCommunityManagement) {
      this.organisationId = Number(this.route.snapshot.paramMap.get('org_id'))
      this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    } else {
      this.organisationId = this.dataService.vendor!.id
      this.communityId = this.dataService.community!.id
    }
  }

  saveEnabled() {
    return this.textProvided(this.system.sname) && this.textProvided(this.system.fname)
  }

  create() {
    if (this.saveEnabled()) {
      this.savePending = true
      this.systemService.registerSystemWithOrganisation(this.system.sname!, this.system.fname!, this.system.description, this.system.version, this.organisationId, this.system.otherSystems, this.propertyData.edit, this.propertyData.properties, this.system.copySystemParameters, this.system.copyStatementParameters)
      .subscribe(() => {
        this.cancel()
        this.popupService.success(this.dataService.labelSystem() + ' created.')
      }).add(() => {
        this.savePending = false
      })
    }
  }

  cancel() {
    if (this.fromCommunityManagement) {
      this.routingService.toOrganisationDetails(this.communityId, this.organisationId, OrganisationTab.systems)
    } else {
      this.routingService.toOwnOrganisationDetails(OrganisationTab.systems)
    }
  }

}
