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

import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {map, Observable, of} from 'rxjs';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {ConformanceService} from '../../services/conformance.service';
import {DataService} from '../../services/data.service';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {PreviewBadgeModalComponent} from '../../modals/preview-badge-modal/preview-badge-modal.component';
import {ConformanceIds} from '../../types/conformance-ids';
import {BsModalService} from 'ngx-bootstrap/modal';
import {RoutingService} from '../../services/routing.service';
import {StatementOptionsButtonApi} from './statement-options-button-api';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';

@Component({
  selector: 'app-statement-options-button',
  standalone: false,
  templateUrl: './statement-options-button.component.html'
})
export class StatementOptionsButtonComponent<T extends ConformanceIds> implements StatementOptionsButtonApi<T> {

  @Input() item!: T
  @Input() organisationId?: number
  @Input() communityId?: number
  @Input() snapshotId?: number
  @Input() pending?: boolean
  @Output() opening = new EventEmitter<T>()
  @Output() export = new EventEmitter<{ item: T, format: 'xml'|'pdf' }>()

  @ViewChild('optionButton') optionButton?: CheckBoxOptionPanelComponentApi

  protected static EXPORT_XML = '0'
  protected static EXPORT_PDF = '1'
  protected static VIEW_COMMUNITY = '2'
  protected static VIEW_ORGANISATION = '3'
  protected static VIEW_SYSTEM = '4'
  protected static VIEW_SPECIFICATION = '5'
  protected static VIEW_ACTOR = '6'
  protected static VIEW_DOMAIN = '7'
  protected static PREVIEW_BADGE = '8'
  protected static COPY_BADGE_URL = '9'

  showBadgeOptions?: boolean

  constructor(
    private readonly conformanceService: ConformanceService,
    private readonly dataService: DataService,
    private readonly modalService: BsModalService,
    private readonly routingService: RoutingService
  ) {}

  optionsOpening() {
    this.opening.emit(this.item)
  }

  close(source?: T): void {
    if ((source == undefined || (this.item.systemId != source.systemId || this.item.actorId != source.actorId)) && this.optionButton) {
      this.optionButton.close()
    }
  }

  loadAvailableOptionsFactory() {
    return this.loadAvailableOptions.bind(this)
  }

