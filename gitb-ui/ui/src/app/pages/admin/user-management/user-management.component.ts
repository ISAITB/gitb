/*
 * Copyright (C) 2025 European Union
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
    private readonly dataService: DataService,
    private readonly communityService: CommunityService,
    private readonly routingService: RoutingService
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
