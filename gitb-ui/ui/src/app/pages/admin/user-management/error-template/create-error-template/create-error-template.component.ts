import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorTemplateService } from 'src/app/services/error-template.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { ErrorData } from 'src/app/types/error-data.type';
import { ErrorTemplate } from 'src/app/types/error-template';
import { CommunityTab } from '../../community/community-details/community-tab.enum';
import { KeyValue } from 'src/app/types/key-value';
import { Constants } from 'src/app/common/constants';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
  selector: 'app-create-error-template',
  templateUrl: './create-error-template.component.html',
  styles: [
  ]
})
export class CreateErrorTemplateComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  template: Partial<ErrorTemplate> = {
    default: false
  }
  placeholders: KeyValue[] = []
  savePending = false
  tooltipForDefaultCheck!: string
  validation = new ValidationState()
  Constants = Constants

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private errorTemplateService: ErrorTemplateService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    public dataService: DataService,
    private errorService: ErrorService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this error template the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this error template the default one assumed for all communities.'
    }
    this.placeholders = [
      { key: Constants.PLACEHOLDER__ERROR_DESCRIPTION, value: "The error message text (a text value that may be empty)." },
      { key: Constants.PLACEHOLDER__ERROR_ID, value: "The error identifier (used to trace the error in the logs)." }
    ]
    const base = this.route.snapshot.data['base'] as ErrorTemplate|undefined
    if (base != undefined) {
      this.template.name = base.name
      this.template.description = base.description
      this.template.content = base.content
    }
  }

  saveDisabled() {
    return !this.textProvided(this.template.name) || !this.textProvided(this.template.content)
  }

  createErrorTemplate() {
    if (this.template.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default error template. Are you sure?", "Change", "Cancel")
      .subscribe(() => {
        this.doCreate()
      })
    } else {
      this.doCreate()
    }
  }

  doCreate() {
    this.validation.clearErrors()
    this.savePending = true
    this.errorTemplateService.createErrorTemplate(this.template.name!, this.template.description, this.template.content, this.template.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        this.cancelCreateErrorTemplate()
        this.popupService.success('Error template created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateErrorTemplate() {
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toSystemAdministration(SystemAdministrationTab.errorTemplates)
    } else {
      this.routingService.toCommunity(this.communityId, CommunityTab.errorTemplates)
    }
  }

preview() {
    const error: ErrorData = {
      statusText: 'Internal server error',
      error: {
        error_description: 'This is a sample error description message.',
        error_id: '0123456789'
      },
      template: this.template.content
    }
    this.errorService.showErrorMessage(error)
  }

}
