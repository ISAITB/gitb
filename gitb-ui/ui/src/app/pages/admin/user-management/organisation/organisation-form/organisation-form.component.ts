import { Component, Input, OnInit } from '@angular/core';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { DataService } from 'src/app/services/data.service';
import { ErrorTemplate } from 'src/app/types/error-template';
import { LandingPage } from 'src/app/types/landing-page';
import { LegalNotice } from 'src/app/types/legal-notice';
import { Organisation } from 'src/app/types/organisation.type';
import { OrganisationFormData } from './organisation-form-data';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
  selector: 'app-organisation-form',
  templateUrl: './organisation-form.component.html',
  styles: [
  ]
})
export class OrganisationFormComponent implements OnInit {

  @Input() organisation!: Partial<OrganisationFormData>
  @Input() communityId!: number
  @Input() propertyData!: OptionalCustomPropertyFormData
  @Input() showAdminInfo = true
  @Input() showLandingPage = false
  @Input() readonly = false
  @Input() validation!: ValidationState
  @Input() landingPages: LandingPage[] = []
  @Input() legalNotices: LegalNotice[] = []
  @Input() errorTemplates: ErrorTemplate[] = []
  @Input() otherOrganisations: Organisation[] = []

  selfRegEnabled = false

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    this.organisation.copyOrganisationParameters = false
    this.organisation.copySystemParameters = false
    this.organisation.copyStatementParameters = false
    this.selfRegEnabled = this.dataService.configuration.registrationEnabled
  }

  copyChanged() {
    if (this.organisation.otherOrganisations == undefined) {
      this.organisation.copyOrganisationParameters = false
      this.organisation.copySystemParameters = false
      this.organisation.copyStatementParameters = false
    } else if (this.organisation.copyOrganisationParameters) {
      this.propertyData.edit = false    
    }
  }

  templateChoiceChanged() {
    if (this.organisation.template) {
      this.dataService.focus('templateName')
    }
  }

}
