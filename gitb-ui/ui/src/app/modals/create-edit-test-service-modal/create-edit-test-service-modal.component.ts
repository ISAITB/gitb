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

import {AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, ViewChild} from '@angular/core';
import {BaseComponent} from '../../pages/base-component.component';
import {TestServiceWithParameter} from '../../types/test-service-with-parameter';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {ConfirmationDialogService} from '../../services/confirmation-dialog.service';
import {DomainParameterService} from '../../services/domain-parameter.service';
import {PopupService} from '../../services/popup.service';
import {Constants} from '../../common/constants';
import {ValidationState} from '../../types/validation-state';
import {cloneDeep} from 'lodash';
import {Id} from '../../types/id';
import {DataService} from '../../services/data.service';
import {ServiceCallResultHandlerService} from '../../services/service-call-result-handler.service';

@Component({
  selector: 'app-create-edit-test-service-modal',
  standalone: false,
  templateUrl: './create-edit-test-service-modal.component.html'
})
export class CreateEditTestServiceModalComponent extends BaseComponent implements OnInit, AfterViewInit {

  @Input() testService!: Partial<TestServiceWithParameter>
  @Input() domainId!: number
  @Input() updateMatching = false
  public servicesUpdated = new EventEmitter<boolean>()

  @ViewChild("nameField") nameField?: ElementRef;
  title!: string
  pending = false
  savePending = false
  testPending = false
  deletePending = false
  endpointSectionCollapsed = false
  additionalInfoSectionCollapsed = false
  validation = new ValidationState()
  hasBasicAuthentication!: boolean
  hasTokenAuthentication!: boolean
  hasExistingBasicAuthentication!: boolean
  hasExistingTokenAuthentication!: boolean
  isUpdate!: boolean
  updateBasicAuthPassword!: boolean
  updateTokenAuthPassword!: boolean
  basicAuthPasswordMask!: string
  tokenAuthPasswordMask!: string

