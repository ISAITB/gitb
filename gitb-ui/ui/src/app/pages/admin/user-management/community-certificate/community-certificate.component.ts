import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { ConformanceCertificateSettings } from 'src/app/types/conformance-certificate-settings';
import { FileData } from 'src/app/types/file-data.type';

@Component({
  selector: 'app-community-certificate',
  templateUrl: './community-certificate.component.html',
  styles: [
  ]
})
export class CommunityCertificateComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  settings: Partial<ConformanceCertificateSettings> = {}
  placeholderDomain = Constants.PLACEHOLDER__DOMAIN
  placeholderSpecification = Constants.PLACEHOLDER__SPECIFICATION
  placeholderActor = Constants.PLACEHOLDER__ACTOR
  placeholderOrganisation = Constants.PLACEHOLDER__ORGANISATION
  placeholderSystem = Constants.PLACEHOLDER__SYSTEM
  updatePasswords = true
  removeKeystore = false
  updatePending = false
  testPending = false
  exportPending = false

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private conformanceService: ConformanceService,
    public dataService: DataService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    private errorService: ErrorService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('title')
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    this.conformanceService.getConformanceCertificateSettings(this.communityId, true)
    .subscribe((data) => {
      if (data != undefined && data.id != undefined) {
        this.settings = data
        if (this.settings.passwordsSet == undefined || !this.settings.passwordsSet) {
          this.updatePasswords = true
        } else {
          this.updatePasswords = false
        }
      } else {
        this.updatePasswords = true
      }
    })
  }

  attachKeystore(file: FileData) {
    if (file != undefined) {
      if (file.size >= Number(this.dataService.configuration.savedFileMaxSize) * 1024) {
        this.errorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for the keystore file is '+this.dataService.configuration.savedFileMaxSize+' KBs.')
      } else {
        this.settings.keystoreFile = file.data
        this.removeKeystore = false
      }
    }
  }

  downloadKeystore() {
    const base64 = this.dataService.base64FromDataURL(this.settings.keystoreFile!)
    const blob = this.dataService.b64toBlob(base64, 'application/octet-stream')
    let fileName = 'keystore'
    if (this.settings.keystoreType == 'JKS') {
      fileName += '.jks'
    } else if (this.settings.keystoreType == 'JCEKS') {
      fileName += '.jceks'
    } else if (this.settings.keystoreType == 'PKCS12') {
      fileName += '.p12'
    }
    saveAs(blob, fileName)  
  }

  clearKeystore() {
    this.settings.keystoreFile = undefined
    this.settings.keystoreType = undefined
    this.settings.keystorePassword = undefined
    this.settings.keyPassword = undefined
    this.settings.passwordsSet = false
    this.removeKeystore = true
    this.updatePasswords = true  
  }

  testKeystore() {
    this.testPending = true
    this.conformanceService.testKeystoreSettings(this.communityId, this.settings as ConformanceCertificateSettings, this.updatePasswords)
    .subscribe((result) => {
      let title = "Test "
      let message: string
      if (result?.problem != undefined) {
        message = result.problem
        if (!message.endsWith('.')) {
          message += '.'
        }
        if (result.level != 'error') {
          title += "warning"
        } else {
          title += "error"
        }
      } else {
        title += "success"
        message = "The keystore configuration is correct."
      }
      this.confirmationDialogService.notify(title, message, 'Close')
    }).add(() => {
      this.testPending = false
    })
  }

  preview() {
    this.exportPending = true
    this.conformanceService.exportDemoConformanceCertificateReport(this.communityId, this.settings as ConformanceCertificateSettings)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "conformance_certificate.pdf");
    }).add(() => {
      this.exportPending = false
    })
  }

  settingsOk() {
    return !this.settings.includeSignature || this.keystoreSettingsOk()
  }

  keystoreSettingsOk() {
    return this.settings.keystoreFile != undefined && this.settings.keystoreType != undefined && (!this.updatePasswords || (this.textProvided(this.settings.keystorePassword) && this.textProvided(this.settings.keyPassword)))
  }

  update() {
    this.updatePending = true
    this.conformanceService.updateConformanceCertificateSettings(this.communityId, this.settings as ConformanceCertificateSettings, this.updatePasswords, this.removeKeystore)
    .subscribe(() => {
      this.router.navigate(['admin', 'users', 'community', this.communityId])
      this.popupService.success('Conformance certificate settings updated.')
    }).add(() => {
      this.updatePending = false
    })
  }

  cancel() {
    this.router.navigate(['admin', 'users', 'community', this.communityId])
  }

}
