import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { HtmlService } from 'src/app/services/html.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { ReportService } from 'src/app/services/report.service';
import { SystemService } from 'src/app/services/system.service';
import { TestService } from 'src/app/services/test.service';
import { Actor } from 'src/app/types/actor';
import { Domain } from 'src/app/types/domain';
import { Specification } from 'src/app/types/specification';
import { ConformanceConfiguration } from './conformance-configuration';
import { ConformanceEndpoint } from './conformance-endpoint';
import { ConformanceTestCase } from './conformance-test-case';
import { ConformanceTestSuite } from './conformance-test-suite';
import { EndpointRepresentation } from './endpoint-representation';
import { cloneDeep, find, map, remove } from 'lodash'
import { ParameterPresetValue } from 'src/app/types/parameter-preset-value';
import { SystemConfigurationParameter } from 'src/app/types/system-configuration-parameter';
import { Organisation } from 'src/app/types/organisation.type';
import { forkJoin, Observable } from 'rxjs'
import { MissingConfigurationModalComponent } from 'src/app/modals/missing-configuration-modal/missing-configuration-modal.component';
import { EditEndpointConfigurationModalComponent } from 'src/app/modals/edit-endpoint-configuration-modal/edit-endpoint-configuration-modal.component';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
  selector: 'app-conformance-statement',
  templateUrl: './conformance-statement.component.html',
  styles: [
  ]
})
export class ConformanceStatementComponent implements OnInit {

  systemId!: number
  actorId!: number
  specId!: number
  organisationId!: number
  loadingStatus = {status: Constants.STATUS.PENDING}
  Constants = Constants
  hasTests = false
  testSuites: ConformanceTestSuite[] = []
  testStatus = ''
  lastUpdate?: string
  conformanceStatus = ''
  allTestsSuccessful = false
  actor?: Actor
  domain?: Domain
  specification?: Specification
  endpoints: ConformanceEndpoint[] = []
  configurations: ConformanceConfiguration[] = []
  endpointRepresentations: EndpointRepresentation[] = []
  hasEndpoints = false
  hasMultipleEndpoints = false
  runTestClicked = false
  endpointsExpanded = false
  backgroundMode = false
  deletePending = false
  exportPending = false
  exportCertificatePending = false

