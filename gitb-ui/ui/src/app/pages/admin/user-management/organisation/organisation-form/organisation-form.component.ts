import {Component, EventEmitter, Input, OnInit} from '@angular/core';
import {OptionalCustomPropertyFormData} from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import {DataService} from 'src/app/services/data.service';
import {ErrorTemplate} from 'src/app/types/error-template';
import {LandingPage} from 'src/app/types/landing-page';
import {LegalNotice} from 'src/app/types/legal-notice';
import {Organisation} from 'src/app/types/organisation.type';
import {OrganisationFormData} from './organisation-form-data';
import {ValidationState} from 'src/app/types/validation-state';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';
import {FilterUpdate} from '../../../../../components/test-filter/filter-update';
import {map, Observable, of} from 'rxjs';
import {OrganisationService} from '../../../../../services/organisation.service';

@Component({
    selector: 'app-organisation-form',
    templateUrl: './organisation-form.component.html',
    styles: [],
    standalone: false
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
  otherOrganisations?: Organisation[]

  selfRegEnabled = false
  landingPageSelectionConfig!: MultiSelectConfig<LandingPage>
  legalNoticeSelectionConfig!: MultiSelectConfig<LegalNotice>
  errorTemplateSelectionConfig!: MultiSelectConfig<ErrorTemplate>
  copySelectionConfig!: MultiSelectConfig<Organisation>

  constructor(
    public dataService: DataService,
    private organisationService: OrganisationService,
  ) { }

  ngOnInit(): void {
    this.organisation.copyOrganisationParameters = false
    this.organisation.copySystemParameters = false
    this.organisation.copyStatementParameters = false
    this.selfRegEnabled = this.dataService.configuration.registrationEnabled
    this.landingPageSelectionConfig = {
      name: "landingPage",
      textField: "name",
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: "Use community default",
      replaceSelectedItems: new EventEmitter(),
      loader: () => of(this.landingPages)
    }
    this.legalNoticeSelectionConfig = {
      name: "legalNotice",
      textField: "name",
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: "Use community default",
      replaceSelectedItems: new EventEmitter(),
      loader: () => of(this.legalNotices)
    }
    this.errorTemplateSelectionConfig = {
      name: "template",
      textField: "name",
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: "Use community default",
      replaceSelectedItems: new EventEmitter(),
      loader: () => of(this.errorTemplates)
    }
    this.copySelectionConfig = {
      name: "otherOrganisation",
      textField: "fname",
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: "Select source " + this.dataService.labelOrganisationLower() + "...",
      loader: () => this.loadOtherOrganisations()
    }
  }

  loadOtherOrganisations(): Observable<Organisation[]> {
    if (this.otherOrganisations == undefined) {
      return this.organisationService.getOrganisationsByCommunity(this.communityId).pipe(
        map((data) => {
          let sources: Organisation[]
          if (this.organisation.id != undefined) {
            sources = data
          } else {
            sources = data.filter(x => Number(x.id) != Number(this.organisation.id))
          }
          this.otherOrganisations = sources
          return sources
        })
      )
    } else {
      return of(this.otherOrganisations)
    }
  }

  dataLoaded() {
    setTimeout(() => {
      if (this.organisation.landingPage != undefined) {
        const item = this.landingPages.find(x => x.id == this.organisation.landingPage)
        if (item) {
          this.landingPageSelectionConfig.replaceSelectedItems!.emit([item])
        }
      }
      if (this.organisation.legalNotice != undefined) {
        const item = this.legalNotices.find(x => x.id == this.organisation.legalNotice)
        if (item) {
          this.legalNoticeSelectionConfig.replaceSelectedItems!.emit([item])
        }
      }
      if (this.organisation.errorTemplate != undefined) {
        const item = this.errorTemplates.find(x => x.id == this.organisation.errorTemplate)
        if (item) {
          this.errorTemplateSelectionConfig.replaceSelectedItems!.emit([item])
        }
      }
    })
  }

  templateChoiceChanged() {
    if (this.organisation.template) {
      this.dataService.focus('templateName')
    }
  }

  landingPageSelected(event: FilterUpdate<LandingPage>) {
    if (this.organisation) {
      if (event.values.active.length != 0) {
        this.organisation.landingPage = event.values.active[0].id
      } else {
        this.organisation.landingPage = undefined
      }
    }
  }

  legalNoticeSelected(event: FilterUpdate<LegalNotice>) {
    if (this.organisation) {
      if (event.values.active.length != 0) {
        this.organisation.legalNotice = event.values.active[0].id
      } else {
        this.organisation.legalNotice = undefined
      }
    }
  }

  errorTemplateSelected(event: FilterUpdate<ErrorTemplate>) {
    if (this.organisation) {
      if (event.values.active.length != 0) {
        this.organisation.errorTemplate = event.values.active[0].id
      } else {
        this.organisation.errorTemplate = undefined
      }
    }
  }

  otherOrganisationSelected(event: FilterUpdate<Organisation>) {
    if (this.organisation) {
      if (event.values.active.length != 0) {
        this.organisation.otherOrganisations = event.values.active[0].id
      } else {
        this.organisation.otherOrganisations = undefined
      }
      this.copyChanged()
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


}
