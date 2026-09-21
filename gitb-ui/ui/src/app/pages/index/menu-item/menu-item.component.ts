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

import {Component, ContentChild, Input, OnDestroy, OnInit, TemplateRef} from '@angular/core';
import {Subscription} from 'rxjs';
import {DataService} from 'src/app/services/data.service';
import {MenuItem} from 'src/app/types/menu-item.enum';
import {MenuItemStatus} from '../../../types/menu-item-status.enum';
import {NavigationTarget} from 'src/app/types/navigation-target';
import {Utils} from 'src/app/common/utils';

@Component({
    selector: 'app-menu-item',
    templateUrl: './menu-item.component.html',
    styleUrls: ['./menu-item.component.less'],
    standalone: false
})
export class MenuItemComponent implements OnInit, OnDestroy {

  @Input() label!: string
  @Input() icon?: string
  @Input() expanded = false
  @Input() type!: MenuItem
  /**
   * Set for entries that navigate (the majority) so the component renders an anchor. Left
   * undefined for entries that trigger an in-page action instead (e.g. "Link to current page",
   * "Collapse menu"), which render as a plain clickable div as before.
   */
  @Input() target?: NavigationTarget
  @ContentChild(TemplateRef) customTemplate?: TemplateRef<any>;

  active = false
  pageChangeSubscription?: Subscription
  statusSubscription?: Subscription
  status = MenuItemStatus.None
  protected readonly MenuItemStatus = MenuItemStatus;

  constructor(private readonly dataService: DataService) { }

  ngOnInit(): void {
    if (this.dataService.latestPageChange && this.dataService.latestPageChange.menuItem == this.type) {
      this.active = true
      this.dataService.changeBanner(this.label)
    }
    this.pageChangeSubscription = this.dataService.onPageChange$.subscribe((event) => {
      if (event.menuItem != undefined) {
        setTimeout(() => {
          this.active = event.menuItem  == this.type
          if (this.active) {
            this.dataService.changeBanner(this.label)
          }
        }, 1)
      }
    })
    this.statusSubscription = this.dataService.onMenuItemStatusChange$.subscribe((event) => {
      if (event.menuItem === this.type) {
        setTimeout(() => {
          this.status = event.status
        })
      }
    })
  }

  ngOnDestroy(): void {
    if (this.pageChangeSubscription) this.pageChangeSubscription.unsubscribe()
    if (this.statusSubscription) this.statusSubscription.unsubscribe()
  }

  /**
   * @param event Passed only when the item is rendered as a link (see `target`). A modified click
   * (ctrl/cmd/shift/alt, or a non-primary button) opens the destination in a new tab/window rather
   * than navigating away from the current one, so display state must not be cleared in that case.
   */
  itemClicked(event?: MouseEvent) {
    if (event == undefined || Utils.isPlainNavigationClick(event)) {
      this.dataService.clearAllDisplayStates()
    }
  }

}
