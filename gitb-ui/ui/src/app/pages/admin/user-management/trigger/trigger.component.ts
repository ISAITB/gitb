import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { CommunityService } from 'src/app/services/community.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { TriggerService } from 'src/app/services/trigger.service';
import { IdLabel } from 'src/app/types/id-label';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { SystemParameter } from 'src/app/types/system-parameter';
import { Trigger } from 'src/app/types/trigger';
import { remove } from 'lodash'
import { forkJoin, Observable } from 'rxjs';
import { DomainParameter } from 'src/app/types/domain-parameter';
import { map, share } from 'rxjs/operators';
import { TriggerDataItem } from 'src/app/types/trigger-data-item';
import { ErrorDescription } from 'src/app/types/error-description';
import { CodeEditorModalComponent } from 'src/app/components/code-editor-modal/code-editor-modal.component';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { RoutingService } from 'src/app/services/routing.service';
import { CommunityTab } from '../community/community-details/community-tab.enum';
import { StatementParameterMinimal } from 'src/app/types/statement-parameter-minimal';
import { TestTriggerModalComponent } from './test-trigger-modal/test-trigger-modal.component';

@Component({
  selector: 'app-trigger',
  templateUrl: './trigger.component.html',
  styleUrls: [ './trigger.component.less' ]
})
export class TriggerComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  triggerId?: number
  update = false
  savePending = false
  testPending = false
  deletePending = false
  previewPending = false
  clearStatusPending = false
  statusTextOk = {id: 1, msg: 'Success'}
  statusTextError = {id: 2, msg: 'Error'}
  statusTextUnknown = {id: 0, msg: 'None'}

  trigger: Partial<Trigger> = {}
  organisationParameters: OrganisationParameter[] = []
  systemParameters: SystemParameter[] = []
  statementParameters: StatementParameterMinimal[] = []
  domainParameters: DomainParameter[] = []
  dataTypes!: IdLabel[]
  dataTypeMap!: {[key: number]: string}
  organisationParameterMap: {[key: number]: OrganisationParameter} = {}
  systemParameterMap: {[key: number]: SystemParameter} = {}
  statementParameterMap: {[key: number]: StatementParameterMinimal} = {}
  domainParameterMap: {[key: number]: DomainParameter} = {}

  triggerData = {
    community: {dataType: Constants.TRIGGER_DATA_TYPE.COMMUNITY, visible: true, selected: false},
    organisation: {dataType: Constants.TRIGGER_DATA_TYPE.ORGANISATION, visible: true, selected: false},
    system: {dataType: Constants.TRIGGER_DATA_TYPE.SYSTEM, visible: true, selected: false},
    specification: {dataType: Constants.TRIGGER_DATA_TYPE.SPECIFICATION, visible: true, selected: false},
    actor: {dataType: Constants.TRIGGER_DATA_TYPE.ACTOR, visible: true, selected: false},
    organisationParameter: {dataType: Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER, visible: true, selected: false},
    systemParameter: {dataType: Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER, visible: true, selected: false},
    domainParameter: {dataType: Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER, visible: true, selected: false},
    statementParameter: {dataType: Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER, visible: true, selected: false},
    testSession: {dataType: Constants.TRIGGER_DATA_TYPE.TEST_SESSION, visible: true, selected: false},
    testReport: {dataType: Constants.TRIGGER_DATA_TYPE.TEST_REPORT, visible: true, selected: false}
  }
  Constants = Constants

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private modalService: BsModalService,
    private triggerService: TriggerService,
    private conformanceService: ConformanceService,
    private communityService: CommunityService,
    private confirmationDialogService: ConfirmationDialogService,
    private popupService: PopupService,
    public dataService: DataService
  ) {
    super();
  }
  ngAfterViewInit(): void {
    this.dataService.focus('name')
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    let triggerIdParam = this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.TRIGGER_ID)
    if (triggerIdParam != undefined) {
      this.triggerId = Number(triggerIdParam)
      this.update = true
    } else {
      this.trigger.serviceType = Constants.TRIGGER_SERVICE_TYPE.GITB
    }
    this.dataTypes = this.dataService.triggerDataTypes()
    this.dataTypeMap = this.dataService.idToLabelMap(this.dataTypes)

    const loadPromises: Observable<any>[] = []

    loadPromises.push(this.communityService.getOrganisationParameters(this.communityId).pipe(
      map((data) => {
        this.organisationParameters = data
        for (let parameter of this.organisationParameters) {
          parameter.selected = false
          this.organisationParameterMap[parameter.id] = parameter
        }
        if (this.organisationParameters.length == 0) {
          remove(this.dataTypes, (current) => current.id == Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER)
        }
      }),
      share()
    ))
    loadPromises.push(this.communityService.getSystemParameters(this.communityId).pipe(
      map((data) => {
        this.systemParameters = data
        for (let parameter of this.systemParameters) {
          parameter.selected = false
          this.systemParameterMap[parameter.id] = parameter
        }
        if (this.systemParameters.length == 0) {
          remove(this.dataTypes, (current) => current.id == Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER)
        }
      }),
      share()
    ))
    loadPromises.push(this.conformanceService.getStatementParametersOfCommunity(this.communityId).pipe(
      map((data) => {
        this.statementParameters = data
        for (let parameter of this.statementParameters) {
          parameter.selected = false
          this.statementParameterMap[parameter.id] = parameter
        }
        if (this.statementParameters.length == 0) {
          remove(this.dataTypes, (current) => current.id == Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER)
        }
      }),
      share()
    ))    

    let domainParameterFnResult: Observable<DomainParameter[]>|undefined
    if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId != undefined) {
      domainParameterFnResult = this.conformanceService.getDomainParameters(this.dataService.community!.domainId, false, false)
    } else if (this.dataService.isSystemAdmin) {
      domainParameterFnResult = this.conformanceService.getDomainParametersOfCommunity(this.communityId, false, false)
    }
    if (domainParameterFnResult != undefined) {
      loadPromises.push(domainParameterFnResult.pipe(
        map((data) => {
          this.domainParameters = data
          for (let parameter of this.domainParameters) {
            parameter.selected = false
            this.domainParameterMap[parameter.id] = parameter
          }
          if (this.domainParameters.length == 0) {
            remove(this.dataTypes, (current) => current.id == Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER)
          }
        }),
        share()
      ))
    }

    forkJoin(loadPromises).subscribe(() => {
      if (this.update) {
        this.triggerService.getTriggerById(this.triggerId!)
        .subscribe((data) => {
          this.trigger = data.trigger
          if (this.trigger.latestResultOk != undefined) {
            if (this.trigger.latestResultOk) {
              this.applyStatusValues(this.statusTextOk)
            } else {
              this.applyStatusValues(this.statusTextError)
            }
          } else {
            this.applyStatusValues(this.statusTextUnknown)
          }
          if (data.data != undefined) {
            for (let item of data.data) {
              if (item.dataType == Constants.TRIGGER_DATA_TYPE.COMMUNITY) {
                this.triggerData.community.selected = true
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.ORGANISATION) {
                this.triggerData.organisation.selected = true
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.SYSTEM) {
                this.triggerData.system.selected = true
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.SPECIFICATION) {
                this.triggerData.specification.selected = true
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.ACTOR) {
                this.triggerData.actor.selected = true
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.TEST_SESSION) {
                this.triggerData.testSession.selected = true
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.TEST_REPORT) {
                this.triggerData.testReport.selected = true
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER) {
                this.triggerData.organisationParameter.selected = true
                if (this.organisationParameterMap[item.dataId] != undefined) {
                  this.organisationParameterMap[item.dataId].selected = true
                }
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER) {
                this.triggerData.systemParameter.selected = true
                if (this.systemParameterMap[item.dataId] != undefined) {
                  this.systemParameterMap[item.dataId].selected = true
                }
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER) {
                this.triggerData.domainParameter.selected = true
                if (this.domainParameterMap[item.dataId] != undefined) {
                  this.domainParameterMap[item.dataId].selected = true
                }
              } else if (item.dataType == Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER) {
                this.triggerData.statementParameter.selected = true
                if (this.statementParameterMap[item.dataId] != undefined) {
                  this.statementParameterMap[item.dataId].selected = true
                }
              }
            }
          }
          this.eventTypeChanged()
        })
      } else {
        this.eventTypeChanged()
      }
    })
  }

  saveDisabled() {
    return !(
      !this.savePending && !this.deletePending && this.textProvided(this.trigger.name) && this.textProvided(this.trigger.url) && this.trigger.eventType != undefined && this.trigger.serviceType != undefined
    )
  }

  deleteDisabled() {
    return !(
      !this.savePending && !this.deletePending
    )
  }

  previewDisabled() {
    return this.saveDisabled() || this.previewPending
  }

  clearStatusDisabled() {
    return this.saveDisabled() || this.clearStatusPending
  }

  dataItemsToSave() {
    const dataItems: TriggerDataItem[] = []
    if (this.triggerData.community.visible && this.triggerData.community.selected) {
      dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.COMMUNITY, dataId: -1})
    }
    if (this.triggerData.organisation.visible && this.triggerData.organisation.selected) {
      dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.ORGANISATION, dataId: -1})
    }
    if (this.triggerData.system.visible && this.triggerData.system.selected) {
      dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.SYSTEM, dataId: -1})
    }
    if (this.triggerData.specification.visible && this.triggerData.specification.selected) {
      dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.SPECIFICATION, dataId: -1})
    }
    if (this.triggerData.actor.visible && this.triggerData.actor.selected) {
      dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.ACTOR, dataId: -1})
    }
    if (this.triggerData.testSession.visible && this.triggerData.testSession.selected) {
      dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.TEST_SESSION, dataId: -1})
    }
    if (this.triggerData.testReport.visible && this.triggerData.testReport.selected) {
      dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.TEST_REPORT, dataId: -1})
    }
    if (this.triggerData.organisationParameter.visible && this.triggerData.organisationParameter.selected) {
      for (let parameter of this.organisationParameters) {
        if (parameter.selected) {
          dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.ORGANISATION_PARAMETER, dataId: parameter.id})
        }
      }
    }
    if (this.triggerData.systemParameter.visible && this.triggerData.systemParameter.selected) {
      for (let parameter of this.systemParameters) {
        if (parameter.selected) {
          dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.SYSTEM_PARAMETER, dataId: parameter.id})
        }
      }
    }
    if (this.triggerData.domainParameter.visible && this.triggerData.domainParameter.selected) {
      for (let parameter of this.domainParameters) {
        if (parameter.selected) {
          dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.DOMAIN_PARAMETER, dataId: parameter.id})
        }
      }
    }
    if (this.triggerData.statementParameter.visible && this.triggerData.statementParameter.selected) {
      for (let parameter of this.statementParameters) {
        if (parameter.selected) {
          dataItems.push({dataType: Constants.TRIGGER_DATA_TYPE.STATEMENT_PARAMETER, dataId: parameter.id})
        }
      }
    }
    return dataItems
  }

  save() {
    this.clearAlerts()
    this.savePending = true
    let callResult: Observable<ErrorDescription|undefined>
    if (this.update) {
      callResult = this.triggerService.updateTrigger(this.triggerId!, this.trigger.name!, this.trigger.description, this.trigger.operation, this.trigger.active, this.trigger.url!, this.trigger.eventType!, this.trigger.serviceType!, this.communityId, this.dataItemsToSave())
    } else {
      callResult = this.triggerService.createTrigger(this.trigger.name!, this.trigger.description, this.trigger.operation, this.trigger.active, this.trigger.url!, this.trigger.eventType!, this.trigger.serviceType!, this.communityId, this.dataItemsToSave())
    }
    callResult.subscribe((data) => {
      if (data?.error_code != undefined) {
        this.addAlertError(data.error_description)
      } else {
        if (this.update) {
          this.popupService.success('Trigger updated.')
        } else {
          this.back()
          this.popupService.success('Trigger created.')
        }
      }
    }).add(() => {
      this.savePending = false
    })
  }

  delete() {
    this.clearAlerts()
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this trigger?", "Delete", "Cancel")
    .subscribe(() => {
      this.deletePending = true
      this.triggerService.deleteTrigger(this.triggerId!)
      .subscribe(() => {
        this.back()
        this.popupService.success('Trigger deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  back() {
    this.routingService.toCommunity(this.communityId, CommunityTab.triggers)
  }

  testEndpoint() {
    this.testPending = true
    this.triggerService.testTriggerEndpoint(this.trigger.url!, this.trigger.serviceType!, this.communityId)
    .subscribe((result) => {
      if (result.success) {
        this.modalService.show(CodeEditorModalComponent, {
          class: 'modal-lg',
          initialState: {
            documentName: 'Test success',
            editorOptions: {
              value: (result.contentType == 'application/json')? this.dataService.prettifyJSON(result.texts[0]) : result.texts[0],
              readOnly: true,
              lineNumbers: true,
              smartIndent: false,
              electricChars: false,
              mode: result.contentType
            }
          }
        })
      } else {
        this.popupErrorsArray(result.texts, result.contentType)
      }
    }).add(() => {
      this.testPending = false
    })
  }

  preview() {
    this.previewPending = true
    this.triggerService.preview(this.trigger.operation, this.trigger.serviceType!, this.dataItemsToSave(), this.communityId)
    .subscribe((result) => {
      this.modalService.show(TestTriggerModalComponent, {
        class: 'modal-lg',
        initialState: {
          request: result.message,
          communityId: this.communityId,
          url: this.trigger.url,
          serviceType: this.trigger.serviceType!
        }
      })
    }).add(() => {
      this.previewPending = false
    })
  }

  clearStatus() {
    this.clearStatusPending = true
    this.triggerService.clearStatus(this.trigger.id!)
    .subscribe((result) => {
      this.trigger.latestResultOk = undefined
      this.applyStatusValues(this.statusTextUnknown)
      this.popupService.success('Trigger status cleared.')
    }).add(() => {
      this.clearStatusPending = false
    })
  }

  popupErrors(errorJson: string|undefined) {
    let arrayToUse: string[]|undefined
    if (errorJson != undefined) {
      const output = JSON.parse(errorJson)
      arrayToUse = output.texts
    }
    this.popupErrorsArray(arrayToUse)
  }

  popupErrorsArray(errorArray: string[]|undefined, contentType?: string) {
    let content = this.dataService.errorArrayToString(errorArray)
    if (contentType == undefined) {
      contentType = 'text/plain'
    } else if (contentType == 'application/json') {
      content = this.dataService.prettifyJSON(content)
    }
    this.modalService.show(CodeEditorModalComponent, {
      class: 'modal-lg',
      initialState: {
        documentName: 'Error message(s)',
        editorOptions: {
          value: content,
          readOnly: true,
          copy: true,
          lineNumbers: false,
          smartIndent: false,
          electricChars: false,
          styleClass: 'editor-short',
          mode: contentType
        }
      }
    })
  }

  viewLatestErrors() {
    this.popupErrors(this.trigger.latestResultOutput)
  }

  applyStatusValues(statusToApply: {id: number, msg: string}) {
    this.trigger.status = statusToApply.id
    this.trigger.statusText = statusToApply.msg
  }

  eventTypeChanged() {
    let eventType: number|undefined
    if (this.trigger.eventType != undefined) {
      eventType = this.trigger.eventType
    }
    this.triggerData.community.visible = eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.community.dataType)
    this.triggerData.organisation.visible = eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.organisation.dataType)
    this.triggerData.system.visible = eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.system.dataType)
    this.triggerData.specification.visible = eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.specification.dataType)
    this.triggerData.actor.visible = eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.actor.dataType)
    this.triggerData.testSession.visible = eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.testSession.dataType)
    this.triggerData.testReport.visible = eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.testReport.dataType)
    this.triggerData.organisationParameter.visible = this.organisationParameters.length > 0 && (eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.organisationParameter.dataType))
    this.triggerData.systemParameter.visible = this.systemParameters.length > 0 && (eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.systemParameter.dataType))
    this.triggerData.domainParameter.visible = this.domainParameters.length > 0 && (eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.domainParameter.dataType))
    this.triggerData.statementParameter.visible = this.statementParameters.length > 0 && (eventType == undefined || this.dataService.triggerDataTypeAllowedForEvent(eventType, this.triggerData.statementParameter.dataType))
  }

  parameterType(parameter: CustomProperty|DomainParameter|StatementParameterMinimal) {
    if (parameter.kind == 'SIMPLE') {
      parameter.kindLabel = 'Simple'
    } else if (parameter.kind == 'BINARY') {
      parameter.kindLabel = 'Binary' 
    } else {
      parameter.kindLabel = 'Secret'
    }
    return parameter.kindLabel
  }

}
