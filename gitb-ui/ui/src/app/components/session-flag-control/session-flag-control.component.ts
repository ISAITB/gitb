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

import {Component, EventEmitter, HostBinding, Input, OnChanges, Output, ViewChild} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {CheckboxOptionPanelComponent} from '../checkbox-option-panel/checkbox-option-panel.component';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';

/**
 * The "Flag" assignment control shown next to a completed test session's identifier (test execution
 * page, session dashboard row). Wraps app-checkbox-option-panel as a single-selection picker over the
 * community's flags that are settable by the current user, plus a trailing "Clear flag" option once a
 * flag is set. Entirely absent if the user has no settable flags in the relevant community, or if the
 * session's current flag is admin-only and the user isn't an administrator (matching the server-side
 * rule that an admin-only flag can't be replaced or cleared by an organisation user) - the flag's value
 * itself is still visible via the separate tag display in that case.
 */
@Component({
    selector: 'app-session-flag-control',
    templateUrl: './session-flag-control.component.html',
    standalone: false
})
export class SessionFlagControlComponent implements OnChanges, CheckBoxOptionPanelComponentApi {

  @Input() communityId?: number
  @Input() currentFlagId?: number
  @Input() pending = false
  @Output() flagChanged = new EventEmitter<number|undefined>()

  @ViewChild(CheckboxOptionPanelComponent) private panel?: CheckboxOptionPanelComponent

  // Collapses the component's own host element out of the enclosing btn-toolbar's flex layout when not
  // visible - otherwise an empty custom element still counts as a flex item and its gap/margin spacing
  // shows up as a stray gap between the surrounding buttons.
  @HostBinding('style.display') get hostDisplay(): string|null { return this.visible ? null : 'none' }

  options?: CheckboxOption[][]
  visible = false

  protected readonly Constants = Constants

  constructor(private readonly dataService: DataService) { }

  ngOnChanges(): void {
    this.recompute()
  }

  private recompute(): void {
    const flags = this.dataService.getApplicableTestFlags(this.communityId)
    const isAdmin = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin
    const settable = flags.filter(f => isAdmin || !f.adminOnly)
    const currentFlagReadOnly = this.currentFlagId != undefined && !settable.some(f => f.id == this.currentFlagId)
    // An admin-only flag currently set on the session is not offered for replacement/clearing by an
    // organisation user - the control is fully hidden in that case (the flag itself stays visible via
    // the separate tag display), not just shown inert.
    this.visible = settable.length > 0 && !currentFlagReadOnly
    if (!this.visible) {
      this.options = undefined
      return
    }
    const flagOptions: CheckboxOption[] = settable.map(f => ({
      key: String(f.id),
      label: f.name,
      default: f.id == this.currentFlagId,
      iconClass: Constants.BUTTON_ICON.FLAG,
      iconColour: f.colour
    }))
    const groups: CheckboxOption[][] = [flagOptions]
    if (this.currentFlagId != undefined) {
      groups.push([{ key: 'clear', label: 'Clear flag', default: false, iconClass: Constants.BUTTON_ICON.UNFLAGGED }])
    }
    this.options = groups
  }

  optionSelected(state: CheckboxOptionState) {
    const key = Object.keys(state)[0]
    this.flagChanged.emit(key == 'clear' ? undefined : Number(key))
  }

  // Forwarded from the owning page/table's single top-level document listener (see CLAUDE.md's
  // popup-dismissal convention) rather than attaching a listener here, since this control can appear
  // once per row in a session table.
  close(): void { this.panel?.close() }
  refresh(options: CheckboxOption[][]): void { this.panel?.refresh(options) }
  getReferenceItem() { return this.panel?.getReferenceItem() }
  documentEscape(): void { this.panel?.documentEscape() }
  documentClick(event: Event): void { this.panel?.documentClick(event) }

}
