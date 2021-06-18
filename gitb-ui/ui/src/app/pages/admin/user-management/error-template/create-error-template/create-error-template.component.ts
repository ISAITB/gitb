import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorTemplateService } from 'src/app/services/error-template.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { ErrorData } from 'src/app/types/error-data.type';
import { ErrorTemplate } from 'src/app/types/error-template';

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
  savePending = false

  constructor(
    private router: Router,
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
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
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
    this.clearAlerts()
    if (this.template.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default error template. Are you sure?", "Yes", "No")
      .subscribe(() => {
        this.doCreate()
      })
    } else {
      this.doCreate()
    }
  }

  doCreate() {
    this.savePending = true
    this.errorTemplateService.createErrorTemplate(this.template.name!, this.template.description, this.template.content, this.template.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.addAlertError(data.error_description)
      } else {
        this.cancelCreateErrorTemplate()
        this.popupService.success('Error template created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateErrorTemplate() {
    this.router.navigate(['admin', 'users', 'community', this.communityId])
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