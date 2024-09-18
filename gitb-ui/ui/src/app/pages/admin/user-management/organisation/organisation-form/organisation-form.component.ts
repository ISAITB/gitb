import { AfterViewInit, Component, Input, OnInit } from '@angular/core';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { CommunityService } from 'src/app/services/community.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorTemplateService } from 'src/app/services/error-template.service';
import { LandingPageService } from 'src/app/services/landing-page.service';
import { LegalNoticeService } from 'src/app/services/legal-notice.service';
import { OrganisationService } from 'src/app/services/organisation.service';
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
export class OrganisationFormComponent implements OnInit, AfterViewInit {

  @Input() organisation!: Partial<OrganisationFormData>
  @Input() communityId!: number
  @Input() propertyData!: OptionalCustomPropertyFormData
  @Input() showAdminInfo = true
  @Input() showLandingPage = false
  @Input() readonly = false
  @Input() validation!: ValidationState

  selfRegEnabled = false
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []
  otherOrganisations: Organisation[] = []

  constructor(
    public dataService: DataService,
    private communityService: CommunityService,
    private landingPageService: LandingPageService,
    private legalNoticeService: LegalNoticeService,
    private errorTemplateService: ErrorTemplateService,
    private organisationService: OrganisationService
  ) { }

  ngAfterViewInit(): void {
    if (!this.readonly) {
      this.dataService.focus('sname')
    }
  }

  ngOnInit(): void {
    this.organisation.copyOrganisationParameters = false
    this.organisation.copySystemParameters = false
    this.organisation.copyStatementParameters = false
    this.selfRegEnabled = this.dataService.configuration.registrationEnabled
    if (this.organisation.id == undefined) {
      this.communityService.getOrganisationParameters(this.communityId)
      .subscribe((data) => {
        this.propertyData.properties = data
      })
    } else {
      this.propertyData.owner = this.organisation.id
      this.communityService.getOrganisationParameterValues(this.organisation.id)
      .subscribe((data) => {
        this.propertyData.properties = data
      })
    }
    if (this.showAdminInfo || this.showLandingPage) {
      this.landingPageService.getLandingPagesByCommunity(this.communityId)
      .subscribe((data) => {
        this.landingPages = data
      })
    }
    if (this.showAdminInfo) {
      this.legalNoticeService.getLegalNoticesByCommunity(this.communityId)
      .subscribe((data) => {
        this.legalNotices = data
      })
      this.errorTemplateService.getErrorTemplatesByCommunity(this.communityId)
      .subscribe((data) => {
        this.errorTemplates = data
      })
      this.organisationService.getOrganisationsByCommunity(this.communityId)
      .subscribe((data) => {
        for (let organisation of data) {
          if (this.organisation.id == undefined || Number(organisation.id) != Number(this.organisation.id)) {
            this.otherOrganisations.push(organisation)
          }
        }
      })
    }
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
