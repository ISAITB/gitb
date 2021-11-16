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

@Component({
  selector: 'app-error-template-details',
  templateUrl: './error-template-details.component.html',
  styles: [
  ]
})
export class ErrorTemplateDetailsComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  templateId!: number
  template: Partial<ErrorTemplate> = {}
  isDefault = false
  savePending = false
  copyPending = false
  deletePending = false

  constructor(
    public dataService: DataService,
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private errorTemplateService: ErrorTemplateService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    private errorService: ErrorService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    this.templateId = Number(this.route.snapshot.paramMap.get('template_id'))
    this.errorTemplateService.getErrorTemplateById(this.templateId)
    .subscribe((data) => {
      this.template = data
      this.isDefault = data.default
    })
  }

  saveDisabled() {
    return !this.textProvided(this.template.name) || !this.textProvided(this.template.content)
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
    this.savePending = true
    this.errorTemplateService.updateErrorTemplate(this.templateId, this.template.name!, this.template.description, this.template.content, this.template.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.addAlertError(data.error_description)
      } else {
        if (copy) {
          this.copyErrorTemplate()
        } else {
          this.popupService.success('Error template updated.')
        }
      }
    }).add(() => {
      this.savePending = false
    })
  }

  copyErrorTemplate() {
    this.copyPending = true
    this.routingService.toCreateErrorTemplate(this.communityId, false, this.templateId)
  }

  deleteErrorTemplate() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this error template?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
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
    this.routingService.toCommunity(this.communityId, CommunityTab.errorTemplates)
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
