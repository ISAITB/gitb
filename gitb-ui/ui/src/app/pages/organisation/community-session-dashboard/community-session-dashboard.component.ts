import { Component } from '@angular/core';
import {BaseSessionDashboardComponent} from '../../sessions/base-session-dashboard.component';

@Component({
  selector: 'app-community-session-dashboard',
  standalone: false,
  templateUrl: './../../sessions/base-session-dashboard.component.html'
})
export class CommunitySessionDashboardComponent extends BaseSessionDashboardComponent {

  protected showActiveTestSessions(): boolean {
    return false
  }

  protected showTestSessionNavigationControls(): boolean {
    return false
  }

  protected showTestSessionDeleteControls() {
    return false
  }

  protected setBreadcrumbs() {
    this.routingService.communitySessionsBreadcrumbs()
  }

}
