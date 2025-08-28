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

import {Component, EventEmitter, Input, OnInit, Output, QueryList, ViewChildren} from '@angular/core';
import {Observable} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {ConformanceStatus} from 'src/app/types/conformance-status';
import {ExportReportEvent} from 'src/app/types/export-report-event';
import {ConformanceStatementItemsDisplayComponentApi} from './conformance-statement-items-display-component-api';
import {
  ConformanceStatementItemDisplayComponentApi
} from '../conformance-statement-item-display/conformance-statement-item-display-component-api';

@Component({
    selector: 'app-conformance-statement-items-display',
    templateUrl: './conformance-statement-items-display.component.html',
    styles: [],
    standalone: false
})
export class ConformanceStatementItemsDisplayComponent implements OnInit, ConformanceStatementItemsDisplayComponentApi {

  @Input() items: ConformanceStatementItem[] = []
  @Input() shade = false
  @Input() animated = false
  @Input() expandable = true
  @Input() wrapDescriptions = false
  @Input() withCheck = true
  @Input() withExport = false
  @Input() withResults = false
  @Input() filtering = true
  @Input() withTestCases = false

  // Inputs for when we display test cases
  @Input() testSuiteLoader?: (item: ConformanceStatementItem) => Observable<ConformanceStatus|undefined>
  @Input() communityId?: number
  @Input() organisationId?: number
  @Input() snapshotId?: number
  @Input() snapshotLabel?: string

  @Output() selectionChanged = new EventEmitter<ConformanceStatementItem>()
  @Output() export = new EventEmitter<ExportReportEvent>()
  @Output() viewTestSession = new EventEmitter<string>()

  @ViewChildren('itemComponent') itemComponents?: QueryList<ConformanceStatementItemDisplayComponentApi>

  hidden = false

  constructor() { }

  ngOnInit(): void {
    // If we have only one domain then we don't show it.
    this.hidden = (this.items.length == 1 && this.items[0].itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN)
  }

  childSelectionChanged(childItem: ConformanceStatementItem) {
    this.selectionChanged.emit(childItem)
  }

  childExported(event: ExportReportEvent) {
    this.export.emit(event)
  }

  onViewTestSession(session: string) {
    this.viewTestSession.emit(session)
  }

  reset() {
    this.ngOnInit()
    if (this.itemComponents) {
      this.itemComponents.forEach(item => item.reset())
    }
  }

}
