import { Component, Input, OnInit } from '@angular/core';
import { SystemParameter } from 'src/app/types/system-parameter';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { ConfigurationPropertyVisibility } from 'src/app/types/configuration-property-visibility';
import { EndpointParameter } from 'src/app/types/endpoint-parameter';

@Component({
  selector: 'app-missing-configuration-display',
  templateUrl: './missing-configuration-display.component.html',
  styleUrls: ['./missing-configuration-display.component.less']
})
export class MissingConfigurationDisplayComponent implements OnInit {

  @Input() organisationProperties!: OrganisationParameter[]
  @Input() organisationConfigurationValid!: boolean
  @Input() systemProperties!: SystemParameter[]
  @Input() systemConfigurationValid!: boolean
  @Input() statementProperties!: EndpointParameter[]
  @Input() configurationValid!: boolean
  @Input() organisationPropertyVisibility!: ConfigurationPropertyVisibility
  @Input() systemPropertyVisibility!: ConfigurationPropertyVisibility
  @Input() statementPropertyVisibility!: ConfigurationPropertyVisibility


  showOrganisationProperties = false
  showSystemProperties = false
  showStatementProperties = false
  somethingIsVisible = false
  requiredPropertiesAreHidden = false


  constructor() { }

  ngOnInit(): void {
    this.showOrganisationProperties = this.organisationPropertyVisibility.hasVisibleMissingRequiredProperties || this.organisationPropertyVisibility.hasVisibleMissingOptionalProperties
    this.showSystemProperties = this.systemPropertyVisibility.hasVisibleMissingRequiredProperties || this.systemPropertyVisibility.hasVisibleMissingOptionalProperties
    this.showStatementProperties = this.statementPropertyVisibility.hasVisibleMissingRequiredProperties || this.statementPropertyVisibility.hasVisibleMissingOptionalProperties
    this.somethingIsVisible = this.showOrganisationProperties || this.showSystemProperties || this.showStatementProperties
    this.requiredPropertiesAreHidden = this.organisationPropertyVisibility.hasNonVisibleMissingRequiredProperties || this.systemPropertyVisibility.hasNonVisibleMissingRequiredProperties || this.statementPropertyVisibility.hasNonVisibleMissingRequiredProperties
  }

}
