import { Component, EventEmitter } from '@angular/core';
import { BaseCertificateSettingsFormComponent } from '../base-certificate-settings-form.component';
import { ConformanceOverviewCertificateSettings } from 'src/app/types/conformance-overview-certificate-settings';
import { Observable, forkJoin, map, mergeMap, share } from 'rxjs';
import { PlaceholderInfo } from 'src/app/components/placeholder-selector/placeholder-info';
import { Constants } from 'src/app/common/constants';
import { ConformanceService } from 'src/app/services/conformance.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { PopupService } from 'src/app/services/popup.service';
import { DataService } from 'src/app/services/data.service';
import { Domain } from 'src/app/types/domain';
import { SpecificationService } from 'src/app/services/specification.service';
import { SpecificationGroup } from 'src/app/types/specification-group';
import { Specification } from 'src/app/types/specification';
import { filter, find } from 'lodash';
import { ConformanceOverviewMessage } from '../conformance-overview-message';
import { ReportService } from 'src/app/services/report.service';
import { HttpResponse } from '@angular/common/http';
import { ErrorService } from 'src/app/services/error.service';

@Component({
    selector: 'app-conformance-overview-certificate-form',
    templateUrl: './conformance-overview-certificate-form.component.html',
    standalone: false
})
export class ConformanceOverviewCertificateFormComponent extends BaseCertificateSettingsFormComponent<ConformanceOverviewCertificateSettings> {

  domainsPending = false
  groupsPending = false
  specificationsPending = false

  messageLevelSelectionCollapsed = true
  specificMessageSettingPending = false
  specificMessageSetting = false
  anyLevelEnabled = false
  messageBlockAnimated = true
  levelAnimated = false

  messageLevel: 'all'|'domain'|'group'|'specification' = 'all'
  domains?: Domain[]
  groups?: SpecificationGroup[]
  specifications?: Specification[]
  domainId?: number
  groupId?: number
  specificationId?: number

  allDomains?: Domain[]
  communityDomainId?: number
  allGroups?: SpecificationGroup[]
  allSpecifications?: Specification[]
  groupsPerDomain = new Map<number, SpecificationGroup[]>()
  specsPerDomain = new Map<number, Specification[]>()
  specsPerGroup = new Map<number, Specification[]>()
  specificationSelectLabel?: string
  noGroup = -1
  domainChangedEmitter = new EventEmitter<number>()

  aggregateMessage?: ConformanceOverviewMessage
  defaultDomainMessage?: ConformanceOverviewMessage
  defaultGroupMessage?: ConformanceOverviewMessage
  defaultSpecificationMessage?: ConformanceOverviewMessage
  specificDomainMessages = new Map<number, ConformanceOverviewMessage>()
  specificGroupMessages = new Map<number, ConformanceOverviewMessage>()
  specificSpecificationMessages = new Map<number, ConformanceOverviewMessage>()

  currentMessageKey?: ConformanceOverviewMessage
  currentMessageContent?: string

  constructor(
    reportService: ReportService,
    conformanceService: ConformanceService,
    modalService: BsModalService,
    popupService: PopupService,
    public dataService: DataService,
    private specificationService: SpecificationService,
    errorService: ErrorService
  ) { super(conformanceService, modalService, popupService, reportService, errorService) }

  getSettings(): Observable<ConformanceOverviewCertificateSettings | undefined> {
    return this.conformanceService.getConformanceOverviewCertificateSettings(this.communityId)
  }

