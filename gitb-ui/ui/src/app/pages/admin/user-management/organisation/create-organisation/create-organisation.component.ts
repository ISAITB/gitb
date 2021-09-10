import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { OrganisationFormData } from '../organisation-form/organisation-form-data';

@Component({
  selector: 'app-create-organisation',
  templateUrl: './create-organisation.component.html',
  styles: [
  ]
})
export class CreateOrganisationComponent extends BaseComponent implements OnInit {

  communityId!: number
  organisation: Partial<OrganisationFormData> = {}
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false
  }
  savePending = false

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private organisationService: OrganisationService,
    private popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
  }

  saveDisabled() {
    return !(this.textProvided(this.organisation.sname) && this.textProvided(this.organisation.fname) && (!this.dataService.configuration.registrationEnabled || (!this.organisation?.template || this.textProvided(this.organisation?.templateName))) && (!this.propertyData.edit || this.dataService.customPropertiesValid(this.propertyData.properties)))
  }

  createOrganisation() {
    this.clearAlerts()
    this.savePending = true
    this.organisationService.createOrganisation(this.organisation.sname!, this.organisation.fname!, this.organisation.landingPage, this.organisation.legalNotice, this.organisation.errorTemplate, this.organisation.otherOrganisations, this.communityId, this.organisation.template!, this.organisation.templateName, this.propertyData.edit, this.propertyData.properties, this.organisation.copyOrganisationParameters!, this.organisation.copySystemParameters!, this.organisation.copyStatementParameters!)
    .subscribe((result) => {
      if (result?.error_code != undefined) {
        this.addAlertError(result.error_description)
      } else {
        this.cancelCreateOrganisation()
        this.popupService.success(this.dataService.labelOrganisation()+" created.")
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateOrganisation() {
    this.routingService.toCommunity(this.communityId)
  }

}
