import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { OrganisationParameterWithValue } from 'src/app/types/organisation-parameter-with-value';
import { SystemConfigurationParameter } from 'src/app/types/system-configuration-parameter';
import { SystemParameterWithValue } from 'src/app/types/system-parameter-with-value';
import { MissingConfigurationAction } from './missing-configuration-action';

@Component({
  selector: 'app-missing-configuration-display',
  templateUrl: './missing-configuration-display.component.html',
  styleUrls: ['./missing-configuration-display.component.less']
})
export class MissingConfigurationDisplayComponent implements OnInit {

  @Input() organisationProperties!: OrganisationParameterWithValue[]
  @Input() organisationConfigurationValid!: boolean
  @Input() systemProperties!: SystemParameterWithValue[]
  @Input() systemConfigurationValid!: boolean
  @Input() statementProperties!: SystemConfigurationParameter[]
  @Input() configurationValid!: boolean
  @Output() action = new EventEmitter<MissingConfigurationAction>()

  showOrganisationProperties = false
  showSystemProperties = false
  showStatementProperties = false
  somethingIsVisible = false
  requiredPropertiesAreHidden = false


  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    const organisationPropertyVisibility = this.dataService.checkPropertyVisibility(this.organisationProperties!)
    const systemPropertyVisibility = this.dataService.checkPropertyVisibility(this.systemProperties!)
    const statementPropertyVisibility = this.dataService.checkPropertyVisibility(this.statementProperties)
    this.showOrganisationProperties = organisationPropertyVisibility.hasVisibleMissingRequiredProperties || organisationPropertyVisibility.hasVisibleMissingOptionalProperties
    this.showSystemProperties = systemPropertyVisibility.hasVisibleMissingRequiredProperties || systemPropertyVisibility.hasVisibleMissingOptionalProperties
    this.showStatementProperties = statementPropertyVisibility.hasVisibleMissingRequiredProperties || statementPropertyVisibility.hasVisibleMissingOptionalProperties
    this.somethingIsVisible = this.showOrganisationProperties || this.showSystemProperties || this.showStatementProperties
    this.requiredPropertiesAreHidden = organisationPropertyVisibility.hasNonVisibleMissingRequiredProperties || systemPropertyVisibility.hasNonVisibleMissingRequiredProperties || statementPropertyVisibility.hasNonVisibleMissingRequiredProperties
  }

  toOrganisationProperties() {
    this.action.emit(MissingConfigurationAction.viewOrganisation)
  }

  toSystemProperties() {
    this.action.emit(MissingConfigurationAction.viewSystem)
  }

  toConfigurationProperties() {
    this.action.emit(MissingConfigurationAction.viewStatement)
  }

}