  prepareSettingsForUse(): ConformanceOverviewCertificateSettings {
    this.applyMessage(this.currentMessageKey, this.currentMessageContent)
    const settingsToUse: ConformanceOverviewCertificateSettings = {
      title: this.settings!.title,
      includeTitle: this.settings!.includeTitle == true,
      includeDetails: this.settings!.includeDetails == true,
      includeMessage: this.settings!.includeMessage == true,
      includePageNumbers: this.settings!.includePageNumbers == true,
      includeSignature: this.settings!.includeSignature == true,
      includeTestCases: this.settings!.includeTestCases == true,
      includeTestCaseDetails: this.settings!.includeTestCaseDetails == true,
      includeTestStatus: this.settings!.includeTestStatus == true,
      enableAllLevel: this.settings!.enableAllLevel == true,
      enableDomainLevel: this.settings!.enableDomainLevel == true && this.communityDomainId == undefined,
      enableGroupLevel: this.settings!.enableGroupLevel == true,
      enableSpecificationLevel: this.settings!.enableSpecificationLevel == true,
      community: this.communityId
    }
    // Include custom messages
    if (settingsToUse.includeMessage) {
      settingsToUse.messages = []
      // Aggregate level
      if (this.settings?.enableAllLevel && this.textProvided(this.aggregateMessage?.message)) {
        settingsToUse.messages.push({ id: this.aggregateMessage?.id, level: "all", message: this.aggregateMessage?.message })
      }
      // Domain level
      if (this.settings?.enableDomainLevel) {
        if (this.textProvided(this.defaultDomainMessage?.message)) {
          settingsToUse.messages.push({ id: this.defaultDomainMessage?.id, level: "domain", message: this.defaultDomainMessage?.message })
        }
        for (let entry of this.specificDomainMessages.entries()) {
          if (this.textProvided(entry[1].message)) settingsToUse.messages.push({ id: entry[1].id, level: "domain", identifier: entry[0], message: entry[1].message })
        }
      }
      // Group level
      if (this.settings?.enableGroupLevel) {
        if (this.textProvided(this.defaultGroupMessage?.message)) {
          settingsToUse.messages.push({ id: this.defaultGroupMessage?.id, level: "group", message: this.defaultGroupMessage?.message })
        }
        for (let entry of this.specificGroupMessages.entries()) {
          if (this.textProvided(entry[1].message)) settingsToUse.messages.push({ id: entry[1].id, level: "group", identifier: entry[0], message: entry[1].message })
        }
      }
      // Specification level
      if (this.settings?.enableSpecificationLevel) {
        if (this.textProvided(this.defaultSpecificationMessage?.message)) {
          settingsToUse.messages.push({ id: this.defaultSpecificationMessage?.id, level: "specification", message: this.defaultSpecificationMessage?.message })
        }
        for (let entry of this.specificSpecificationMessages.entries()) {
          if (this.textProvided(entry[1].message)) settingsToUse.messages.push({ id: entry[1].id, level: "specification", identifier: entry[0], message: entry[1].message })
        }
      }
    }
    return settingsToUse
  }

  exportDemoReport(level: string): Observable<HttpResponse<ArrayBuffer>> {
    let identifier: number|undefined
    if (this.currentMessageKey?.level == level) {
      identifier = this.currentMessageKey.identifier
    }
    return this.reportService.exportDemoConformanceOverviewCertificateReport(this.communityId, this.reportSettings!, this.prepareSettingsForUse(), level, identifier, this.uploadedStylesheet)
  }

  updateSettings(): Observable<any> {
    return this.reportService.updateConformanceOverviewCertificateSettings(this.communityId, this.reportSettings!, this.prepareSettingsForUse(), this.uploadedStylesheet)
  }

  private placeholderForDomain(indexed?: boolean) {
    if (indexed == true) {
      return { key: Constants.PLACEHOLDER__DOMAIN+'{index}', value: 'The name of the ' + this.dataService.labelDomainLower() + ' at the specific order index.', select: () => Constants.PLACEHOLDER__DOMAIN+'{0}' }
    } else {
      return { key: Constants.PLACEHOLDER__DOMAIN, value: 'The name of the ' + this.dataService.labelDomainLower() + '.' }
    }
  }

  private placeholderForSpecificationGroup(indexed?: boolean) {
    if (indexed == true) {
      return { key: Constants.PLACEHOLDER__SPECIFICATION_GROUP+'{index}', value: 'The name of the ' + this.dataService.labelSpecificationGroupLower() + ' at the specific order index.', select: () => Constants.PLACEHOLDER__SPECIFICATION_GROUP+'{0}' }
    } else {
      return { key: Constants.PLACEHOLDER__SPECIFICATION_GROUP, value: 'The name of the ' + this.dataService.labelSpecificationGroupLower() + '.' }
    }
  }

  private placeholderForSpecification(indexed?: boolean) {
    if (indexed == true) {
      return { key: Constants.PLACEHOLDER__SPECIFICATION+'{index}', value: 'The name of the ' + this.dataService.labelSpecificationLower() + ' at the specific order index.', select: () => Constants.PLACEHOLDER__SPECIFICATION+'{0}' }
    } else {
      return { key: Constants.PLACEHOLDER__SPECIFICATION, value: 'The name of the ' + this.dataService.labelSpecificationLower() + '.' }
    }
  }

