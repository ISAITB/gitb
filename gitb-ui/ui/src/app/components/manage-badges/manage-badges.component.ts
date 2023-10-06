import { Component, Input, OnInit } from '@angular/core';
import { BadgesInfo } from './badges-info';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: 'app-manage-badges',
  templateUrl: './manage-badges.component.html'
})
export class ManageBadgesComponent implements OnInit {

  @Input() badges!: BadgesInfo

  badgesCollapsed: boolean = false
  successName?: string
  otherName?: string
  failureName?: string

  Constants = Constants

  constructor() { }

  ngOnInit(): void {
    this.badgesCollapsed = this.badges.initiallyEnabled != undefined && this.badges.initiallyEnabled
  }

  formCollapsed(): boolean {
    return this.badgesCollapsed
  }

}
