import { Component, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { CommunityService } from 'src/app/services/community.service';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { Community } from 'src/app/types/community';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';

@Component({
    selector: 'app-user-management',
    templateUrl: './user-management.component.html',
    styles: [],
    standalone: false
})
export class UserManagementComponent implements OnInit {

  communityStatus = {status: Constants.STATUS.PENDING}
  communityColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' }
  ]
  communities: Community[] = []

  constructor(
    private dataService: DataService,
    private communityService: CommunityService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
    if (!this.dataService.isSystemAdmin) {
      this.routingService.toHome()
    }
    this.communityService.getUserCommunities()
    .subscribe((data) => {
      this.communities = data
    }).add(() => {
      this.communityStatus.status = Constants.STATUS.FINISHED
    })
    this.routingService.communitiesBreadcrumbs()
  }

  communitySelect(community: Community) {
    this.routingService.toCommunity(community.id)
  }


  createCommunity() {
    this.routingService.toCreateCommunity()
  }
}