  constructor(
    private readonly modalInstance: BsModalRef,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly domainParameterService: DomainParameterService,
    private readonly popupService: PopupService,
    public readonly dataService: DataService,
    private readonly serviceCallResultHandlerService: ServiceCallResultHandlerService
  ) { super(); }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.nameField != undefined) {
        this.nameField.nativeElement.focus();
      }
    })
  }

  ngOnInit(): void {
    this.testService = cloneDeep(this.testService)
    if (this.testService.service == undefined) {
      this.testService.service = {
        id: 0,
        serviceType: Constants.TEST_SERVICE_TYPE.VALIDATION,
        apiType: Constants.TEST_SERVICE_API_TYPE.SOAP,
        parameter: 0,
      }
    }
    this.isUpdate = this.testService.service?.id != 0
    this.hasBasicAuthentication = this.isUpdate && this.textProvided(this.testService.service.authBasicUsername)
    this.hasTokenAuthentication = this.isUpdate && this.textProvided(this.testService.service.authTokenUsername)
    this.hasExistingBasicAuthentication = this.hasBasicAuthentication
    this.hasExistingTokenAuthentication = this.hasTokenAuthentication
    this.basicAuthPasswordMask = this.hasBasicAuthentication?'*****':''
    this.tokenAuthPasswordMask = this.hasTokenAuthentication?'*****':''
    this.updateBasicAuthPassword = !this.isUpdate || !this.hasExistingBasicAuthentication
    this.updateTokenAuthPassword = !this.isUpdate || !this.hasExistingTokenAuthentication
    if (this.testService.service.authTokenPasswordType == undefined) {
      this.testService.service.authTokenPasswordType = Constants.TEST_SERVICE_AUTH_TOKEN_PASSWORD_TYPE.DIGEST
    }
    if (this.testService.parameter == undefined) {
      this.testService.parameter = {
        id: 0,
        name: "",
        inTests: true,
        isTestService: true,
        kind: "SIMPLE",
        value: ""
      }
    }
    if (this.testService.service?.id! != 0) {
      this.title = 'Update test service definition'
    } else {
      this.title = 'Register test service definition'
    }
  }

  saveAllowed() {
    return this.textProvided(this.testService.parameter!.name) && this.serviceSettingsOk()
  }

  serviceSettingsOk() {
    return this.textProvided(this.testService.parameter?.value) && this.authSettingsOk()
  }

  validateData(serviceData: Partial<TestServiceWithParameter>): boolean {
    let nameCheck = Constants.VARIABLE_NAME_REGEX.test(serviceData.parameter!.name!)
    if (!nameCheck) {
      this.validation.invalid('name', 'The service name must begin with a character followed by zero or more characters, digits, or one of [\'.\', \'_\', \'-\'].')
    }
    let addressCheck = this.validateServiceData(serviceData)
    return nameCheck && addressCheck
  }

  validateServiceData(serviceData: Partial<TestServiceWithParameter>): boolean {
    if (!this.isValidAbsoluteHttpUrl(serviceData.parameter?.value)) {
      this.validation.invalid("value", "The service address must be a valid absolute HTTP url.")
      return false
    }
    return true
  }

  private authSettingsOk() {
    return (!this.hasBasicAuthentication || (this.textProvided(this.testService.service!.authBasicUsername)
            && ((!this.updateBasicAuthPassword && this.hasExistingBasicAuthentication) || (this.updateBasicAuthPassword && this.textProvided(this.testService.service!.authBasicPassword)))))
        && (!this.hasTokenAuthentication || (this.textProvided(this.testService.service!.authTokenUsername)
            && ((!this.updateTokenAuthPassword && this.hasExistingTokenAuthentication) || (this.updateTokenAuthPassword && this.textProvided(this.testService.service!.authTokenPassword)))))
  }

  private doCreate(serviceData: TestServiceWithParameter, updateExistingParameter: boolean) {
    this.domainParameterService.createTestService(serviceData, this.domainId, updateExistingParameter)
      .subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.validation.applyError(data)
        } else if (this.isMatchingParameterId(data)) {
          this.confirmationDialogService.confirmed("Matching parameter found", "A parameter was found with a name matching the provided identifier. Do you want to replace it with this test service?", "Replace", "Cancel")
            .subscribe(() => {
              this.doCreate(serviceData, true)
            })
        } else {
          this.servicesUpdated.emit(true)
          this.modalInstance.hide()
          this.popupService.success('Test service registered.')
        }
      }).add(() => {
      this.pending = false
      this.savePending = false
    })
  }

  private doUpdate(serviceData: TestServiceWithParameter, updateExistingParameter: boolean) {
    this.domainParameterService.updateTestService(serviceData, this.domainId, updateExistingParameter)
      .subscribe((data) => {
        if (this.isErrorDescription(data)) {
          this.validation.applyError(data)
        } else if (this.isMatchingParameterId(data)) {
          this.confirmationDialogService.confirmed("Matching parameter found", "An existing parameter was found with the same name. Do you want to replace it with this test service?", "Replace", "Cancel")
            .subscribe(() => {
              this.doUpdate(serviceData, true)
            })
        } else {
          this.servicesUpdated.emit(true)
          this.modalInstance.hide()
          this.popupService.success('Test service updated.')
        }
      }).add(() => {
      this.pending = false
      this.savePending = false
    })
  }

  private prepareServiceDataForSubmission() {
    const serviceData = cloneDeep(this.testService) as TestServiceWithParameter
    if (this.hasBasicAuthentication) {
      if (!this.updateBasicAuthPassword) {
        serviceData.service.authBasicPassword = undefined
      }
    } else {
      serviceData.service.authBasicUsername = undefined
      serviceData.service.authBasicPassword = undefined
    }
    if (this.hasTokenAuthentication) {
      if (!this.updateTokenAuthPassword) {
        serviceData.service.authTokenPassword = undefined
      }
    } else {
      serviceData.service.authTokenUsername = undefined
      serviceData.service.authTokenPassword = undefined
      serviceData.service.authTokenPasswordType = undefined
    }
    return serviceData
  }

  save() {
    this.validation.clearErrors()
    if (this.saveAllowed() && this.validateData(this.testService)) {
      const serviceData = this.prepareServiceDataForSubmission()
      this.pending = true
      this.savePending = true
      if (this.testService.service!.id != 0) {
        // Update
        this.doUpdate(serviceData, this.updateMatching)
      } else {
        // Create
        this.doCreate(serviceData, this.updateMatching)
      }
    }
  }

  delete() {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this test service?", "Delete", "Cancel")
      .subscribe(() => {
        this.pending = true
        this.deletePending = true
        this.domainParameterService.deleteTestService(this.domainId, this.testService.service?.id!)
          .subscribe(() => {
            this.servicesUpdated.emit(true)
            this.modalInstance.hide()
            this.popupService.success('Test service deleted.')
          }).add(() => {
          this.pending = false
          this.deletePending = false
        })
      })
  }

  cancel() {
    this.servicesUpdated.emit(false)
    this.modalInstance.hide()
  }

  private isMatchingParameterId(obj: Id|any): obj is Id {
    return obj != undefined && ((obj as Id).id != undefined)
  }

  test() {
    this.validation.clearErrors()
    if (this.serviceSettingsOk() && this.validateServiceData(this.testService)) {
      const serviceData = this.prepareServiceDataForSubmission()
      this.testPending = true
      this.domainParameterService.testTestService(serviceData, this.domainId).subscribe((result) => {
        this.serviceCallResultHandlerService.handleResult(result)
      }).add(() => {
        this.pending = false
        this.testPending = false
      })
    }
  }

}
