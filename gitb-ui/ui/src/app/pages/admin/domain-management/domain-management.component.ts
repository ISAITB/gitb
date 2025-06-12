import { Component, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Domain } from 'src/app/types/domain';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';

@Component({
    selector: 'app-domain-management',
    templateUrl: './domain-management.component.html',
    styles: [],
    standalone: false
})
export class DomainManagementComponent implements OnInit {

  dataStatus = {status: Constants.STATUS.PENDING}
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description'}
  ]
  domains: Domain[] = []


  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
		this.getDomains()
    this.routingService.domainsBreadcrumbs()    
  }

	getDomains() {
		if (this.dataService.isSystemAdmin) {
			this.conformanceService.getDomains()
			.subscribe((data) => {
				this.domains = data
				this.dataStatus.status = Constants.STATUS.FINISHED
      }).add(() => {
				this.dataStatus.status = Constants.STATUS.FINISHED
      })
    } else if (this.dataService.isCommunityAdmin) {
			this.conformanceService.getCommunityDomains(this.dataService.community!.id)
			.subscribe((data) => {
        if (data) {
          this.domains = data.domains
        }
      }).add(() => {
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

	onDomainSelect(domain: Domain) {
    this.routingService.toDomain(domain.id)
  }

  create() {
    this.routingService.toCreateDomain()
  }

}