  constructor(
    public dataService: DataService,
    private route: ActivatedRoute,
    private conformanceService: ConformanceService,
    private modalService: BsModalService,
    private systemService: SystemService,
    private confirmationDialogService: ConfirmationDialogService,
    private reportService: ReportService,
    private testService: TestService,
    private popupService: PopupService,
    private htmlService: HtmlService,
    private organisationService: OrganisationService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
    this.actorId = Number(this.route.snapshot.paramMap.get('actor_id'))
    this.specId = Number(this.route.snapshot.paramMap.get('spec_id'))
    this.organisationId = Number(this.route.snapshot.paramMap.get('org_id'))
    const viewPropertiesParam = this.route.snapshot.queryParamMap.get('viewProperties')
    if (viewPropertiesParam != undefined) {
      this.endpointsExpanded = Boolean(viewPropertiesParam)
    }
    // Load conformance results.
    this.conformanceService.getConformanceStatus(this.actorId, this.systemId)
    .subscribe((data) => {
      let testSuiteResults: ConformanceTestSuite[] = []
      let testSuiteIds: number[] = []
      let testSuiteData: {[key: number]: ConformanceTestSuite} = {}
      for (let result of data.items) {
        let testCase: ConformanceTestCase = {
          id: result.testCaseId,
          sname: result.testCaseName,
          description: result.testCaseDescription,
          outputMessage: result.outputMessage,
          hasDocumentation: result.testCaseHasDocumentation,
          sessionId: result.sessionId,
          result: result.result,
          updateTime: result.sessionTime
        }
        if (testSuiteData[result.testSuiteId] == undefined) {
          let currentTestSuite: ConformanceTestSuite = {
            id: result.testSuiteId,
            sname: result.testSuiteName,
            description: result.testSuiteDescription,
            result: result.result,
            hasDocumentation: result.testSuiteHasDocumentation,
            expanded: false,
            testCases: []
          }
          testSuiteIds.push(result.testSuiteId)
          testSuiteData[result.testSuiteId] = currentTestSuite
        } else {
          if (testSuiteData[result.testSuiteId].result == Constants.TEST_CASE_RESULT.SUCCESS) {
            if (testCase.result == Constants.TEST_CASE_RESULT.FAILURE || testCase.result == Constants.TEST_CASE_RESULT.UNDEFINED) {
              testSuiteData[result.testSuiteId].result = testCase.result
            }
          } else if (testSuiteData[result.testSuiteId].result == Constants.TEST_CASE_RESULT.UNDEFINED) {
            if (testCase.result == Constants.TEST_CASE_RESULT.FAILURE) {
              testSuiteData[result.testSuiteId].result = Constants.TEST_CASE_RESULT.FAILURE
            }
          }
        }
        testSuiteData[result.testSuiteId].testCases.push(testCase)
      }
      this.hasTests = data.summary.failed > 0 || data.summary.completed > 0
      for (let testSuiteId of testSuiteIds) {
        testSuiteResults.push(testSuiteData[testSuiteId])
      }
      this.testSuites = testSuiteResults
      this.testStatus = this.dataService.testStatusText(data.summary.completed, data.summary.failed, data.summary.undefined)
      this.lastUpdate = data.summary.updateTime
      this.conformanceStatus = data.summary.result
      this.allTestsSuccessful = data.summary.failed == 0 && data.summary.undefined == 0
    }).add(() => {
      this.loadingStatus.status = Constants.STATUS.FINISHED
    })
    // Load actor.
    this.conformanceService.getActorsWithIds([this.actorId])
    .subscribe((data) => {
      this.actor = data[0]
    })
    // Load domain.
    this.conformanceService.getDomainForSpecification(this.specId)
    .subscribe((data) => {
      this.domain = data
    })
    // Load specification.
    this.conformanceService.getSpecificationsWithIds([this.specId])
    .subscribe((data) => {
      this.specification = data[0]
    })
    // Load configurations.
    this.conformanceService.getSystemConfigurations(this.actorId, this.systemId)
    .subscribe((data) => {
      const endpointsTemp: ConformanceEndpoint[] = []
      const configurations: ConformanceConfiguration[] = []
      for (let endpointConfig of data) {
        let endpoint: ConformanceEndpoint = {
          id: endpointConfig.id,
          name: endpointConfig.name,
          description: endpointConfig.description,
          parameters: []
        }
        for (let parameterConfig of endpointConfig.parameters) {
          endpoint.parameters.push(parameterConfig)
          if (parameterConfig.configured) {
            configurations.push({
              system: this.systemId,
              value: parameterConfig.value,
              endpoint: endpointConfig.id,
              parameter: parameterConfig.id,
              mimeType: parameterConfig.mimeType,
              configured: parameterConfig.configured
            })
          }
        }
        if (endpoint.parameters.length > 0) {
          endpointsTemp.push(endpoint)
        }
      }
      this.endpoints = endpointsTemp
      this.configurations = configurations
      this.constructEndpointRepresentations()
    })
  }

  checkPrerequisite(parameterMap: {[key: string]: SystemConfigurationParameter}, repr: SystemConfigurationParameter): boolean {
    if (repr.checkedPrerequisites == undefined) {
      if (repr.dependsOn != undefined) {
        let otherPrerequisites = this.checkPrerequisite(parameterMap, parameterMap[repr.dependsOn])
        let valueCheck = parameterMap[repr.dependsOn].value == repr.dependsOnValue
        repr.prerequisiteOk = otherPrerequisites && valueCheck
      } else {
        repr.prerequisiteOk = true
      }
      repr.checkedPrerequisites = true
    }
    return repr.prerequisiteOk!
  }

