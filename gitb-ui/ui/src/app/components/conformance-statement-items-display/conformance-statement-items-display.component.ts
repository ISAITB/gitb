import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Observable } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { ConformanceStatus } from 'src/app/types/conformance-status';
import { ExportReportEvent } from 'src/app/types/export-report-event';

@Component({
    selector: 'app-conformance-statement-items-display',
    templateUrl: './conformance-statement-items-display.component.html',
    styles: [],
    standalone: false
})
export class ConformanceStatementItemsDisplayComponent implements OnInit {

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
  
  hidden = false

  constructor() { }

  ngOnInit(): void {
    if (this.items.length == 1 && this.items[0].itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.DOMAIN) {
      // If we have only one domain then we don't show it.
      this.hidden = true
    }
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
}
