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

import {Component, Input, OnInit} from '@angular/core';
import {ConfigurationPropertyVisibility} from 'src/app/types/configuration-property-visibility';
import {OrganisationParameter} from 'src/app/types/organisation-parameter';
import {SystemParameter} from 'src/app/types/system-parameter';
import {DataService} from 'src/app/services/data.service';
import {EndpointParameter} from 'src/app/types/endpoint-parameter';
import {Constants} from '../../common/constants';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-missing-configuration-modal',
    templateUrl: './missing-configuration-modal.component.html',
    standalone: false
})
export class MissingConfigurationModalComponent implements OnInit {

  @Input() organisationProperties!: OrganisationParameter[]
  @Input() organisationConfigurationValid!: boolean
  @Input() systemProperties!: SystemParameter[]
  @Input() systemConfigurationValid!: boolean
  @Input() statementProperties!: EndpointParameter[]
  @Input() configurationValid!: boolean

  organisationPropertyVisibility!: ConfigurationPropertyVisibility
  systemPropertyVisibility!: ConfigurationPropertyVisibility
  statementPropertyVisibility!: ConfigurationPropertyVisibility

  showOrganisationProperties = false
  showSystemProperties = false
  showStatementProperties = false
  somethingIsVisible = false
  requiredPropertiesAreHidden = false
  requiredPropertiesIncludeEditable = false
  requiredPropertiesIncludeNonEditable = false

  constructor(
    private readonly modalRef: NgbActiveModal,
    public readonly dataService: DataService
  ) { }

  ngOnInit(): void {
    this.organisationPropertyVisibility = this.dataService.checkPropertyVisibility(this.organisationProperties!)
    this.systemPropertyVisibility = this.dataService.checkPropertyVisibility(this.systemProperties!)
    this.statementPropertyVisibility = this.dataService.checkPropertyVisibility(this.statementProperties)
    this.showOrganisationProperties = this.organisationPropertyVisibility.hasVisibleMissingRequiredProperties
    this.showSystemProperties = this.systemPropertyVisibility.hasVisibleMissingRequiredProperties
    this.showStatementProperties = this.statementPropertyVisibility.hasVisibleMissingRequiredProperties
    this.somethingIsVisible = this.showOrganisationProperties || this.showSystemProperties || this.showStatementProperties
    this.requiredPropertiesAreHidden = this.organisationPropertyVisibility.hasNonVisibleMissingRequiredProperties || this.systemPropertyVisibility.hasNonVisibleMissingRequiredProperties || this.statementPropertyVisibility.hasNonVisibleMissingRequiredProperties
    this.requiredPropertiesIncludeEditable = this.organisationPropertyVisibility.hasVisibleMissingRequiredEditableProperties || this.systemPropertyVisibility.hasVisibleMissingRequiredEditableProperties || this.statementPropertyVisibility.hasVisibleMissingRequiredEditableProperties
    this.requiredPropertiesIncludeNonEditable = this.organisationPropertyVisibility.hasVisibleMissingRequiredNonEditableProperties || this.systemPropertyVisibility.hasVisibleMissingRequiredNonEditableProperties || this.statementPropertyVisibility.hasVisibleMissingRequiredNonEditableProperties
  }

  view() {
    this.close(true)
  }

  close(view?: boolean) {
    if (view == true) {
      this.modalRef.close({
        viewOrganisationProperties: this.showOrganisationProperties,
        viewSystemProperties: this.showSystemProperties,
        viewStatementProperties: this.showStatementProperties
      })
    } else {
      this.modalRef.dismiss()
    }
  }

  protected readonly Constants = Constants;
}