  constructEndpointRepresentations() {
    this.endpointRepresentations = []
    for (let endpoint of this.endpoints) {
      const endpointRepr: EndpointRepresentation = {
        id: endpoint.id,
        name: endpoint.name,
        description: endpoint.description,
        parameters: []
      }
      const parameterMap: {[key: string]: SystemConfigurationParameter} = {}
      for (let parameter of endpoint.parameters) {
        const repr = cloneDeep(parameter)
        const relevantConfig = find(this.configurations, (config) => {
          return Number(parameter.id) == Number(config.parameter) && Number(parameter.endpoint) == Number(config.endpoint)
        })!
        if (relevantConfig != undefined) {
          repr.value = relevantConfig.value
          repr.configured = relevantConfig.configured
        } else {
          repr.configured = false
          repr.value = undefined
        }
        if (repr.configured) {
          if (parameter.kind == 'BINARY') {
            repr.fileName = parameter.name
            if (relevantConfig.mimeType != undefined) {
              repr.fileName += this.dataService.extensionFromMimeType(relevantConfig.mimeType)
            }
            repr.mimeType = relevantConfig.mimeType
          } else if (parameter.kind == 'SECRET') {
            repr.value = '*****'
          } else if (parameter.kind == 'SIMPLE') {
            if (parameter.allowedValues != undefined) {
              const presetValues: ParameterPresetValue[] = JSON.parse(parameter.allowedValues)
              if (presetValues != undefined && presetValues.length > 0) {
                const foundPresetValue = find(presetValues, (v) => { return v.value == repr.value } )
                if (foundPresetValue != undefined) {
                  repr.valueToShow = foundPresetValue.label
                }
              }
            }
          }
        }
        parameterMap[parameter.name] = repr
        if (!parameter.hidden || this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
          endpointRepr.parameters.push(repr)
        }
      }
      let hasVisibleParameters = false
      for (let p of endpointRepr.parameters) {
        if (this.checkPrerequisite(parameterMap, p)) {
          hasVisibleParameters = true
        }
      }
      if (hasVisibleParameters) {
        this.endpointRepresentations.push(endpointRepr)
      }
    }
    this.hasEndpoints = this.endpointRepresentations.length > 0
    this.hasMultipleEndpoints = this.endpointRepresentations.length > 1
  }
  
  onExpand(testSuite: ConformanceTestSuite) {
    if (!this.runTestClicked) {
      testSuite.expanded = !testSuite.expanded
    }
    this.runTestClicked = false
  }

  getOrganisation(): Organisation {
    let organisation = this.dataService.vendor
    if (this.dataService.isCommunityAdmin || this.dataService.isSystemAdmin) {
      organisation = JSON.parse(localStorage.getItem(Constants.LOCAL_DATA.ORGANISATION)!)
    }
    return organisation!
  }

