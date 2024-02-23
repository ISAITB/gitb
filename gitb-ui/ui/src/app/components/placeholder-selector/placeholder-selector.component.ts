import { Component, Input, OnInit } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { CommunityService } from 'src/app/services/community.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { CommunityResource } from 'src/app/types/community-resource';
import { DomainParameter } from 'src/app/types/domain-parameter';
import { KeyValue } from 'src/app/types/key-value';
import { FilterUpdate } from '../test-filter/filter-update';
import { MultiSelectConfig } from '../multi-select-filter/multi-select-config';
import { PlaceholderInfo } from './placeholder-info';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { SystemParameter } from 'src/app/types/system-parameter';

@Component({
  selector: 'app-placeholder-selector',
  templateUrl: './placeholder-selector.component.html',
  styleUrls: [ './placeholder-selector.component.less' ]
})
export class PlaceholderSelectorComponent implements OnInit {

  @Input() placeholders: PlaceholderInfo[] = []
  @Input() domainParameters: boolean = false
  @Input() organisationParameters: boolean = false
  @Input() systemParameters: boolean = false
  @Input() resources: boolean = false
  @Input() community?: number

  domainParameterPlaceholders?: KeyValue[]
  organisationParameterPlaceholders?: KeyValue[]
  systemParameterPlaceholders?: KeyValue[]
  communityResources?: CommunityResource[]
  communityResourceConfig?: MultiSelectConfig<CommunityResource>

  constructor(
    private dataService: DataService,
    private popupService: PopupService,
    private conformanceService: ConformanceService,
    private communityService: CommunityService
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
    let domainParameterObservable: Observable<DomainParameter[]>
    if (this.domainParameters) {
      if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId != undefined) {
        domainParameterObservable = this.conformanceService.getDomainParameters(this.dataService.community!.domainId, false, true)
      } else if (this.dataService.isSystemAdmin && this.community != undefined) {
        domainParameterObservable = this.conformanceService.getDomainParametersOfCommunity(this.community, false, true)
      } else {
        domainParameterObservable = of([])
      }
    } else {
      domainParameterObservable = of([])
    }
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
      if (data[0].length > 0) {
        this.domainParameterPlaceholders = []
        for (let parameter of data[0]) {
          let description = parameter.description
          if (description == undefined) description = ''
          this.domainParameterPlaceholders.push({
            key: '$DOMAIN{'+parameter.name+'}',
            value: description
          })
        }
      }
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
          loader: (() => {
            return this.communityService.getCommunityResources(communityIdToUse!)
          }).bind(this)
        }
      }
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

}
