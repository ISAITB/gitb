import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';
import { LegalNoticeService } from 'src/app/services/legal-notice.service';
import { PopupService } from 'src/app/services/popup.service';
import { LegalNotice } from 'src/app/types/legal-notice';

@Component({
  selector: 'app-create-legal-notice',
  templateUrl: './create-legal-notice.component.html',
  styles: [
  ]
})
export class CreateLegalNoticeComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  notice: Partial<LegalNotice> = {
    default: false
  }
  savePending = false

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private legalNoticeService: LegalNoticeService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    public dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    const base = this.route.snapshot.data['base'] as LegalNotice|undefined
    if (base != undefined) {
      this.notice.name = base.name
      this.notice.description = base.description
      this.notice.content = base.content
    }
  }

  saveDisabled() {
    return !this.textProvided(this.notice.name) || !this.textProvided(this.notice.content)
  }

  createLegalNotice() {
    this.clearAlerts()
    if (this.notice.default) {
      this.confirmationDialogService.confirmed("Confirm default", "You are about to change the default legal notice. Are you sure?", "Yes", "No")
      .subscribe(() => {
        this.doCreate()
      })
    } else {
      this.doCreate()
    }
  }

  doCreate() {
    this.savePending = true
    this.legalNoticeService.createLegalNotice(this.notice.name!, this.notice.description, this.notice.content, this.notice.default!, this.communityId)
    .subscribe((data) => {
      if (this.isErrorDescription(data)) {
        this.addAlertError(data.error_description)
      } else {
        this.cancelCreateLegalNotice()
        this.popupService.success('Legal notice created.')
      }
    }).add(() => {
      this.savePending = false
    })
  }

  cancelCreateLegalNotice() {
    this.router.navigate(['admin', 'users', 'community', this.communityId])
  }

}