  private placeholderForSpecificationGroupOption(indexed?: boolean) {
    if (indexed == true) {
      return { key: Constants.PLACEHOLDER__SPECIFICATION_GROUP_OPTION+'{index}', value: 'The name of the ' + this.dataService.labelSpecificationInGroupLower() + ' at the specific order index.', select: () => Constants.PLACEHOLDER__SPECIFICATION_GROUP_OPTION+'{0}' }
    } else {
      return { key: Constants.PLACEHOLDER__SPECIFICATION_GROUP_OPTION, value: 'The name of the ' + this.dataService.labelSpecificationInGroupLower() + '.' }
    }
  }

  private placeholderForActor(indexed?: boolean) {
    if (indexed == true) {
      return { key: Constants.PLACEHOLDER__ACTOR+'{index}', value: 'The name of the ' + this.dataService.labelActorLower() + ' at the specific order index.', select: () => Constants.PLACEHOLDER__ACTOR+'{0}' }
    } else {
      return { key: Constants.PLACEHOLDER__ACTOR, value: 'The name of the ' + this.dataService.labelActorLower() + '.' }
    }
  }

  getPlaceholders(): PlaceholderInfo[] {
    const placeholders: PlaceholderInfo[] = []
    if (this.messageLevel == "all") {
      if (this.communityDomainId != undefined) {
        placeholders.push(this.placeholderForDomain())
      } else {
        placeholders.push(this.placeholderForDomain(true))
      }
      placeholders.push(this.placeholderForSpecification(true))
      placeholders.push(this.placeholderForSpecificationGroup(true))
    } else if (this.messageLevel == "domain") {
      // We have a specific domain.
      placeholders.push(this.placeholderForDomain())
      placeholders.push(this.placeholderForSpecification(true))
      placeholders.push(this.placeholderForSpecificationGroup(true))
    } else if (this.messageLevel == "group") {
      placeholders.push(this.placeholderForDomain())
      placeholders.push(this.placeholderForSpecificationGroup())
      placeholders.push(this.placeholderForSpecificationGroupOption(true))
    } else if (this.messageLevel == "specification") {
      placeholders.push(this.placeholderForDomain())
      if (!this.specificMessageSetting) {
        placeholders.push(this.placeholderForSpecification())
        placeholders.push(this.placeholderForSpecificationGroup())
        placeholders.push(this.placeholderForSpecificationGroupOption())
      } else {
        // Specific specification.
        if (this.groupId == this.noGroup || this.groupId == undefined) {
          // Specification without a group.
          placeholders.push(this.placeholderForSpecification())
        } else {
          // Specification group option.
          placeholders.push(this.placeholderForSpecificationGroup())
          placeholders.push(this.placeholderForSpecificationGroupOption())
        }
      }
    }
    placeholders.push(this.placeholderForActor(true))
    placeholders.push({ key: Constants.PLACEHOLDER__ORGANISATION, value: 'The name of the ' + this.dataService.labelOrganisationLower() + ' to be granted the certificate.' })
    placeholders.push({ key: Constants.PLACEHOLDER__SYSTEM, value: 'The name of the ' + this.dataService.labelSystemLower() + ' that was used in the tests.' })
    placeholders.push({ key: Constants.PLACEHOLDER__SNAPSHOT, value: 'The public name of the relevant conformance snapshot.' })
    placeholders.push({ key: Constants.PLACEHOLDER__BADGE+'{index}', value: 'The conformance badge image of the statement at the specific order index (original image size).', select: () => Constants.PLACEHOLDER__BADGE+'{0}'})
    placeholders.push({ key: Constants.PLACEHOLDER__BADGE+'{index|width}', value: 'The conformance badge image of the statement at the specific order index (with fixed width in pixels).', select: () => Constants.PLACEHOLDER__BADGE+'{0|100}'})
    placeholders.push({ key: Constants.PLACEHOLDER__BADGES+'{layout}', value: 'The list of all conformance badges using a horizontal (the default) or vertical layout.', select: () => Constants.PLACEHOLDER__BADGES+'{horizontal}'})
    placeholders.push({ key: Constants.PLACEHOLDER__BADGES+'{layout|width}', value: 'The list of all conformance badges (with fixed width in pixels) using a \'horizontal\' (the default) or \'vertical\' layout.', select: () => Constants.PLACEHOLDER__BADGES+'{horizontal|100}'})
    placeholders.push({ key: Constants.PLACEHOLDER__REPORT_DATE+'{format}', value: 'The report generation date (with date format).', select: () => Constants.PLACEHOLDER__REPORT_DATE+'{dd/MM/yyyy}' })
    placeholders.push({ key: Constants.PLACEHOLDER__LAST_UPDATE_DATE+'{format}', value: 'The conformance last update time (with date format).', select: () => Constants.PLACEHOLDER__LAST_UPDATE_DATE+'{dd/MM/yyyy}' })
    return placeholders
  }

