import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
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

@Component({
  selector: 'app-placeholder-selector',
  templateUrl: './placeholder-selector.component.html',
  styleUrls: [ './placeholder-selector.component.less' ]
})
export class PlaceholderSelectorComponent implements OnInit {

  @Input() placeholders: PlaceholderInfo[] = []
  @Input() domainParameters: boolean = false
  @Input() resources: boolean = false
  @Input() community?: number

  parameterPlaceholders?: KeyValue[]
  communityResources?: CommunityResource[]
  communityResourceConfig?: MultiSelectConfig<CommunityResource>

  constructor(
    private dataService: DataService,
    private popupService: PopupService,
    private conformanceService: ConformanceService,
    private communityService: CommunityService
  ) { }

  ngOnInit(): void {
    if (this.domainParameters) {
      let domainParameterFnResult: Observable<DomainParameter[]>|undefined
      if (this.dataService.isCommunityAdmin && this.dataService.community!.domainId != undefined) {
        domainParameterFnResult = this.conformanceService.getDomainParameters(this.dataService.community!.domainId, false, true)
      } else if (this.dataService.isSystemAdmin && this.community != undefined) {
        domainParameterFnResult = this.conformanceService.getDomainParametersOfCommunity(this.community, false, true)
      }
      if (domainParameterFnResult) {
        domainParameterFnResult.subscribe((data) => {
          this.parameterPlaceholders = []
          for (let parameter of data) {
            let description = parameter.description
            if (description == undefined) description = ''
            this.parameterPlaceholders.push({
              key: '$DOMAIN{'+parameter.name+'}',
              value: description
            })
          }
        })
      }
    }
    if (this.resources) {
      let communityIdToUse: number|undefined;
      if (this.dataService.isCommunityAdmin) {
        communityIdToUse = this.dataService.community?.id
      } else {
        communityIdToUse = this.community
      }
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