  executeHeadless(testCases: ConformanceTestCase[]) {
    // Check configurations
    const organisationParameterCheck = this.organisationService.checkOrganisationParameterValues(this.getOrganisation().id)
    const systemParameterCheck = this.systemService.checkSystemParameterValues(this.systemId)
    const statementParameterCheck = this.conformanceService.checkConfigurations(this.actorId, this.systemId)
    // Check status once everything is loaded.
    forkJoin([organisationParameterCheck, systemParameterCheck, statementParameterCheck])
    .subscribe((data) => {
      const organisationProperties = data[0]
      const systemProperties = data[1]
      const endpoints = data[2]
      let organisationConfigurationValid = this.dataService.isMemberConfigurationValid(organisationProperties)
      let systemConfigurationValid = this.dataService.isMemberConfigurationValid(systemProperties)
      let configurationValid = this.dataService.isConfigurationValid(endpoints)
      if (!configurationValid || !systemConfigurationValid || !organisationConfigurationValid) {
        // Missing configuration.
        let statementProperties: SystemConfigurationParameter[] = []
        if (endpoints != undefined && endpoints.length > 0) {
          statementProperties = endpoints[0].parameters
        }
        const organisationPropertyVisibility = this.dataService.checkPropertyVisibility(organisationProperties)
        const systemPropertyVisibility = this.dataService.checkPropertyVisibility(systemProperties)
        const statementPropertyVisibility = this.dataService.checkPropertyVisibility(statementProperties)
        const modalRef = this.modalService.show(MissingConfigurationModalComponent, {
          class: 'modal-lg',
          initialState: {
            organisationProperties: organisationProperties,
            organisationConfigurationValid: organisationConfigurationValid,
            systemProperties: systemProperties,
            systemConfigurationValid: systemConfigurationValid,
            endpointRepresentations: endpoints,
            configurationValid: configurationValid,
            organisationPropertyVisibility: organisationPropertyVisibility,
            systemPropertyVisibility: systemPropertyVisibility,
            statementPropertyVisibility: statementPropertyVisibility
          }
        })
        modalRef.content?.action.subscribe((actionType: string) => {
          if (actionType == 'statement') {
            this.endpointsExpanded = true
          } else if (actionType == 'organisation') {
            if (this.dataService.isVendorUser || this.dataService.isVendorAdmin) {
              this.routingService.toOwnOrganisationDetails(true)
            } else {
              const organisation = this.getOrganisation()
              if (this.dataService.vendor!.id == organisation.id) {
                this.routingService.toOwnOrganisationDetails(true)
              } else {
                this.organisationService.getOrganisationBySystemId(this.systemId)
                .subscribe((data) => {
                  this.routingService.toOrganisationDetails(data.community, data.id, true)
                })
              }
            }
          } else if (actionType == 'system') {
            if (this.dataService.isVendorUser) {
              this.routingService.toSystemInfo(this.organisationId, this.systemId, true)
            } else {
              this.routingService.toSystems(this.organisationId, this.systemId, true)
            }
          }
        })
      } else {
        // Proceed with execution.
        const testCaseIds = map(testCases, (test) => { return test.id } )
        this.testService.startHeadlessTestSessions(testCaseIds, this.specId, this.systemId, this.actorId)
        .subscribe(() => {
          if (testCaseIds.length == 1) {
            this.popupService.success('Started test session.<br/>Check <b>Test Sessions</b> for progress.')
          } else {
            this.popupService.success('Started '+testCaseIds.length+' test sessions.<br/>Check <b>Test Sessions</b> for progress.')
          }
        })
      }
    })
  }

  onTestSelect(test: ConformanceTestCase) {
    if (this.backgroundMode) {
      this.executeHeadless([test])
    } else {
      this.dataService.setTestsToExecute([test])
      this.routingService.toTestCaseExecution(this.organisationId, this.systemId, this.actorId, this.specId, test.id)
    }
  }

  onTestSuiteSelect(testSuite?: ConformanceTestSuite) {
    if (testSuite == undefined) {
      testSuite = this.testSuites[0]
    }
    const testsToExecute: ConformanceTestCase[] = []
    for (let testCase of testSuite.testCases) {
      testsToExecute.push(testCase)
    }
    if (this.backgroundMode) {
      this.executeHeadless(testsToExecute)
    } else {
      this.dataService.setTestsToExecute(testsToExecute)
      this.routingService.toTestSuiteExecution(this.organisationId, this.systemId, this.actorId, this.specId, testSuite.id)
    }
  }