  loadAdditionalData(): Observable<any> {
    return this.conformanceService.getCommunityDomains(this.communityId)
    .pipe(
      map((data) => {
        this.communityDomainId = data.linkedDomain
        this.allDomains = data.domains
        if (this.settings?.includeMessage) {
          this.cacheMessages()
          this.includeMessageChanged()
          if (this.settings.enableDomainLevel) {
            this.settings.enableDomainLevel = this.communityDomainId == undefined
          }
          if (!this.settings.enableAllLevel) {
            if (this.settings.enableDomainLevel) this.messageLevel = "domain"
            if (this.settings.enableGroupLevel) this.messageLevel = "group"
            if (this.settings.enableSpecificationLevel) this.messageLevel = "specification"
          }
        }
        this.messageBlockAnimated = false
        this.reportEnabledOptionChanged()
      })
      , share()
    )
  }

  private cacheMessages() {
    if (this.settings?.messages) {
      for (let messageDefinition of this.settings.messages) {
        this.applyMessage(messageDefinition, messageDefinition.message)
      }
    }
  }

  levelBlockExpanded() {
    this.messageBlockAnimated = true
    this.focusFirstTextField()
  }

  reportEnabledOptionChanged() {
    setTimeout(() => {
      if (this.settings) {
        if ((!this.settings.enableAllLevel && this.messageLevel == "all") ||
              (!this.settings.enableDomainLevel && this.messageLevel == "domain") ||
              (!this.settings.enableGroupLevel && this.messageLevel == "group") ||
              (!this.settings.enableSpecificationLevel && this.messageLevel == "specification")) {
          if (this.settings.enableAllLevel) this.messageLevel = "all"
          if (this.settings.enableDomainLevel) this.messageLevel = "domain"
          if (this.settings.enableGroupLevel) this.messageLevel = "group"
          if (this.settings.enableSpecificationLevel) this.messageLevel = "specification"
          this.messageLevelChanged()
        }
      }
      const newValue = this.settings != undefined && (this.settings.enableAllLevel! || this.settings.enableDomainLevel! || this.settings.enableGroupLevel! || this.settings.enableSpecificationLevel!)
      this.messageBlockAnimated = newValue == this.anyLevelEnabled
      this.anyLevelEnabled = newValue
      this.levelAnimated = true
    }, 1)
  }

  includeMessageChanged() {
    if (this.allDomains && this.allGroups == undefined) {
      // Load also groups and specs.
      const domainIds = this.allDomains.map((domain) => domain.id)
      const groupsObservable = this.specificationService.getSpecificationGroupsOfDomains(domainIds)
      const specsObservable = this.conformanceService.getSpecificationsWithIds(undefined, domainIds, undefined)
      forkJoin([groupsObservable, specsObservable]).subscribe((results) => {
        this.allGroups = results[0]
        for (let group of this.allGroups) {
          if (!this.groupsPerDomain.has(group.domain)) {
            this.groupsPerDomain.set(group.domain, [])
          }
          this.groupsPerDomain.get(group.domain)!.push(group)
        }
        this.allSpecifications = results[1]
        for (let spec of this.allSpecifications) {
          if (spec.group != undefined) {
            if (!this.specsPerGroup.has(spec.group)) {
              this.specsPerGroup.set(spec.group, [])
            }
            this.specsPerGroup.get(spec.group)!.push(spec)
          } else {
            if (!this.specsPerDomain.has(spec.domain)) {
              this.specsPerDomain.set(spec.domain, [])
            }
            this.specsPerDomain.get(spec.domain)!.push(spec)
          }
        }
        this.domains = this.allDomains
        if (this.domains && this.domains.length > 0) {
          this.domainId = this.domains[0].id
          this.domainChanged()
        }
      })
    }
  }

