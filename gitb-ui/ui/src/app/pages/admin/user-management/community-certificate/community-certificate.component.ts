import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { cloneDeep } from 'lodash';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { ConformanceCertificateSettings } from 'src/app/types/conformance-certificate-settings';
import { FileData } from 'src/app/types/file-data.type';
import { saveAs } from 'file-saver'

@Component({
  selector: 'app-community-certificate',
  templateUrl: './community-certificate.component.html',
  styles: [
  ]
})
export class CommunityCertificateComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  settings: Partial<ConformanceCertificateSettings> = {}
  originalSettings: Partial<ConformanceCertificateSettings>|undefined
  placeholderDomain = Constants.PLACEHOLDER__DOMAIN
  placeholderSpecification = Constants.PLACEHOLDER__SPECIFICATION
  placeholderActor = Constants.PLACEHOLDER__ACTOR
  placeholderOrganisation = Constants.PLACEHOLDER__ORGANISATION
  placeholderSystem = Constants.PLACEHOLDER__SYSTEM
  updatePasswords = false
  removeKeystore = false
  updatePending = false
  testPending = false
  exportPending = false
  loading = false

  constructor(
    private routingService: RoutingService,
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
    this.loading = true
    this.conformanceService.getConformanceCertificateSettings(this.communityId, true)
    .subscribe((data) => {
      if (data?.id != undefined) {
        this.settings = data
        if (this.settings.passwordsSet == undefined || !this.settings.passwordsSet) {
          this.updatePasswords = true
        } else {
          this.updatePasswords = false
        }
        this.originalSettings = cloneDeep(this.settings)
      } else {
        this.updatePasswords = true
      }
      setTimeout(() => {
        this.loading = false
      })
    })
  }

  attachKeystore(file: FileData) {
    if (file != undefined) {
      if (file.size >= Number(this.dataService.configuration.savedFileMaxSize) * 1024) {
        this.errorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for the keystore file is '+this.dataService.configuration.savedFileMaxSize+' KBs.')
      } else {
        this.settings.keystoreFile = file.file
        this.settings.keystoreDefined = true
        this.removeKeystore = false
      }
    }
  }

  downloadKeystore() {
    if (this.settings.keystoreFile != undefined) {
      // Uploaded now.
      saveAs(this.settings.keystoreFile, this.settings.keystoreFile.name)  
    } else {
      // Download from server.
      this.conformanceService.downloadConformanceCertificateKeystore(this.communityId)
      .subscribe((data) => {
        const blobData = new Blob([data], {type: 'application/octet-stream'})
        let fileName = 'keystore'
        if (this.settings.keystoreType == 'JKS') {
          fileName += '.jks'
        } else if (this.settings.keystoreType == 'JCEKS') {
          fileName += '.jceks'
        } else if (this.settings.keystoreType == 'PKCS12') {
          fileName += '.p12'
        }        
        saveAs(blobData, fileName)
      })
    }
  }

  clearKeystore() {
    this.settings.keystoreFile = undefined
    this.settings.keystoreDefined = false    
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
    return this.settings.keystoreDefined && this.settings.keystoreType != undefined && (!this.updatePasswords || (this.textProvided(this.settings.keystorePassword) && this.textProvided(this.settings.keyPassword)))
  }

  update() {
    this.updatePending = true
    let updatePasswords = this.updatePasswords
    let removeKeystore = this.removeKeystore
    if (!this.settings.includeSignature && !this.keystoreSettingsOk()) {
      updatePasswords = false
      removeKeystore = false
      if (this.originalSettings) {
        this.settings.keyPassword = this.originalSettings.keyPassword
        this.settings.keystorePassword = this.originalSettings.keystorePassword
        this.settings.keystoreType = this.originalSettings.keystoreType
      } else {
        this.settings.keyPassword = undefined
        this.settings.keystorePassword = undefined
        this.settings.keystoreType = undefined
      }
    }
    this.conformanceService.updateConformanceCertificateSettings(this.communityId, this.settings as ConformanceCertificateSettings, updatePasswords, removeKeystore)
    .subscribe(() => {
      this.routingService.toCommunity(this.communityId)
      this.popupService.success('Conformance certificate settings updated.')
    }).add(() => {
      this.updatePending = false
    })
  }

  cancel() {
    this.routingService.toCommunity(this.communityId)
  }

}
