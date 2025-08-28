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

import {AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone, OnInit, Output, ViewChild} from '@angular/core';
import {PagingStatus} from './paging-status';
import {PagingEvent} from './paging-event';

@Component({
  selector: 'app-paging-controls',
  standalone: false,
  templateUrl: './paging-controls.component.html',
  styleUrl: './paging-controls.component.less'
})
export class PagingControlsComponent implements OnInit, AfterViewInit {

  @Input() refreshing = false
  @Input() inCard = false
  @Output() navigation = new EventEmitter<PagingEvent>();
  @ViewChild("pagingContainer") pagingContainer?: ElementRef
  @ViewChild("lastButton") lastButton?: ElementRef
  @ViewChild("pageControlsContainer") pageControlsContainer?: ElementRef

  status!: PagingStatus
  summaryMessage?: string
  pageLinks!: Array<number|undefined>
  resizeObserver!: ResizeObserver
  controlsWrapped = false
  wrapWidth?: number
  numberFormat = new Intl.NumberFormat('en-GB')

  constructor(private readonly zone: NgZone) {
  }

  ngOnInit(): void {
    this.status = new PagingStatus();
    this.updateStatus()
  }

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => {
      this.zone.run(() => {
        this.calculateWrapping()
      })
    })
    if (this.pagingContainer) {
      this.resizeObserver.observe(this.pagingContainer.nativeElement)
    }
  }

  getCurrentStatus(): PagingStatus {
    return this.status;
  }

  updateStatus(currentPage?: number, totalCount?: number): PagingStatus {
    if (currentPage != undefined) {
      this.status.currentPage = currentPage;
    }
    if (totalCount != undefined) {
      this.status.totalCount = totalCount;
    }
    if (this.status.currentPage == 1) {
      this.status.nextDisabled = this.status.totalCount <= this.status.pageSize
      this.status.previousDisabled = true
    } else if (this.status.currentPage == Math.ceil(this.status.totalCount / this.status.pageSize)) {
      this.status.nextDisabled = true
      this.status.previousDisabled = false
    } else {
      this.status.nextDisabled = false
      this.status.previousDisabled = false
    }

    const lastPage = this.status.getLastPage()
    const totalPageLinks = 9
    let linksAround = (totalPageLinks - 1) / 2
    let minPage = this.status.currentPage - linksAround - 1
    let maxPage = this.status.currentPage + linksAround + 1
    if (minPage < 1) {
      maxPage += (1 - minPage) - 1
      minPage = 1
    }
    if (maxPage > lastPage) {
      minPage -= (maxPage - lastPage) - 1
      if (minPage < 1) {
        minPage = 1
      }
      maxPage = lastPage
    }
    this.pageLinks = []
    for (let i = minPage; i <=  maxPage; i++) {
      this.pageLinks.push(i)
    }
    if (this.pageLinks[0]! > 1) {
      this.pageLinks[0] = undefined
    }
    if (this.pageLinks[this.pageLinks.length - 1]! < lastPage) {
      this.pageLinks[this.pageLinks.length - 1] = undefined
    }

    this.updateSummaryMessage()
    return this.status;
  }

  private updateSummaryMessage() {
    let minItemsOnPage = ((this.status.currentPage - 1) * this.status.pageSize) + 1
    let maxItemsOnPage = minItemsOnPage + this.status.pageSize - 1
    if (maxItemsOnPage > this.status.totalCount) {
      maxItemsOnPage = this.status.totalCount
    }
    this.summaryMessage = `Showing ${minItemsOnPage} to ${maxItemsOnPage} of ${this.numberFormat.format(this.status.totalCount)}`
  }

  doFirstPage() {
    this.navigation.emit({
      targetPage: 1,
      targetPageSize: this.status.pageSize
    })
  }

  doPrevPage() {
    this.navigation.emit({
      targetPage: this.status.currentPage - 1,
      targetPageSize: this.status.pageSize
    })
  }

  doNextPage() {
    this.navigation.emit({
      targetPage: this.status.currentPage + 1,
      targetPageSize: this.status.pageSize
    })
  }

  doLastPage() {
    this.navigation.emit({
      targetPage: this.status.getLastPage(),
      targetPageSize: this.status.pageSize
    })
  }

  navigateToPage(page?: number) {
    if (page != undefined && page != this.status.currentPage) {
      this.navigation.emit({
        targetPage: page,
        targetPageSize: this.status.pageSize
      })
    }
  }

  protected calculateWrapping() {
    if (this.pagingContainer && this.pageControlsContainer && this.lastButton) {
      if (!this.controlsWrapped) {
        if (this.pageControlsContainer!.nativeElement.getBoundingClientRect().top < this.lastButton!.nativeElement.getBoundingClientRect().top) {
          this.wrapWidth = this.pagingContainer!.nativeElement.getBoundingClientRect().right
          this.controlsWrapped = true
        }
      } else {
        if (this.wrapWidth != undefined && this.pagingContainer!.nativeElement.getBoundingClientRect().right > this.wrapWidth) {
          this.wrapWidth = undefined
          this.controlsWrapped = false
        }
      }
    }
  }

}
