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

import {AfterViewInit, Component, OnInit} from '@angular/core';
import { mergeMap, Observable, of } from 'rxjs';
import { AccountService } from 'src/app/services/account.service';
import { DataService } from 'src/app/services/data.service';
import {BsModalService} from 'ngx-bootstrap/modal';
import {ServiceHealthModalComponent} from '../../modals/service-health-modal/service-health-modal.component';
import {StartupWizardModalComponent} from '../../modals/startup-wizard-modal/startup-wizard-modal.component';

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    standalone: false
})
export class HomeComponent implements OnInit, AfterViewInit {

  pageContent?: string

  constructor(
    private readonly accountService: AccountService,
    public readonly dataService: DataService,
    private readonly modalService: BsModalService
  ) { }

  ngOnInit(): void {
    let page$: Observable<string>
    if (this.dataService.currentLandingPageContent == undefined) {
      // Reload.
      page$ = this.accountService.getVendorProfile().pipe(
        mergeMap((vendor) => {
          if (vendor.landingPages) {
            this.dataService.currentLandingPageContent = this.orEmptyString(vendor.landingPages.content)
          } else {
            this.dataService.currentLandingPageContent = ''
          }
          return of(this.dataService.currentLandingPageContent)
        })
      )
    } else {
      page$ = of(this.dataService.currentLandingPageContent)
    }
    page$.subscribe((data) => {
      this.pageContent = data
    })
    this.dataService.breadcrumbUpdate({breadcrumbs: []})
  }

  ngAfterViewInit() {
    if (this.dataService.isSystemAdmin && this.dataService.configuration.startupWizardEnabled) {
      const modal = this.modalService.show(StartupWizardModalComponent, {
        class: 'modal-lg',
        keyboard: false,
        backdrop: 'static'
      })
    }
  }

  private orEmptyString(content: string|undefined) {
    if (content != undefined) {
      return content
    } else {
      return ''
    }
  }

}
