import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { DomainParameter } from 'src/app/types/domain-parameter';
import { KeyValue } from 'src/app/types/key-value';

@Component({
  selector: 'app-placeholder-selector',
  templateUrl: './placeholder-selector.component.html',
  styleUrls: [ './placeholder-selector.component.less' ]
})
export class PlaceholderSelectorComponent implements OnInit {

  @Input() placeholders: KeyValue[] = []
  @Input() domainParameters?: boolean = false
  @Input() community?: number

  parameterPlaceholders?: KeyValue[]

  constructor(
    private dataService: DataService,
    private popupService: PopupService,
    private conformanceService: ConformanceService
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
  }

  selected(placeholder: KeyValue) {
    this.dataService.copyToClipboard(placeholder.key).subscribe(() => {
      this.popupService.success('Placeholder copied to clipboard.')
    })
  }

}
