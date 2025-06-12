import { Component, OnInit } from '@angular/core';
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
import { Constants } from 'src/app/common/constants';
import { KeyValue } from 'src/app/types/key-value';
import { SystemAdministrationTab } from '../../../system-administration/system-administration-tab.enum';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: 'app-error-template-details',
    templateUrl: './error-template-details.component.html',
    standalone: false
})
export class ErrorTemplateDetailsComponent extends BaseComponent implements OnInit {

  communityId!: number
  templateId!: number
  template: Partial<ErrorTemplate> = {}
  isDefault = false
  savePending = false
  copyPending = false
  deletePending = false
  placeholders: KeyValue[] = []
  tooltipForDefaultCheck!: string
  validation = new ValidationState()
  loaded = false
  Constants = Constants

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private errorTemplateService: ErrorTemplateService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    private errorService: ErrorService
  ) { super() }

  ngOnInit(): void {
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
      this.tooltipForDefaultCheck = 'Check this to make this error template the default one assumed for the community\'s '+this.dataService.labelOrganisationsLower()+'.'
    } else {
      this.communityId = Constants.DEFAULT_COMMUNITY_ID
      this.tooltipForDefaultCheck = 'Check this to make this error template the default one assumed for all communities.'
    }
    this.templateId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ERROR_TEMPLATE_ID))
    this.placeholders = [
      { key: Constants.PLACEHOLDER__ERROR_DESCRIPTION, value: "The error message text (a text value that may be empty)." },
      { key: Constants.PLACEHOLDER__ERROR_ID, value: "The error identifier (used to trace the error in the logs)." }
    ]
    this.errorTemplateService.getErrorTemplateById(this.templateId)
    .subscribe((data) => {
      this.template = data
      this.isDefault = data.default
      if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
        this.routingService.systemErrorTemplateBreadcrumbs(this.templateId, this.template.name!)
      } else {
        this.routingService.errorTemplateBreadcrumbs(this.communityId, this.templateId, this.template.name!)
      }
    }).add(() => {
      this.loaded = true
    })
  }

  saveDisabled() {
    return !this.loaded || !this.textProvided(this.template.name) || !this.textProvided(this.template.content)
  }

  updateErrorTemplate(copy: boolean) {
    if (!this.isDefault && this.template.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default error template. Are you sure?", "Yes", "No")
      .subscribe(() => {
        this.doUpdate(copy)
      })
    } else {
      this.doUpdate(copy)
    }
  }

  doUpdate(copy: boolean) {
    this.validation.clearErrors()
    this.savePending = true
    this.errorTemplateService.updateErrorTemplate(this.templateId, this.template.name!, this.template.description, this.template.content, this.template.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.validation.applyError(data)
      } else {
        if (copy) {
          this.copyErrorTemplate()
        } else {
          this.popupService.success('Error template updated.')
          if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
            this.dataService.breadcrumbUpdate({id: this.templateId, type: BreadcrumbType.systemErrorTemplate, label: this.template.name})
          } else {
            this.dataService.breadcrumbUpdate({id: this.templateId, type: BreadcrumbType.errorTemplate, label: this.template.name})
          }
        }
      }
    }).add(() => {
      this.savePending = false
    })
  }

  copyErrorTemplate() {
    this.copyPending = true
    if (this.communityId == Constants.DEFAULT_COMMUNITY_ID) {
      this.routingService.toCreateErrorTemplate(undefined, undefined, this.templateId)
    } else {
      this.routingService.toCreateErrorTemplate(this.communityId, false, this.templateId)
    }
  }

  deleteErrorTemplate() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this error template?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.validation.clearErrors()
      this.errorTemplateService.deleteErrorTemplate(this.templateId)
      .subscribe(() => {
        this.cancelDetailErrorTemplate()
        this.popupService.success('Error template deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  cancelDetailErrorTemplate() {
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