  loadAvailableOptions(): Observable<CheckboxOption[][]> {
    let showBadgeOptions$: Observable<boolean>
    if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
      if (this.showBadgeOptions != undefined) {
        showBadgeOptions$ = of(this.showBadgeOptions)
      } else if (this.item.specificationId != undefined && this.item.actorId != undefined) {
        showBadgeOptions$ = this.conformanceService.getConformanceBadgeStatus(this.item.specificationId, this.item.actorId, this.snapshotId)
      } else {
        showBadgeOptions$ = of(false)
      }
    } else {
      showBadgeOptions$ = of(false)
    }
    return showBadgeOptions$.pipe(
      map((showBadgeOptions) => {
        this.showBadgeOptions = showBadgeOptions
        const optionState: CheckboxOption[][] = [
          [
            {key: StatementOptionsButtonComponent.EXPORT_PDF, label: 'Download report', default: true, iconClass: 'fa-solid fa-file-pdf'},
            {key: StatementOptionsButtonComponent.EXPORT_XML, label: 'Download report as XML', default: true, iconClass: 'fa-solid fa-file-lines'}
          ]
        ]
        const viewOptions: CheckboxOption[] = []
        if (this.item.systemId != undefined && this.item.systemId >= 0) {
          viewOptions.push({key: StatementOptionsButtonComponent.VIEW_SYSTEM, label: `View ${this.dataService.labelSystemLower()}`, default: true, iconClass: 'fa-solid fa-cube'})
        }
        if (this.organisationId != undefined && this.organisationId >= 0) {
          viewOptions.push({key: StatementOptionsButtonComponent.VIEW_ORGANISATION, label: `View ${this.dataService.labelOrganisationLower()}`, default: true, iconClass: 'fa-solid fa-building'})
        }
        if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
          if (this.communityId != undefined && this.communityId >= 0) {
            viewOptions.push({key: StatementOptionsButtonComponent.VIEW_COMMUNITY, label: 'View community', default: true, iconClass: 'fa-solid fa-people-group'})
          }
          if (this.item.actorId != undefined && this.item.actorId >= 0) {
            viewOptions.push({key: StatementOptionsButtonComponent.VIEW_ACTOR, label: `View ${this.dataService.labelActorLower()}`, default: true, iconClass: 'fa-solid fa-circle-user'})
          }
          if (this.item.specificationId != undefined && this.item.specificationId >= 0) {
            viewOptions.push({key: StatementOptionsButtonComponent.VIEW_SPECIFICATION, label: `View ${this.dataService.labelSpecificationLower()}`, default: true, iconClass: 'fa-solid fa-list-check'})
          }
          if (this.item.domainId != undefined && this.item.domainId >= 0 && (this.dataService.isSystemAdmin || (this.dataService.isCommunityAdmin && this.dataService.community?.domain != undefined))) {
            viewOptions.push({key: StatementOptionsButtonComponent.VIEW_DOMAIN, label: `View ${this.dataService.labelDomainLower()}`, default: true, iconClass: 'fa-solid fa-sitemap'})
          }
          if (viewOptions.length > 0) {
            optionState.push(viewOptions)
          }
          if (showBadgeOptions) {
            optionState.push([
              {key: StatementOptionsButtonComponent.COPY_BADGE_URL, label: 'Copy badge URL', default: true, iconClass: 'fa-solid fa-award'},
              {key: StatementOptionsButtonComponent.PREVIEW_BADGE, label: 'Preview badge', default: true, iconClass: 'fa-solid fa-award'}
            ])
          }
        }
        return optionState
      })
    )
  }

  handleOption(event: CheckboxOptionState) {
    if (event[StatementOptionsButtonComponent.EXPORT_XML]) {
      this.export.emit({ item: this.item, format: 'xml' })
    } else if (event[StatementOptionsButtonComponent.EXPORT_PDF]) {
      this.export.emit({ item: this.item, format: 'pdf' })
    } else if (event[StatementOptionsButtonComponent.VIEW_COMMUNITY]) {
      this.toCommunity()
    } else if (event[StatementOptionsButtonComponent.VIEW_ORGANISATION]) {
      this.toOrganisation()
    } else if (event[StatementOptionsButtonComponent.VIEW_SYSTEM]) {
      this.toSystem()
    } else if (event[StatementOptionsButtonComponent.VIEW_DOMAIN]) {
      this.toDomain()
    } else if (event[StatementOptionsButtonComponent.VIEW_SPECIFICATION]) {
      this.toSpecification()
    } else if (event[StatementOptionsButtonComponent.VIEW_ACTOR]) {
      this.toActor()
    } else if (event[StatementOptionsButtonComponent.COPY_BADGE_URL]) {
      this.pending = true
      this.conformanceService.copyBadgeURL(this.item.systemId, this.item.actorId, this.snapshotId).subscribe(() => {
        this.pending = false
      })
    } else if (event[StatementOptionsButtonComponent.PREVIEW_BADGE]) {
      this.modalService.show(PreviewBadgeModalComponent, {
        initialState: {
          config: {
            systemId: this.item.systemId,
            actorId: this.item.actorId,
            snapshotId: this.snapshotId
          }
        }
      })
    }
  }

  private toCommunity() {
    this.routingService.toCommunity(this.communityId!)
  }

  private toOrganisation() {
    if (this.organisationId == this.dataService.vendor!.id) {
      // Own organisation
      this.routingService.toOwnOrganisationDetails()
    } else {
      this.routingService.toOrganisationDetails(this.communityId!, this.organisationId!)
    }
  }

  private toSystem() {
    if (this.organisationId == this.dataService.vendor!.id) {
      this.routingService.toOwnSystemDetails(this.item.systemId)
    } else {
      this.routingService.toSystemDetails(this.communityId!, this.organisationId!, this.item.systemId)
    }
  }

  private toActor() {
    this.routingService.toActor(this.item.domainId, this.item.specificationId, this.item.actorId)
  }

  private toSpecification() {
    this.routingService.toSpecification(this.item.domainId, this.item.specificationId)
  }

  private toDomain() {
    this.routingService.toDomain(this.item.domainId)
  }

}
