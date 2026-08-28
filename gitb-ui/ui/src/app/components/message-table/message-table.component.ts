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

import { AfterViewChecked, Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, Output, QueryList, SimpleChanges, ViewChild, ViewChildren } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Constants } from '../../common/constants';
import { MessageRowView } from '../../types/message-row-view';
import { LoadingStatus } from '../../types/loading-status.type';
import { PagingControlsComponent } from '../paging-controls/paging-controls.component';
import { PagingEvent } from '../paging-controls/paging-event';
import { CheckboxOptionPanelComponent } from '../checkbox-option-panel/checkbox-option-panel.component';
import { CheckboxOption } from '../checkbox-option-panel/checkbox-option';
import { CheckboxOptionState } from '../checkbox-option-panel/checkbox-option-state';

/**
 * The "My messages" message listing - a bespoke table (not the generic table-directive) since it needs a
 * combined subject|preview cell that always truncates to a single line, a left-aligned "important"
 * indicator, and Gmail-style hover row actions confined to the date cell. Used for both the received
 * (inbox) and sent (outbox) views via `mode` - see MessageRowView for the shared row shape.
 */
@Component({
  selector: 'app-message-table',
  standalone: false,
  templateUrl: './message-table.component.html',
  styleUrl: './message-table.component.less'
})
export class MessageTableComponent implements OnChanges, AfterViewChecked {

  @Input() data: MessageRowView[] = []
  @Input() mode: 'received'|'sent' = 'received'
  @Input() loadingStatus?: LoadingStatus
  @Input() selectedId?: number
  @Input() sortColumn?: string
  @Input() sortOrder?: 'asc'|'desc'
  @Input() contentRefreshing = false

  @Output() rowSelect = new EventEmitter<MessageRowView>()
  @Output() checkChange = new EventEmitter<void>()
  @Output() markRead = new EventEmitter<{ id: number, read: boolean }>()
  @Output() deleteOne = new EventEmitter<number>()
  @Output() replyOne = new EventEmitter<MessageRowView>()
  @Output() sortChange = new EventEmitter<{ column: string, order: 'asc'|'desc' }>()
  @Output() pageNavigation = new EventEmitter<PagingEvent>()

  @ViewChild('pagingControls') pagingControls?: PagingControlsComponent
  @ViewChild('dateSample') dateSample?: ElementRef<HTMLElement>
  @ViewChildren('rowOptionsPanel') rowOptionsPanels?: QueryList<CheckboxOptionPanelComponent>

  protected readonly Constants = Constants
  // Undefined until measured from the first rendered date cell - the date presentation format is
  // administrator-configurable, so this cannot be a fixed pixel value (see message-table.component.less).
  dateColWidth?: number
  private measuringDateColWidth = false
  private rowOptionsFactories = new Map<number, () => Observable<CheckboxOption[][]>>()

  get peerColumnTitle(): string {
    return this.mode === 'received' ? 'From' : 'To'
  }

  get hasImportantMessage(): boolean {
    return this.data.some(m => m.important)
  }

  get hasRows(): boolean {
    return this.data.length > 0
  }

  get allChecked(): boolean {
    return this.hasRows && this.data.every((r) => r.checked === true)
  }

  get someChecked(): boolean {
    return !this.allChecked && this.data.some((r) => r.checked === true)
  }

  // Derived from the rows via the getters above rather than tracked separately, so it stays correct
  // whether rows were checked individually, a page was reloaded, or the view was toggled. The native
  // checkbox has already flipped itself by the time "change" fires - the [checked]/[indeterminate]
  // bindings simply re-sync from allChecked/someChecked on the same change-detection pass.
  toggleAllChecked() {
    const target = !this.allChecked
    this.data.forEach((r) => r.checked = target)
    this.onCheckChange()
  }

  peerLabel(row: MessageRowView): string {
    if (row.peerCount > 1) {
      return `(${row.peerCount} recipients)`
    }
    return row.peerName
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data']) {
      this.dateColWidth = undefined
      this.rowOptionsFactories.clear()
    }
  }

  ngAfterViewChecked(): void {
    // Deferred to the next tick (the established house pattern for this - see
    // ConformanceStatementsComponent.systemsLoaded()) - assigning dateColWidth synchronously here would
    // change the [style.width.px] binding this same check cycle just verified, tripping Angular's
    // dev-mode ExpressionChangedAfterItHasBeenCheckedError.
    if (this.dateColWidth == undefined && !this.measuringDateColWidth && this.dateSample) {
      const width = this.dateSample.nativeElement.scrollWidth
      if (width > 0) {
        this.measuringDateColWidth = true
        setTimeout(() => {
          this.dateColWidth = width + 4
          this.measuringDateColWidth = false
        })
      }
    }
  }

  // A single instance of this table renders many per-row options panels - this HostListener pair (one
  // per table, not one per row) forwards document events into all of them, matching the approach used
  // by BaseTableComponent for its own per-row popups (see CLAUDE.md's popup-handling convention).
  @HostListener('document:click', ['$event'])
  clickRegistered(event: Event) {
    this.rowOptionsPanels?.forEach((panel) => panel.documentClick(event))
  }

  @HostListener('document:keyup.escape')
  escapeRegistered() {
    this.rowOptionsPanels?.forEach((panel) => panel.documentEscape())
  }

  rowOptionsFactory(row: MessageRowView): () => Observable<CheckboxOption[][]> {
    let factory = this.rowOptionsFactories.get(row.id)
    if (!factory) {
      factory = () => this.loadRowOptions(row)
      this.rowOptionsFactories.set(row.id, factory)
    }
    return factory
  }

  private loadRowOptions(row: MessageRowView): Observable<CheckboxOption[][]> {
    const options: CheckboxOption[] = [
      { key: 'reply', label: 'Reply', default: true, iconClass: Constants.BUTTON_ICON.REPLY }
    ]
    if (this.mode == 'received') {
      if (row.read) {
        options.push({ key: 'unread', label: 'Mark unread', default: true, iconClass: Constants.BUTTON_ICON.MESSAGE_UNREAD })
      } else {
        options.push({ key: 'read', label: 'Mark read', default: true, iconClass: Constants.BUTTON_ICON.MESSAGE_READ })
      }
    }
    options.push({ key: 'delete', label: 'Delete', default: true, iconClass: Constants.BUTTON_ICON.DELETE })
    return of([options])
  }

  onRowOption(row: MessageRowView, event: CheckboxOptionState) {
    if (event['reply']) {
      this.replyOne.emit(row)
    } else if (event['read']) {
      this.markRead.emit({ id: row.id, read: true })
    } else if (event['unread']) {
      this.markRead.emit({ id: row.id, read: false })
    } else if (event['delete']) {
      this.deleteOne.emit(row.id)
    }
  }

  selectRow(row: MessageRowView) {
    this.rowSelect.emit(row)
  }

  headerClicked(column: string) {
    let order: 'asc'|'desc' = 'asc'
    if (this.sortColumn === column && this.sortOrder === 'asc') {
      order = 'desc'
    }
    this.sortColumn = column
    this.sortOrder = order
    this.sortChange.emit({ column: column, order: order })
  }

  onCheckChange() {
    this.checkChange.emit()
  }

}
