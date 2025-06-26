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

import { Component, EventEmitter, Input, OnInit } from '@angular/core';
import { Observable, forkJoin, mergeMap, of, share } from 'rxjs';
import { CommunityService } from 'src/app/services/community.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { CommunityResource } from 'src/app/types/community-resource';
import { KeyValue } from 'src/app/types/key-value';
import { FilterUpdate } from '../test-filter/filter-update';
import { MultiSelectConfig } from '../multi-select-filter/multi-select-config';
import { PlaceholderInfo } from './placeholder-info';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { SystemParameter } from 'src/app/types/system-parameter';
import { Constants } from 'src/app/common/constants';
import {CommunityResourceService} from '../../services/community-resource.service';

@Component({
    selector: 'app-placeholder-selector',
    templateUrl: './placeholder-selector.component.html',
    styleUrls: ['./placeholder-selector.component.less'],
    standalone: false
})
export class PlaceholderSelectorComponent implements OnInit {

  @Input() placeholders: PlaceholderInfo[] = []
  @Input() domainParameters: boolean = false
  @Input() domainId?: number
  @Input() organisationParameters: boolean = false
  @Input() systemParameters: boolean = false
  @Input() resources: boolean = false
  @Input() systemResources: boolean = false
  @Input() community?: number
  @Input() domainChanged?: EventEmitter<number>

  domainParameterCache = new Map<number, KeyValue[]>()
  domainParameterPlaceholders?: KeyValue[]
  organisationParameterPlaceholders?: KeyValue[]
  systemParameterPlaceholders?: KeyValue[]
  communityResourceConfig?: MultiSelectConfig<CommunityResource>
  systemResourceConfig?: MultiSelectConfig<CommunityResource>

  constructor(
    private readonly dataService: DataService,
    private readonly popupService: PopupService,
    private readonly conformanceService: ConformanceService,
    private readonly communityService: CommunityService,
    private readonly communityResourceService: CommunityResourceService
  ) { }

  ngOnInit(): void {
    // Determine community ID
    let communityIdToUse: number|undefined;
    if (this.dataService.isCommunityAdmin) {
      communityIdToUse = this.dataService.community?.id
    } else {
      communityIdToUse = this.community
    }
    // Domain parameters
    const domainParameterObservable = this.updateDomainParameters(this.domainId)
    // Organisation properties
    let organisationParameterObservable: Observable<OrganisationParameter[]>
    if (this.organisationParameters && communityIdToUse) {
      organisationParameterObservable = this.communityService.getOrganisationParameters(communityIdToUse, true)
    } else {
      organisationParameterObservable = of([])
    }
    // System properties
    let systemParameterObservable: Observable<SystemParameter[]>
    if (this.systemParameters && communityIdToUse) {
      systemParameterObservable = this.communityService.getSystemParameters(communityIdToUse, true)
    } else {
      systemParameterObservable = of([])
    }
    // Retrieve all results
    forkJoin([domainParameterObservable, organisationParameterObservable, systemParameterObservable]).subscribe((data) => {
      // Domain parameters
      this.domainParameterPlaceholders = data[0]
      // Organisation parameters
      if (data[1].length > 0) {
        this.organisationParameterPlaceholders = []
        for (let parameter of data[1]) {
          let description = parameter.desc
          if (description == undefined) description = ''
          this.organisationParameterPlaceholders.push({
            key: '$ORGANISATION{'+parameter.testKey+'}',
            value: description
          })
        }
      }
      // System parameters
      if (data[2].length > 0) {
        this.systemParameterPlaceholders = []
        for (let parameter of data[2]) {
          let description = parameter.desc
          if (description == undefined) description = ''
          this.systemParameterPlaceholders.push({
            key: '$SYSTEM{'+parameter.testKey+'}',
            value: description
          })
        }
      }
    })
    // Community resources
    if (this.resources) {
      if (communityIdToUse) {
        this.communityResourceConfig = {
          name: 'resources',
          textField: 'name',
          filterLabel: 'Copy resource reference',
          singleSelection: true,
          loader: () => {
            return this.communityResourceService.getCommunityResources(communityIdToUse!)
          }
        }
      }
    }
    // System resources
    if (this.systemResources) {
      this.systemResourceConfig = {
        name: 'systemResources',
        textField: 'name',
        filterLabel: 'Copy system-wide resource reference',
        singleSelection: true,
        loader: () => {
          return this.communityResourceService.getSystemResources()
        }
      }
    }
    // Listen for domain changes
    if (this.domainChanged) {
      this.domainChanged.subscribe((newDomainId) => {
        if (this.domainId != newDomainId) {
          this.domainId = newDomainId
          this.updateDomainParameters(newDomainId).subscribe((data) =>{
            this.domainParameterPlaceholders = data
          })
        }
      })
    }
  }

  updateDomainParameters(domainId: number|undefined): Observable<KeyValue[]> {
    if (this.domainParameters && domainId != undefined) {
      if (this.domainParameterCache.has(domainId)) {
        return of(this.domainParameterCache.get(domainId)!)
      } else {
        return this.conformanceService.getDomainParameters(domainId, false, true)
          .pipe(
            mergeMap((data) => {
              const domainParameterPlaceholders = []
              for (let parameter of data) {
                let description = parameter.description
                if (description == undefined) description = ''
                domainParameterPlaceholders.push({
                  key: Constants.PLACEHOLDER__DOMAIN+'{'+parameter.name+'}',
                  value: description
                })
              }
              this.domainParameterCache.set(domainId, domainParameterPlaceholders)
              return of(domainParameterPlaceholders)
            }), share()
          )
      }
    } else {
      return of([])
    }
  }

  selectedPlaceholder(placeholder: PlaceholderInfo) {
    let valueToCopy: string
    if (placeholder.select) {
      valueToCopy = placeholder.select()
    } else {
      valueToCopy = placeholder.key
    }
    this.copyValue(valueToCopy, 'Placeholder copied to clipboard.')
  }

  selectedParameter(placeholder: KeyValue) {
    this.copyValue(placeholder.key, 'Placeholder copied to clipboard.')
  }

  private copyValue(value: string, message: string) {
    this.dataService.copyToClipboard(value).subscribe(() => {
      this.popupService.success(message)
    })
  }

  resourceSelected(update: FilterUpdate<CommunityResource>) {
    if (update.values.active.length > 0) {
      this.copyValue(update.values.active[0].reference, 'Resource reference copied to clipboard.')
    }
  }

  systemResourceSelected(update: FilterUpdate<CommunityResource>) {
    if (update.values.active.length > 0) {
      this.copyValue(update.values.active[0].reference, 'Resource reference copied to clipboard.')
    }
  }

}
