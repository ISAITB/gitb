import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
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
  @Output() action = new EventEmitter<string>()

  showOrganisationProperties = false
  showSystemProperties = false
  showStatementProperties = false
  somethingIsVisible = false
  requiredPropertiesAreHidden = false

  constructor(
    private modalRef: BsModalRef
  ) { }

  ngOnInit(): void {
    this.showOrganisationProperties = this.organisationPropertyVisibility.hasVisibleMissingRequiredProperties || this.organisationPropertyVisibility.hasVisibleMissingOptionalProperties
    this.showSystemProperties = this.systemPropertyVisibility.hasVisibleMissingRequiredProperties || this.systemPropertyVisibility.hasVisibleMissingOptionalProperties
    this.showStatementProperties = this.statementPropertyVisibility.hasVisibleMissingRequiredProperties || this.statementPropertyVisibility.hasVisibleMissingOptionalProperties
    this.somethingIsVisible = this.showOrganisationProperties || this.showSystemProperties || this.showStatementProperties
    this.requiredPropertiesAreHidden = this.organisationPropertyVisibility.hasNonVisibleMissingRequiredProperties || this.systemPropertyVisibility.hasNonVisibleMissingRequiredProperties || this.statementPropertyVisibility.hasNonVisibleMissingRequiredProperties
  }

  toOrganisationProperties() {
    this.close('organisation')
  }

  toSystemProperties() {
    this.close('system')
  }

  toConfigurationProperties() {
    this.close('statement')
  }

  close(action?: string) {
    if (action != undefined) {
      this.action.emit(action)
    }
    this.modalRef.hide()
  }

}
