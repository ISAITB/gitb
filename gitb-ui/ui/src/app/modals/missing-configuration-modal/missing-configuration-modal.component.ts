import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { ConfigurationPropertyVisibility } from 'src/app/types/configuration-property-visibility';
import { OrganisationParameter } from 'src/app/types/organisation-parameter';
import { SystemParameter } from 'src/app/types/system-parameter';
import { MissingConfigurationAction } from './missing-configuration-action';
import { DataService } from 'src/app/services/data.service';
import { EndpointParameter } from 'src/app/types/endpoint-parameter';

@Component({
    selector: 'app-missing-configuration-modal',
    templateUrl: './missing-configuration-modal.component.html',
    styleUrls: ['./missing-configuration-modal.component.less'],
    standalone: false
})
export class MissingConfigurationModalComponent implements OnInit {

  @Input() organisationProperties!: OrganisationParameter[]
  @Input() organisationConfigurationValid!: boolean
  @Input() systemProperties!: SystemParameter[]
  @Input() systemConfigurationValid!: boolean
  @Input() statementProperties!: EndpointParameter[]
  @Input() configurationValid!: boolean
  @Output() action = new EventEmitter<MissingConfigurationAction>()

  organisationPropertyVisibility!: ConfigurationPropertyVisibility
  systemPropertyVisibility!: ConfigurationPropertyVisibility
  statementPropertyVisibility!: ConfigurationPropertyVisibility

  showOrganisationProperties = false
  showSystemProperties = false
  showStatementProperties = false
  somethingIsVisible = false
  requiredPropertiesAreHidden = false

  constructor(
    private modalRef: BsModalRef,
    private dataService: DataService
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
  }

  view() {
    this.close(true)
  }

  close(view?: boolean) {
    if (view == true) {
      this.action.emit({
        viewOrganisationProperties: this.showOrganisationProperties,
        viewSystemProperties: this.showSystemProperties,
        viewStatementProperties: this.showStatementProperties
      })
    }
    this.modalRef.hide()
  }

}