  messageLevelChanged() {
    setTimeout(() => {
      if (this.messageLevel == "domain") {
        this.domains = this.allDomains
        this.groupId = undefined
        this.specificationId = undefined
      } else if (this.messageLevel == "group") {
        this.domains = filter(this.allDomains, (domain) => this.groupsPerDomain.has(domain.id))
        this.specificationId = undefined
      } else if (this.messageLevel == "specification") {
        this.domains = filter(this.allDomains, (domain) => this.groupsPerDomain.has(domain.id) || this.specsPerDomain.has(domain.id))
      } else {
        this.domains = []
        this.domainId = undefined
        this.groupId = undefined
        this.specificationId = undefined
      }
      this.specificMessageSetting = false
      this.toggleSpecificMessageSetting()
    }, 1)
  }

  toggleSpecificMessageSetting() {
    if (this.specificMessageSetting && this.domains && this.domains.length > 0) {
      if (this.domainId != undefined) {
        const found = find(this.domains, (domain) => domain.id == this.domainId)
        if (!found) {
          this.domainId = undefined
        }
      }
      if (this.domainId == undefined) {
        this.domainId = this.domains[0].id
      }
    } else {
      this.domainId = undefined
    }
    this.domainChanged()
  }

  domainChanged() {
    if (this.domainId != undefined) {
      if (this.groupsPerDomain.has(this.domainId)) {
        // The selected domain has groups.
        if (this.messageLevel == "group") {
          this.groups = this.groupsPerDomain.get(this.domainId)
          if (this.groupId != undefined) {
            const found = find(this.groups, (group) => group.id == this.groupId)
            if (!found) {
              this.groupId = undefined
            }
          }
          if (this.groupId == undefined && this.groups!.length > 0) {
            this.groupId = this.groups![0].id
          }
        } else if (this.messageLevel == "specification") {
          this.groups = filter(this.groupsPerDomain.get(this.domainId), (group) => this.specsPerGroup.has(group.id))
          if (this.groupId != undefined) {
            const found = find(this.groups, (group) => group.id == this.groupId)
            if (!found) {
              this.groupId = undefined
            }
          }
          if (this.groupId == undefined) {
            if (this.specsPerDomain.has(this.domainId)) {
              this.groupId = this.noGroup
            } else if (this.groups.length > 0) {
                this.groupId = this.groups[0].id
            }
          }
        }
      } else if (this.specsPerDomain.has(this.domainId)) {
        // The selected domain has specifications.
        this.groups = []
        this.groupId = this.noGroup
      }
    }
    if (this.communityDomainId != undefined) {
      this.domainChangedEmitter.emit(this.communityDomainId)
    } else {
      if (this.messageLevel == "all" || this.messageLevel == "domain" && !this.specificMessageSetting) {
        this.domainChangedEmitter.emit(undefined)
      } else {
        this.domainChangedEmitter.emit(this.domainId)
      }
    }
    this.groupChanged(true)
    this.prepareMessage()
  }

  groupChanged(skipMessageUpdate?: boolean) {
    if (this.groupId != undefined && this.messageLevel == "specification") {
      if (this.groupId == this.noGroup) {
        if (this.domainId != undefined) {
          this.specifications = this.specsPerDomain.get(this.domainId)
          this.specificationSelectLabel = this.dataService.labelSpecification()+":"
        }
      } else {
        if (this.specsPerGroup.has(this.groupId)) {
          this.specifications = this.specsPerGroup.get(this.groupId)
          this.specificationSelectLabel = this.dataService.labelSpecificationInGroup()+":"
        }
      }
      if (this.specifications && this.specifications.length > 0) {
        this.specificationId = this.specifications[0].id
        this.specificationChanged(true)
      }
    }
    if (skipMessageUpdate != true) {
      this.prepareMessage()
    }
  }

  specificationChanged(skipMessageUpdate?: boolean) {
    if (skipMessageUpdate != true) {
      this.prepareMessage()
    }
  }

  clearCurrentMessage() {
    this.currentMessageContent = undefined
    this.applyMessage(this.currentMessageKey, this.currentMessageContent)
  }

