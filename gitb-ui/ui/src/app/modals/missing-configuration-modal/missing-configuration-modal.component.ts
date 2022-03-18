import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { MissingConfigurationAction } from 'src/app/components/missing-configuration-display/missing-configuration-action';
import { ConfigurationPropertyVisibility } from 'src/app/types/configuration-property-visibility';
import { OrganisationParameterWithValue } from 'src/app/types/organisation-parameter-with-value';
import { SystemConfigurationParameter } from 'src/app/types/system-configuration-parameter';
import { SystemParameterWithValue } from 'src/app/types/system-parameter-with-value';

@Component({
  selector: 'app-missing-configuration-modal',
  templateUrl: './missing-configuration-modal.component.html',
  styles: [
  ]
})
export class MissingConfigurationModalComponent implements OnInit {

  @Input() organisationProperties!: OrganisationParameterWithValue[]
  @Input() organisationConfigurationValid!: boolean
  @Input() systemProperties!: SystemParameterWithValue[]
  @Input() systemConfigurationValid!: boolean
  @Input() statementProperties!: SystemConfigurationParameter[]
  @Input() configurationValid!: boolean
  @Input() organisationPropertyVisibility!: ConfigurationPropertyVisibility
  @Input() systemPropertyVisibility!: ConfigurationPropertyVisibility
  @Input() statementPropertyVisibility!: ConfigurationPropertyVisibility
  @Output() action = new EventEmitter<MissingConfigurationAction>()

  constructor(
    private modalRef: BsModalRef
  ) { }

  ngOnInit(): void {}

  close(action?: MissingConfigurationAction) {
    if (action != undefined) {
      this.action.emit(action)
    }
    this.modalRef.hide()
  }

}
