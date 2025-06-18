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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { filter } from 'lodash';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { ConformanceStatementResult } from 'src/app/types/conformance-statement-result';
import { Counters } from '../test-status-icons/counters';
import { DataService } from 'src/app/services/data.service';
import { Constants } from 'src/app/common/constants';
import { ConformanceStatus } from 'src/app/types/conformance-status';
import { Observable } from 'rxjs';
import { ConformanceTestSuite } from 'src/app/pages/organisation/conformance-statement/conformance-test-suite';
import { ConformanceStatusSummary } from 'src/app/types/conformance-status-summary';
import { ExportReportEvent } from 'src/app/types/export-report-event';

@Component({
    selector: 'app-conformance-statement-item-display',
    templateUrl: './conformance-statement-item-display.component.html',
    styleUrls: ['./conformance-statement-item-display.component.less'],
    standalone: false
})
export class ConformanceStatementItemDisplayComponent implements OnInit {

  @Input() item!: ConformanceStatementItem
  @Input() shade = false
  @Input() hideSelf = false
  @Input() animated = true
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

  hasChildren = false
  allChildrenHidden = false
  showCheck = false
  showResults = false
  showTestCases = false
  results?: ConformanceStatementResult
  counters?: Counters
  status?: string
  updateTime?: string
  testCasesOpen = false
  testSuites: ConformanceTestSuite[]|undefined
  statementSummary?: ConformanceStatusSummary
  hasBadge = false
  pending = false
  Constants = Constants

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    this.hasChildren = this.item.items != undefined && this.item.items.length > 0
    if (this.hasChildren) {
      const hiddenChildren = filter(this.item.items, (item) => {
        return item.hidden == true
      })
      this.allChildrenHidden = this.item.items!.length == hiddenChildren.length
    } else {
      this.allChildrenHidden = true
    }
    this.showCheck = this.withCheck && (!this.hasChildren || this.allChildrenHidden)
    if (this.withResults && this.allChildrenHidden) {
      this.results = this.findResults(this.item)
      if (this.results) {
        this.showResults = true
        this.showTestCases = this.withTestCases
        this.counters = {
          completed: this.results.completed,
          failed: this.results.failed,
          other: this.results.undefined,
          completedOptional: this.results.completedOptional,
          failedOptional: this.results.failedOptional,
          otherOptional: this.results.undefinedOptional,
          completedToConsider: this.results.completedToConsider,
          failedToConsider: this.results.failedToConsider,
          otherToConsider: this.results.undefinedToConsider,
        }
        this.updateTime = this.results.updateTime
        this.status = this.dataService.conformanceStatusForTests(this.results.completedToConsider, this.results.failedToConsider, this.results.undefinedToConsider)
      }
    }
  }

  findResults(item: ConformanceStatementItem):ConformanceStatementResult|undefined  {
    if (item.results) {
      return item.results
    } else if (item.items) {
      for (let child of item.items) {
        if (child.results) {
          return child.results
        }
      }
    }
    return undefined
  }

  clickHeader() {
    if (this.hasChildren && !this.allChildrenHidden) {
      this.item.collapsed = this.expandable && !this.item.collapsed
    } else {
      if (this.showCheck) {
        this.item.checked = !this.item.checked
      }
      if (this.hasChildren) {
        this.itemSelected(this.item.items![0])
      } else {
        this.itemSelected()
      }
    }
  }

  itemSelected(otherItem?: ConformanceStatementItem) {
    if (this.showTestCases) {
      if (this.testCasesOpen) {
        this.testCasesOpen = false
      } else {
        this.loadTestSuites(otherItem?otherItem:this.item)
      }
    }
    this.notifyForSelectionChange(otherItem)
  }

  private loadTestSuites(item: ConformanceStatementItem) {
    if (this.testSuites) {
      this.testCasesOpen = true
    } else {
      this.pending = true
      this.testSuiteLoader!(item).subscribe((data) => {
        if (data) {
          this.dataService.organiseTestSuitesForDisplay(data.testSuites)
          this.statementSummary = data.summary
          this.hasBadge = data.summary.hasBadge
          this.testSuites = data.testSuites
        }
      }).add(() => {
        setTimeout(() => {
          this.testCasesOpen = true
          this.pending = false
        }, 1)
      })
    }
  }

  notifyForSelectionChange(otherItem?: ConformanceStatementItem) {
    if (otherItem) {
      this.selectionChanged.emit(otherItem)
    } else {
      this.selectionChanged.emit(this.item)
    }
  }

  updateChecked() {
    this.notifyForSelectionChange()
  }

  expanded() {
    setTimeout(() => {
      if (this.item.items && this.item.items.length == 1) {
        this.item.items[0].collapsed = false
      }
    })
  }

  collapses() {
    setTimeout(() => {
      if (this.item.items) {
        for (let child of this.item.items) {
          child.collapsed = true
        }
      }
    })
  }

  childSelectionChanged(item: ConformanceStatementItem) {
    this.notifyForSelectionChange(item)
  }

  handleExportClick(statementReport: boolean, item: ConformanceStatementItem, format: 'xml'|'pdf') {
    this.onExport({
      item: item,
      format: format,
      statementReport: statementReport,
    })
  }

  onExport(event: ExportReportEvent) {
    this.export.emit(event)
  }

  onViewTestSession(session: string) {
    this.viewTestSession.emit(session)
  }
}