  onParameterSelect(parameter: SystemConfigurationParameter) {
    const oldConfiguration = find(this.configurations, (configuration) => {
      return parameter.id == configuration.parameter 
        && configuration.configured 
        && Number(configuration.endpoint) == Number(parameter.endpoint)
    })
    const endpoint = find(this.endpoints, (endpoint) => { return Number(parameter.endpoint) == Number(endpoint.id) })
    const modalRef = this.modalService.show(EditEndpointConfigurationModalComponent, {
      class: 'modal-m',
      initialState: {
        endpoint: endpoint,
        parameter: parameter,
        systemId: this.systemId,
        oldConfiguration: oldConfiguration
      }
    })
    modalRef.content?.action.subscribe((result: {operation: number, configuration: ConformanceConfiguration}) => {
      switch (result.operation) {
        case Constants.OPERATION.ADD:
          if (result.configuration.configured) {
            this.configurations.push(result.configuration)
          }
          break
        case Constants.OPERATION.UPDATE:
          if (oldConfiguration != undefined && result.configuration.configured) {
            oldConfiguration.value = result.configuration.value
            oldConfiguration.configured = result.configuration.configured
            oldConfiguration.mimeType = result.configuration.mimeType
          }
          break
        case Constants.OPERATION.DELETE:
          if (oldConfiguration != undefined) {
            remove(this.configurations, (configuration) => {
              return configuration.parameter == oldConfiguration.parameter &&
                Number(configuration.endpoint) == Number(oldConfiguration.endpoint)
            })
          }
          break
        default:
          break;
      }
      this.constructEndpointRepresentations()
    })
  }

  onParameterDownload(parameter: SystemConfigurationParameter) {
    this.systemService.downloadEndpointConfigurationFile(this.systemId, parameter.id, parameter.endpoint)
    .subscribe((data) => {
      const extension = this.dataService.extensionFromMimeType(parameter.mimeType)
      let fileName = parameter.name + extension      
      const blobData = new Blob([data], {type: parameter.mimeType})
      saveAs(blobData, fileName)
    })
  }

  canDelete() {
    return this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowStatementManagement && (this.dataService.community!.allowPostTestStatementUpdates || !this.hasTests))
  }

  canEditParameter(parameter: SystemConfigurationParameter) {
    return this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && !parameter.adminOnly && (this.dataService.community!.allowPostTestStatementUpdates || !this.hasTests))
  }

  deleteConformanceStatement() {
    this.confirmationDialogService.confirmed("Confirm delete", "Are you sure you want to delete this conformance statement?", "Yes", "No")
    .subscribe(() => {
      this.deletePending = true
      this.systemService.deleteConformanceStatement(this.systemId, [this.actorId])
      .subscribe(() => {
        this.routingService.toConformanceStatements(this.organisationId, this.systemId)
        this.popupService.success('Conformance statement deleted.')
      }).add(() => {
        this.deletePending = false
      })
    })
  }

  onExportConformanceStatement() {
    this.confirmationDialogService.confirm("Report options", "Would you like to include the detailed test step results per test session?", "Yes, include step results", "No, summary only", true)
    .subscribe((choice: boolean) => {
      this.exportPending = true
      this.reportService.exportConformanceStatementReport(this.actorId, this.systemId, choice)
      .subscribe((data) => {
        const blobData = new Blob([data], {type: 'application/pdf'});
        saveAs(blobData, "conformance_report.pdf");
      }).add(() => {
        this.exportPending = false
      })
    })
  }

  onExportConformanceCertificate() {
    this.exportCertificatePending = true
    this.conformanceService.exportOwnConformanceCertificateReport(this.actorId, this.systemId)
    .subscribe((data) => {
      const blobData = new Blob([data], {type: 'application/pdf'});
      saveAs(blobData, "conformance_certificate.pdf");
    }).add(() => {
      this.exportCertificatePending = false
    })
  }

  canExportConformanceCertificate() {
    return this.allTestsSuccessful && (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || this.dataService.community!.allowCertificateDownload)
  }

  showDocumentation(title: string, content: string) {
    this.htmlService.showHtml(title, content)
  }

  showTestCaseDocumentation(testCaseId: number) {
    this.conformanceService.getTestCaseDocumentation(testCaseId)
    .subscribe((data) => {
      this.showDocumentation("Test case documentation", data)
    })
  }

  showTestSuiteDocumentation(testSuiteId: number) {
    this.conformanceService.getTestSuiteDocumentation(testSuiteId)
    .subscribe((data) => {
      this.showDocumentation("Test suite documentation", data)
    })
  }

  toTestSession(sessionId: string) {
    this.routingService.toTestHistory(this.organisationId, this.systemId, sessionId)
  }
}
