/*
 * Copyright (C) 2026 European Union
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

import {ChangeDetectionStrategy, Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {forkJoin, of} from 'rxjs';
import {Constants} from '../../common/constants';
import {FilterUpdate} from '../../components/test-filter/filter-update';
import {MultiSelectConfig} from '../../components/multi-select-filter/multi-select-config';
import {ConfirmationDialogService} from '../../services/confirmation-dialog.service';
import {DataService} from '../../services/data.service';
import {OrganisationService} from '../../services/organisation.service';
import {PopupService} from '../../services/popup.service';
import {SystemService} from '../../services/system.service';
import {MergeRequest} from '../../types/merge-request';
import {Organisation} from '../../types/organisation.type';
import {System} from '../../types/system';
import {MergeSystemRow} from './merge-system-row';
import {MergeResult} from './merge-result';

@Component({
  selector: 'app-merge-organisation-modal',
  standalone: false,
  templateUrl: './merge-organisation-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './merge-organisation-modal.component.less'
})
export class MergeOrganisationModalComponent implements OnInit {

  @Input() organisation!: Organisation
  @Input() communityId!: number
  @ViewChild('focusAnchor') focusAnchor?: ElementRef<HTMLElement>

  protected readonly Constants = Constants
  loading = true
  pending = false
  step = 0
  /** Set once the title changes, so it is not animated when the modal first opens. */
  titleAnimated = false
  operation?: string
  otherOrganisations: Organisation[] = []
  sourceSystems: System[] = []
  targetOrganisation?: Organisation
  targetSystems: System[] = []
  targetSystemsPending = false
  rows: MergeSystemRow[] = []
  targetOrganisationConfig!: MultiSelectConfig<Organisation>

  constructor(
    private readonly modalInstance: NgbActiveModal,
    public readonly dataService: DataService,
    private readonly organisationService: OrganisationService,
    private readonly systemService: SystemService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly popupService: PopupService
  ) {
  }

  ngOnInit(): void {
    this.targetOrganisationConfig = {
      name: 'targetOrganisation',
      textField: 'fname',
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: 'Select target ' + this.dataService.labelOrganisationLower() + '...',
      loader: () => of(this.otherOrganisations)
    }
    forkJoin([
      this.organisationService.getOrganisationsByCommunity(this.communityId),
      this.systemService.getSystemsByOrganisation(this.organisation.id, undefined, true)
    ]).subscribe((data) => {
      // Administration and template organisations can neither be the source nor the target.
      this.otherOrganisations = data[0].filter(x => x.id != this.organisation.id && !x.adminOrganization && !x.template)
      this.sourceSystems = data[1]
    }).add(() => {
      this.loading = false
      this.preselectOperation()
    })
  }

  /** If only one operation is possible there is nothing to choose, so it is preselected. */
  private preselectOperation() {
    const available = [
      this.mergeOrganisationsAvailable() ? Constants.MERGE_OPERATION.MERGE_ORGANISATIONS : undefined,
      this.mergeSystemsAvailable() ? Constants.MERGE_OPERATION.MERGE_SYSTEMS : undefined,
      this.moveSystemsAvailable() ? Constants.MERGE_OPERATION.MOVE_SYSTEMS : undefined
    ].filter(x => x != undefined)
    if (available.length == 1) {
      this.operation = available[0]
    }
  }

  mergeOrganisationsAvailable() {
    return this.otherOrganisations.length > 0
  }

  mergeSystemsAvailable() {
    return this.sourceSystems.length > 1
  }

  moveSystemsAvailable() {
    return this.otherOrganisations.length > 0 && this.sourceSystems.length > 0
  }

  next() {
    this.step = 1
    this.titleAnimated = true
    this.targetOrganisation = undefined
    this.targetSystems = []
    this.rows = this.createRows()
    this.restoreFocus()
  }

  previous() {
    this.step = 0
    this.titleAnimated = true
    this.restoreFocus()
  }

  /**
   * The Next/Previous button that had focus is removed when the step changes, which moves focus to the document body.
   * The modal only receives key events (such as Escape) from within itself, so focus is moved back to the modal.
   */
  private restoreFocus() {
    setTimeout(() => this.focusAnchor?.nativeElement.focus())
  }

  /** The main label of an operation (also used as the title once it is selected). */
  operationLabel(operation: string) {
    switch (operation) {
      case Constants.MERGE_OPERATION.MERGE_ORGANISATIONS: return 'Merge ' + this.dataService.labelOrganisationsLower()
      case Constants.MERGE_OPERATION.MERGE_SYSTEMS: return 'Merge ' + this.dataService.labelSystemsLower()
      default: return 'Move ' + this.dataService.labelSystemsLower()
    }
  }

  operationIncomplete() {
    return this.operation == undefined
  }

  needsTargetOrganisation() {
    return this.operation != Constants.MERGE_OPERATION.MERGE_SYSTEMS
  }

  private defaultAction() {
    switch (this.operation) {
      case Constants.MERGE_OPERATION.MERGE_ORGANISATIONS: return Constants.MERGE_SYSTEM_ACTION.MOVE
      default: return Constants.MERGE_SYSTEM_ACTION.KEEP
    }
  }

  private createRows(): MergeSystemRow[] {
    return this.sourceSystems.map((system) => {
      const row: MergeSystemRow = {
        system: system,
        action: this.defaultAction(),
        targetConfig: {
          name: 'targetSystem' + system.id,
          textField: 'sname',
          singleSelection: true,
          singleSelectionPersistent: true,
          singleSelectionClearable: true,
          showAsFormControl: true,
          filterLabel: 'Select target ' + this.dataService.labelSystemLower() + '...',
          noItemsMessage: 'No ' + this.dataService.labelSystemsLower() + ' available.',
          loader: () => of(this.targetCandidates(row))
        }
      }
      return row
    })
  }

  targetOrganisationSelected(event: FilterUpdate<Organisation>) {
    this.targetOrganisation = event.values.active.length > 0 ? event.values.active[0] : undefined
    this.targetSystems = []
    // Merge targets depend on the selected organisation.
    this.rows = this.createRows()
    if (this.targetOrganisation) {
      this.targetSystemsPending = true
      this.systemService.getSystemsByOrganisation(this.targetOrganisation.id).subscribe((data) => {
        this.targetSystems = data
      }).add(() => {
        this.targetSystemsPending = false
      })
    }
  }

  /** Whether the system rows can be shown (a target organisation is needed only when moving between organisations). */
  showSystems() {
    return (!this.needsTargetOrganisation() || this.targetOrganisation != undefined) && this.rows.length > 0
  }

  actionOptions(): { value: string, label: string, icon: string }[] {
    const mergeOption = { value: Constants.MERGE_SYSTEM_ACTION.MERGE, label: 'Merge into...', icon: Constants.BUTTON_ICON.MERGE }
    switch (this.operation) {
      case Constants.MERGE_OPERATION.MERGE_ORGANISATIONS:
        return [
          { value: Constants.MERGE_SYSTEM_ACTION.MOVE, label: 'Move', icon: Constants.BUTTON_ICON.MOVE },
          mergeOption,
          { value: Constants.MERGE_SYSTEM_ACTION.DELETE, label: 'Delete', icon: Constants.BUTTON_ICON.DELETE }
        ]
      case Constants.MERGE_OPERATION.MOVE_SYSTEMS:
        return [
          { value: Constants.MERGE_SYSTEM_ACTION.KEEP, label: 'Keep (no change)', icon: Constants.BUTTON_ICON.SKIP },
          { value: Constants.MERGE_SYSTEM_ACTION.MOVE, label: 'Move', icon: Constants.BUTTON_ICON.MOVE },
          mergeOption
        ]
      default:
        return [
          { value: Constants.MERGE_SYSTEM_ACTION.KEEP, label: 'Keep (no change)', icon: Constants.BUTTON_ICON.SKIP },
          mergeOption
        ]
    }
  }

  /** The systems a row can be merged into. */
  private targetCandidates(row: MergeSystemRow): System[] {
    if (this.operation == Constants.MERGE_OPERATION.MERGE_SYSTEMS) {
      // Systems that are themselves merged into another system cannot be targets.
      return this.rows
        .filter(x => x !== row && x.action != Constants.MERGE_SYSTEM_ACTION.MERGE)
        .map(x => x.system)
    } else {
      return this.targetSystems
    }
  }

  /** Whether the merge option is available for the row. */
  mergeDisabled(row: MergeSystemRow) {
    if (row.action == Constants.MERGE_SYSTEM_ACTION.MERGE) {
      return false
    }
    if (this.operation == Constants.MERGE_OPERATION.MERGE_SYSTEMS) {
      // A system that is the target of another merge cannot be merged itself.
      const isTarget = this.rows.some(x => x !== row && x.action == Constants.MERGE_SYSTEM_ACTION.MERGE && x.target?.id == row.system.id)
      return isTarget || this.targetCandidates(row).length == 0
    } else {
      return this.targetSystems.length == 0
    }
  }

  actionChanged(row: MergeSystemRow) {
    if (row.action != Constants.MERGE_SYSTEM_ACTION.MERGE) {
      row.target = undefined
    }
  }

  targetSystemSelected(row: MergeSystemRow, event: FilterUpdate<System>) {
    row.target = event.values.active.length > 0 ? event.values.active[0] : undefined
  }

  actionDisabled() {
    if (this.pending || this.loading || this.operation == undefined) {
      return true
    }
    if (this.needsTargetOrganisation() && (this.targetOrganisation == undefined || this.targetSystemsPending)) {
      return true
    }
    if (this.rows.some(x => x.action == Constants.MERGE_SYSTEM_ACTION.MERGE && x.target == undefined)) {
      return true
    }
    // Unless the organisation is merged (and thus removed) at least one change is needed.
    return this.operation != Constants.MERGE_OPERATION.MERGE_ORGANISATIONS && !this.rows.some(x => x.action != Constants.MERGE_SYSTEM_ACTION.KEEP);
  }

  actionLabel() {
    return this.operation == Constants.MERGE_OPERATION.MOVE_SYSTEMS ? 'Move' : 'Merge'
  }

  actionIcon() {
    return this.operation == Constants.MERGE_OPERATION.MOVE_SYSTEMS ? Constants.BUTTON_ICON.MOVE : Constants.BUTTON_ICON.MERGE
  }

  private confirmationText() {
    switch (this.operation) {
      case Constants.MERGE_OPERATION.MERGE_ORGANISATIONS: {
        return 'Merging ' + this.dataService.labelOrganisationsLower() + ' adapts conformance statements, test sessions and configuration, and is not reversible. Are you sure you want to proceed?'
      }
      case Constants.MERGE_OPERATION.MERGE_SYSTEMS:
        return 'Merging ' + this.dataService.labelSystemsLower() + ' adapts conformance statements, test sessions and configuration, and is not reversible. Are you sure you want to proceed?'
      default:
        return 'Moving ' + this.dataService.labelSystemsLower() + ' to another ' + this.dataService.labelOrganisationLower() + ' adapts conformance statements, test sessions and configuration, and is not reversible. Are you sure you want to proceed?'
    }
  }

  merge() {
    this.confirmationDialogService.confirmedDangerous('Confirm ' + this.actionLabel().toLowerCase(), this.confirmationText(), this.actionLabel(), 'Cancel', this.actionIcon())
      .subscribe(() => {
        const request: MergeRequest = {
          operation: this.operation!,
          target: this.operation == Constants.MERGE_OPERATION.MERGE_SYSTEMS ? undefined : this.targetOrganisation!.id,
          systems: this.rows.map(x => ({ id: x.system.id, action: x.action, target: x.target?.id }))
        }
        this.pending = true
        this.organisationService.mergeOrganisation(this.organisation.id, request).subscribe((result) => {
          this.popupService.success(this.successMessage())
          const mergeResult: MergeResult = { operation: this.operation!, organisationId: result.id }
          this.modalInstance.close(mergeResult)
        }).add(() => {
          this.pending = false
        })
      })
  }

  private successMessage() {
    switch (this.operation) {
      case Constants.MERGE_OPERATION.MERGE_ORGANISATIONS: return this.dataService.labelOrganisation() + ' merged.'
      case Constants.MERGE_OPERATION.MERGE_SYSTEMS: return this.dataService.labelSystems() + ' merged.'
      default: return this.dataService.labelSystems() + ' moved.'
    }
  }

  cancel() {
    this.modalInstance.dismiss()
  }

}
