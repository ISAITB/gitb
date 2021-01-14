import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OptionalCustomPropertyFormData } from 'src/app/components/optional-custom-property-form/optional-custom-property-form-data.type';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';
import { System } from 'src/app/types/system';

@Component({
  selector: 'app-system-info',
  templateUrl: './system-info.component.html',
  styles: [
  ]
})
export class SystemInfoComponent implements OnInit {

  system: Partial<System> = {}
  systemId!: number
  propertyData: OptionalCustomPropertyFormData = {
    properties: [],
    edit: false
  }  

  constructor(
    private systemService: SystemService,
    public dataService: DataService,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
    const viewPropertiesParam = this.route.snapshot.queryParamMap.get('viewProperties')
    if (viewPropertiesParam != undefined) {
      this.propertyData.edit = Boolean(viewPropertiesParam)
    }
		this.systemService.getSystem(this.systemId)
    .subscribe((data) => {
      this.system = data
    })
		this.systemService.getSystemParameterValues(this.systemId)
    .subscribe((data) => {
      this.propertyData.properties = data
    })
  }

}
