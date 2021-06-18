import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { Community } from 'src/app/types/community';
import { Domain } from 'src/app/types/domain';
import { IdLabel } from 'src/app/types/id-label';
import { find } from 'lodash';

@Component({
  selector: 'app-community-form',
  templateUrl: './community-form.component.html',
  styles: [
  ]
})
export class CommunityFormComponent extends BaseComponent implements OnInit {

  @Input() community!: Partial<Community>
  @Input() domains: Partial<Domain>[] = []
  @Input() admin = false
  selfRegEnabled = false
  ssoEnabled = false
  emailEnabled = false
  selfRegTypes: IdLabel[] = [
    {id: Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED, label: 'Not supported'}, 
    {id: Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING, label: 'Select from public communities'}, 
    {id: Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN, label: 'Select from public communities and provide token'} 
  ]
  selfRegRestrictions: IdLabel[] = []
  selfRegOptionsVisible = false

  constructor(
    public dataService: DataService
  ) { super() }

  ngOnInit(): void {
    this.selfRegEnabled = this.dataService.configuration.registrationEnabled
    this.ssoEnabled = this.dataService.configuration.ssoEnabled
    this.emailEnabled = this.dataService.configuration.emailEnabled
    if (this.ssoEnabled) {
      this.selfRegRestrictions = [
        {id: Constants.SELF_REGISTRATION_RESTRICTION.NO_RESTRICTION, label: 'No restrictions'}, 
        {id: Constants.SELF_REGISTRATION_RESTRICTION.USER_EMAIL, label: 'One registration allowed per user'}, 
        {id: Constants.SELF_REGISTRATION_RESTRICTION.USER_EMAIL_DOMAIN, label: 'One registration allowed per user email domain'} 
      ]
    }
    this.selfRegOptionsVisible = this.community.selfRegType != Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED
    this.community.sameDescriptionAsDomain = this.community.domain != undefined && !(this.textProvided(this.community.description))
    if (this.community.sameDescriptionAsDomain) {
      this.community.activeDescription = this.community.domain!.description
    } else {
      this.community.activeDescription = this.community.description
    }
  }

  showToken() {
    return this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN || this.community.selfRegType == Constants.SELF_REGISTRATION_TYPE.TOKEN
  }
    
  selfRegTypeChanged() {
    this.selfRegOptionsVisible = this.community.selfRegType != Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED
    if (this.showToken()) {
      this.dataService.focus('selfRegToken', 200)
    }
  }

  identifyDomain(index: number, item: Partial<Domain>) {
    return item?.id
  }

  private domainDescription(domainId: number) {
    return find(this.domains, (d) => { return d.id === domainId })!.description
  }

  domainChanged() {
    if (this.community.domainId != undefined) {
      if (this.community.sameDescriptionAsDomain) {
        this.community.activeDescription = this.domainDescription(this.community.domainId!)
      }
    } else {
      if (this.community.sameDescriptionAsDomain) {
        this.community.activeDescription = ''
        this.community.sameDescriptionAsDomain = false
      }
    }
  }

  descriptionCheckChanged() {
    if (this.community.sameDescriptionAsDomain) {
      this.community.activeDescription = this.domainDescription(this.community.domainId!)
    }
  }

  setSameDescription() {
    this.community.sameDescriptionAsDomain = this.community.domainId != undefined && !(this.textProvided(this.community.activeDescription))
    if (this.community.sameDescriptionAsDomain) {
      this.community.activeDescription = this.domainDescription(this.community.domainId!)
    }
  }

}
