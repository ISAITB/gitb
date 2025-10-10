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

import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {OptionalCustomPropertyFormData} from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {DataService} from 'src/app/services/data.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {PopupService} from 'src/app/services/popup.service';
import {RoutingService} from 'src/app/services/routing.service';
import {OrganisationFormData} from '../organisation-form/organisation-form-data';
import {Constants} from 'src/app/common/constants';
import {ValidationState} from 'src/app/types/validation-state';
import {forkJoin} from 'rxjs';
import {LandingPage} from 'src/app/types/landing-page';
import {LegalNotice} from 'src/app/types/legal-notice';
import {ErrorTemplate} from 'src/app/types/error-template';
import {LandingPageService} from 'src/app/services/landing-page.service';
import {LegalNoticeService} from 'src/app/services/legal-notice.service';
import {ErrorTemplateService} from 'src/app/services/error-template.service';
import {CommunityService} from 'src/app/services/community.service';
import {OrganisationFormComponent} from '../organisation-form/organisation-form.component';

@Component({
    selector: 'app-create-organisation',
    templateUrl: './create-organisation.component.html',
    styles: [],
    standalone: false
})
export class CreateOrganisationComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  organisation: Partial<OrganisationFormData> = {}
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false,
    propertyType: 'organisation'
  }
  landingPages: LandingPage[] = []
  legalNotices: LegalNotice[] = []
  errorTemplates: ErrorTemplate[] = []
  loaded = false
  savePending = false
  validation = new ValidationState()
  @ViewChild('form') form?: OrganisationFormComponent
  formDataLoaded = false
  formDataUpdated = false

  constructor(
    public readonly dataService: DataService,
    private readonly routingService: RoutingService,
    private readonly route: ActivatedRoute,
    private readonly organisationService: OrganisationService,
    private readonly popupService: PopupService,
    private readonly landingPageService: LandingPageService,
    private readonly legalNoticeService: LegalNoticeService,
    private readonly errorTemplateService: ErrorTemplateService,
    private readonly communityService: CommunityService
  ) { super() }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    const properties$ = this.communityService.getOrganisationParameters(this.communityId)
    const landingPages$ = this.landingPageService.getLandingPagesByCommunity(this.communityId)
    const legalNotices$ = this.legalNoticeService.getLegalNoticesByCommunity(this.communityId)
    const errorTemplates$ = this.errorTemplateService.getErrorTemplatesByCommunity(this.communityId)
    forkJoin([properties$, landingPages$, legalNotices$, errorTemplates$]).subscribe((data) => {
      this.propertyData.properties = data[0]
      this.landingPages = data[1]
      this.legalNotices = data[2]
      this.errorTemplates = data[3]
      this.formDataLoaded = true
      this.updateFormData()
    }).add(() => {
      this.loaded = true
    })
  }

  ngAfterViewInit(): void {
    this.updateFormData()
  }

  private updateFormData() {
    if (this.form && this.formDataLoaded && !this.formDataUpdated) {
      this.formDataUpdated = true
      this.form.dataLoaded()
    }
  }

  saveDisabled() {
    return !(this.textProvided(this.organisation.sname) && this.textProvided(this.organisation.fname) && (!this.dataService.configuration.registrationEnabled || (!this.organisation?.template || this.textProvided(this.organisation?.templateName))) && (!this.propertyData.edit || this.dataService.customPropertiesValid(this.propertyData.properties)))
  }

  createOrganisation() {
    this.validation.clearErrors()
    this.savePending = true
    this.organisationService.createOrganisation(this.organisation.sname!, this.organisation.fname!, this.organisation.landingPage, this.organisation.legalNotice, this.organisation.errorTemplate, this.organisation.otherOrganisations, this.communityId, this.organisation.template!, this.organisation.templateName, this.propertyData.edit, this.propertyData.properties, this.organisation.copyOrganisationParameters!, this.organisation.copySystemParameters!, this.organisation.copyStatementParameters!)
    .subscribe((result) => {
      if (this.isErrorDescription(result)) {
        this.validation.applyError(result)
      } else {
        this.cancelCreateOrganisation()
        this.popupService.success(this.dataService.labelOrganisation()+" created.")
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateOrganisation() {
    this.routingService.toCommunity(this.communityId, Constants.TAB.COMMUNITY.ORGANISATIONS)
  }

}
