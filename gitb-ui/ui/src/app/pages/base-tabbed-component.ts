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

import {AfterViewInit, Component, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {BaseComponent} from './base-component.component';
import {Constants} from '../common/constants';
import {NgbNav} from '@ng-bootstrap/ng-bootstrap';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseTabbedComponent extends BaseComponent implements AfterViewInit {

    @ViewChild('tabs', { static: false }) tabs?: NgbNav;
    tabIdToShow = 0

    constructor(
      protected readonly router: Router,
      protected readonly route: ActivatedRoute) {
      super()
      const navigation = router.currentNavigation()
      let tabParam: any
      if (navigation) {
        tabParam = navigation.extras.state?.tab
      } else if (route.snapshot.queryParamMap.has(Constants.NAVIGATION_QUERY_PARAM.TAB)) {
        tabParam = route.snapshot.queryParamMap.get(Constants.NAVIGATION_QUERY_PARAM.TAB)
      }
      if (tabParam != undefined) {
        this.tabIdToShow = Number(tabParam)
      }
    }

    abstract loadTab(tabIndex: number): void

    showTab() {
        setTimeout(() => {
            if (this.tabs) {
              const matchedTab = this.tabs.items.filter(tab => tab.id === this.tabIdToShow)
              if (matchedTab.length == 0) {
                this.tabIdToShow = this.defaultTabId() // Default tab.
                this.showTab()
              } else {
                this.loadTab(this.tabIdToShow)
                // Set the tab ID as a URL query parameter. This ensures we don't lose the tab upon refresh.
                this.router.navigate([], {
                  queryParams: {tab: this.tabIdToShow},
                  queryParamsHandling: 'merge',
                  replaceUrl: true
                })
              }
            }
        })
    }

    protected defaultTabId(): number {
      return 0
    }

    ngAfterViewInit(): void {
       this.showTab()
    }

}
