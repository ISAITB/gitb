import { Component, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { CommunityService } from 'src/app/services/community.service';
import { DataService } from 'src/app/services/data.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { Community } from 'src/app/types/community';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { User } from 'src/app/types/user.type';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styles: [
  ]
})
export class UserManagementComponent implements OnInit {

  adminStatus = {status: Constants.STATUS.PENDING}
  communityStatus = {status: Constants.STATUS.PENDING}
  adminColumns: TableColumnDefinition[] = []
  communityColumns = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' }
  ]
  admins: User[] = []
  communities: Community[] = []

  constructor(
    private dataService: DataService,
    private userService: UserService,
    private communityService: CommunityService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
    if (!this.dataService.isSystemAdmin) {
      this.routingService.toHome()
    }
    this.adminColumns.push({ field: 'name', title: 'Name' })
    if (this.dataService.configuration.ssoEnabled) {
      this.adminColumns.push({ field: 'email', title: 'Email' })
    } else {
      this.adminColumns.push({ field: 'email', title: 'Username' })
    }
    this.adminColumns.push({ field: 'ssoStatusText', title: 'Status' })
    this.userService.getSystemAdministrators()
    .subscribe((data) => {
      for (let admin of data) {
        admin.ssoStatusText = this.dataService.userStatus(admin.ssoStatus)
      }
      this.admins = data
    }).add(() => {
      this.adminStatus.status = Constants.STATUS.FINISHED
    })
    this.communityService.getCommunities()
    .subscribe((data) => {
      this.communities = data
    }).add(() => {
      this.communityStatus.status = Constants.STATUS.FINISHED
    })
  }

  adminSelect(admin: User) {
    this.routingService.toTestBedAdmin(admin.id!)
  }

  communitySelect(community: Community) {
    this.routingService.toCommunity(community.id)
  }

  createAdmin() {
    this.routingService.toCreateTestBedAdmin()
  }

  createCommunity() {
    this.routingService.toCreateCommunity()
  }
}
