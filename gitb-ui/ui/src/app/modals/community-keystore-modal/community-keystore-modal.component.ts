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

import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { CommunityKeystore } from 'src/app/types/community-keystore';
import { FileData } from 'src/app/types/file-data.type';
import { saveAs } from 'file-saver'
import { ConformanceService } from 'src/app/services/conformance.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { PopupService } from 'src/app/services/popup.service';

@Component({
    selector: 'app-community-keystore-modal',
    templateUrl: './community-keystore-modal.component.html',
    styleUrl: './community-keystore-modal.component.less',
    standalone: false
})
export class CommunityKeystoreModalComponent extends BaseComponent implements OnInit {

  @Input() communityId!: number
  @Input() communityKeystore?: CommunityKeystore

  settings: Partial<CommunityKeystore> = {}
  keystoreExists = false
  keystoreDefined = false
  savePending = false
  testPending = false
  deletePending = false
  downloadPending = false
  updatePasswords = false

  constructor(
    private readonly modalInstance: BsModalRef,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly conformanceService: ConformanceService,
    private readonly popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    if (this.communityKeystore) {
      this.settings = this.communityKeystore
      this.keystoreDefined = true
      this.keystoreExists = true
    } else {
      this.updatePasswords = true
    }
  }

  keystoreSettingsOk() {
    return this.settings != undefined && this.settings.keystoreType != undefined && (!this.updatePasswords || (this.textProvided(this.settings.keystorePassword) && this.textProvided(this.settings.keyPassword)))
  }

  getKeystoreName() {
    if (this.settings.keystoreFile != undefined) {
      return this.settings.keystoreFile.name
    } else if (this.keystoreDefined) {
      let fileName = 'keystore'
      if (this.settings.keystoreType == 'JKS') {
        fileName += '.jks'
      } else if (this.settings!.keystoreType == 'JCEKS') {
        fileName += '.jceks'
      } else if (this.settings!.keystoreType == 'PKCS12') {
        fileName += '.p12'
      }
      return fileName
    } else {
      return undefined
    }
  }

  uploadKeystore(file: FileData) {
    if (file != undefined) {
      this.settings.keystoreFile = file.file
      this.keystoreDefined = true
    }
  }

  downloadKeystore() {
    this.clearAlerts()
    const fileName = this.getKeystoreName()
    if (this.settings.keystoreFile != undefined) {
      // Uploaded now.
      saveAs(this.settings.keystoreFile, fileName)
    } else {
      // Download from server.
      this.downloadPending = true
      this.conformanceService.downloadCommunityKeystore(this.communityId)
      .subscribe((data) => {
        const blobData = new Blob([data], {type: 'application/octet-stream'})
        saveAs(blobData, fileName)
      }).add(() => {
        this.downloadPending = false
      })
    }
  }

  deleteKeystore() {
    this.clearAlerts()
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete the keystore?", "Delete", "Cancel").subscribe(() => {
      this.deletePending = true
      this.conformanceService.deleteCommunityKeystore(this.communityId).subscribe(() => {
        this.modalInstance.hide()
        this.popupService.success("Keystore deleted.")
      }).add(() => {
        this.deletePending = true
      })
    })
  }

  testKeystore() {
    this.clearAlerts()
    this.testPending = true
    this.conformanceService.testCommunityKeystore(this.communityId, this.settings)
    .subscribe((result) => {
      if (result?.level == "warning") {
        this.addAlertWarning(result.problem)
      } else if (result?.level == "error") {
        this.addAlertError(result.problem)
      } else {
        this.addAlertSuccess("The keystore configuration is correct.")
      }
    }).add(() => {
      this.testPending = false
    })
  }

  save() {
    this.clearAlerts()
    this.savePending = true
    this.conformanceService.saveCommunityKeystore(this.communityId, this.settings).subscribe(() => {
      this.modalInstance.hide()
      this.popupService.success("Keystore updated.")
    }).add(() => {
      this.savePending = false
    })
  }

  close() {
    this.modalInstance.hide()
  }

}