  applyMessage(messageInfo: ConformanceOverviewMessage|undefined, messageContent: string|undefined) {
    if (messageInfo) {
      if (messageInfo.level == "all") {
        if (this.aggregateMessage) {
          this.aggregateMessage.message = messageContent
        } else {
          this.aggregateMessage = { id: messageInfo.id, level: "all", message: messageContent }
        }
      } else if (messageInfo.level == "domain" && messageInfo.identifier == undefined) {
        if (this.defaultDomainMessage) {
          this.defaultDomainMessage.message = messageContent
        } else {
          this.defaultDomainMessage = { id: messageInfo.id, level: "domain", message: messageContent }
        }
      } else if (messageInfo.level == "domain" && messageInfo.identifier != undefined) {
        if (this.specificDomainMessages.has(messageInfo.identifier)) {
          this.specificDomainMessages.get(messageInfo.identifier)!.message = messageContent
        } else {
          this.specificDomainMessages.set(messageInfo.identifier, { id: messageInfo.id, level: "domain", identifier: messageInfo.identifier, message: messageContent })
        }
      } else if (messageInfo.level == "group" && messageInfo.identifier == undefined) {
        if (this.defaultGroupMessage) {
          this.defaultGroupMessage.message = messageContent
        } else {
          this.defaultGroupMessage = { id: messageInfo.id, level: "group", message: messageContent }
        }
      } else if (messageInfo.level == "group" && messageInfo.identifier != undefined) {
        if (this.specificGroupMessages.has(messageInfo.identifier)) {
          this.specificGroupMessages.get(messageInfo.identifier)!.message = messageContent
        } else {
          this.specificGroupMessages.set(messageInfo.identifier, { id: messageInfo.id, level: "group", identifier: messageInfo.identifier, message: messageContent })
        }
      } else if (messageInfo.level == "specification" && messageInfo.identifier == undefined) {
        if (this.defaultSpecificationMessage) {
          this.defaultSpecificationMessage.message = messageContent
        } else {
          this.defaultSpecificationMessage = { id: messageInfo.id, level: "specification", message: messageContent }
        }
      } else if (messageInfo.level == "specification" && messageInfo.identifier != undefined) {
        if (this.specificSpecificationMessages.has(messageInfo.identifier)) {
          this.specificSpecificationMessages.get(messageInfo.identifier)!.message = messageContent
        } else {
          this.specificSpecificationMessages.set(messageInfo.identifier, { id: messageInfo.id, level: "specification", identifier: messageInfo.identifier, message: messageContent })
        }
      }
    }
  }

  prepareMessage() {
    this.placeholders = this.getPlaceholders()
    this.applyMessage(this.currentMessageKey, this.currentMessageContent)
    if (this.messageLevel == "all") {
      this.currentMessageContent = this.aggregateMessage?.message
      this.currentMessageKey = { level: "all" }
    } else if (this.messageLevel == "domain" && !this.specificMessageSetting) {
      this.currentMessageContent = this.defaultDomainMessage?.message
      this.currentMessageKey = { level: "domain" }
    } else if (this.messageLevel == "domain" && this.domainId != undefined) {
      this.currentMessageContent = this.specificDomainMessages.get(this.domainId)?.message
      this.currentMessageKey = { level: "domain", identifier: this.domainId }
    } else if (this.messageLevel == "group" && !this.specificMessageSetting) {
      this.currentMessageContent = this.defaultGroupMessage?.message
      this.currentMessageKey = { level: "group" }
    } else if (this.messageLevel == "group" && this.groupId != undefined) {
      this.currentMessageContent = this.specificGroupMessages.get(this.groupId)?.message
      this.currentMessageKey = { level: "group", identifier: this.groupId }
    } else if (this.messageLevel == "specification" && !this.specificMessageSetting) {
      this.currentMessageContent = this.defaultSpecificationMessage?.message
      this.currentMessageKey = { level: "specification" }
    } else if (this.messageLevel == "specification" && this.specificationId != undefined) {
      this.currentMessageContent = this.specificSpecificationMessages.get(this.specificationId)?.message
      this.currentMessageKey = { level: "specification", identifier: this.specificationId }
    } else {
      this.currentMessageKey = undefined
      this.currentMessageContent = undefined
    }
  }

  getReportType(): number {
    return Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_CERTIFICATE
  }

}
