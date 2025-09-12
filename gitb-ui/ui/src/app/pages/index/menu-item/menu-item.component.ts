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

import {Component, ContentChild, Input, OnDestroy, OnInit, TemplateRef} from '@angular/core';
import {Subscription} from 'rxjs';
import {DataService} from 'src/app/services/data.service';
import {MenuItem} from 'src/app/types/menu-item.enum';

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
  @ContentChild(TemplateRef) customTemplate?: TemplateRef<any>;

  active = false
  pageChangeSubscription?: Subscription

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
  }

  ngOnDestroy(): void {
    if (this.pageChangeSubscription) this.pageChangeSubscription.unsubscribe()
  }

}
