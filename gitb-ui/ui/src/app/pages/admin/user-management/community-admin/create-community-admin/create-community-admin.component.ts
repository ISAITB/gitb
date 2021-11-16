import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EMPTY } from 'rxjs';
import { map, mergeMap, share } from 'rxjs/operators';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { RoutingService } from 'src/app/services/routing.service';
import { UserService } from 'src/app/services/user.service';
import { User } from 'src/app/types/user.type';
import { CommunityTab } from '../../community/community-details/community-tab.enum';

@Component({
  selector: 'app-create-community-admin',
  templateUrl: './create-community-admin.component.html',
  styles: [
  ]
})
export class CreateCommunityAdminComponent extends BaseComponent implements OnInit, AfterViewInit {

  communityId!: number
  user: Partial<User> = {}
  isSSO: boolean = false
  savePending = false

  constructor(
    private routingService: RoutingService,
    private route: ActivatedRoute,
    private userService: UserService,
    private authService: AuthService,
    public dataService: DataService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
    if (this.isSSO) {
      this.dataService.focus('email')
    } else {
      this.dataService.focus('name')
    }
  }

  ngOnInit(): void {
    this.communityId = Number(this.route.snapshot.paramMap.get('community_id'))
    this.isSSO = this.dataService.configuration.ssoEnabled
  }

  saveDisabled() {
    if (this.isSSO) {
      return !this.textProvided(this.user.email)
    } else {
      return !(this.textProvided(this.user.name) 
        && this.textProvided(this.user.email) 
        && this.textProvided(this.user.password) 
        && this.textProvided(this.user.passwordConfirmation))
    }
  }

  checkEmail(email: string) {
    if (this.isSSO) {
      return this.authService.checkEmailOfCommunityAdmin(email, this.communityId)
    } else {
      return this.authService.checkEmail(email)
    }
  }

  createAdmin() {
    this.clearAlerts()
    let ok = false
    if (this.isSSO) {
      ok = this.requireValidEmail(this.user.email, "Please enter a valid email address.")
    } else {
      ok = this.requireSame(this.user.password, this.user.passwordConfirmation, "Please enter equal passwords.")
    }
    if (ok) {
      this.savePending = true
      this.checkEmail(this.user.email!).pipe(
        mergeMap((data) => {
          if (data.available) {
            return this.userService.createCommunityAdmin(this.user.name!, this.user.email!, this.user.password!, this.communityId).pipe(
              map(() => {
                this.cancelCreateAdmin()
                this.popupService.success('Administrator created.')
              })
            )
          } else {
            if (this.dataService.configuration.ssoEnabled) {
              this.addAlertError('An administrator with this email address has already been registered.')
            } else {
              this.addAlertError('An administrator with this username has already been registered.')
            }
            return EMPTY
          }
        }),
        share()
      ).subscribe(() => {}).add(() => {
        this.savePending = false
      })
    }
  }

  cancelCreateAdmin() {
    this.routingService.toCommunity(this.communityId, CommunityTab.administrators)
  }

}
