import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { RoutingService } from 'src/app/services/routing.service';
import { BreadcrumbItem } from './breadcrumb-item';
import { DataService } from 'src/app/services/data.service';
import { find } from 'lodash';
import { BreadcrumbType } from 'src/app/types/breadcrumb-type';
import { BreadcrumbService } from 'src/app/services/breadcrumb.service';
import { BreadcrumbLabelRequest } from 'src/app/types/breadcrumb-label-request';
import { Subscription } from 'rxjs';
import { AuthProviderService } from 'src/app/services/auth-provider.service';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: 'app-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: [ './breadcrumb.component.less' ]
})
export class BreadcrumbComponent implements OnInit, OnDestroy {

  @Input() logoutInProgress: boolean = false

  breadcrumbs?: BreadcrumbItem[]
  breadcrumbsToShow?: BreadcrumbItem[]
  homeCrumb!: BreadcrumbItem
  breadcrumbSubscription?: Subscription
  hasOverflow = false

  Constants = Constants

  constructor(
    private routingService: RoutingService,
    private dataService: DataService,
    private breadcrumbService: BreadcrumbService,
    private authProviderService: AuthProviderService
  ) { }

  ngOnInit(): void {
    this.homeCrumb = {
      label: 'Home',
      type: BreadcrumbType.home,
      action: () => this.routingService.toHome()
    }
    this.breadcrumbSubscription = this.dataService.onBreadcrumbChange$.subscribe((info) => {
      if (info.breadcrumbs) {
        this.updateBreadcrumbs(info.breadcrumbs)
      }
      if (info.id != undefined && info.type != undefined && info.label != undefined && this.breadcrumbs) {
        this.updateBreadcrumbLabel(this.breadcrumbs, info.id, info.type, info.label)
      }
    })
  }

  ngOnDestroy(): void {
    if (this.breadcrumbSubscription) this.breadcrumbSubscription.unsubscribe()
  }

  private updateBreadcrumbLabel(crumbs: BreadcrumbItem[], id: number|string, type: BreadcrumbType, label: string) {
    const locatedCrumb = find(crumbs, (crumb) => crumb.type == type && crumb.typeId == id)
    if (locatedCrumb) {
      locatedCrumb.label = label
    }
  }

  private completeBreadcrumbs(breadcrumbs: BreadcrumbItem[]) {
    return [this.homeCrumb].concat(breadcrumbs)
  }

  private updateBreadcrumbs(newCrumbs: BreadcrumbItem[]) {
    const crumbsToLookup: BreadcrumbItem[] = []
    for (let newCrumb of newCrumbs) {
      if (newCrumb.label == undefined && newCrumb.typeId != undefined) {
        if (this.breadcrumbs) {
          const previousCrumbs = this.breadcrumbs
          const locatedCrumb = find(previousCrumbs, (crumb) => crumb.type == newCrumb.type && crumb.typeId == newCrumb.typeId)
          if (locatedCrumb) {
            newCrumb.label = locatedCrumb.label
          } else {
            crumbsToLookup.push(newCrumb)
          }
        } else {
          crumbsToLookup.push(newCrumb)
        }
      }
    }
    if (crumbsToLookup.length == 0) {
      this.breadcrumbs = newCrumbs
      this.breadcrumbsToShow = this.completeBreadcrumbs(this.breadcrumbs)
    } else {
      // Lookup missing breadcrumb labels.
      const request: BreadcrumbLabelRequest = {}
      for (let missingCrumb of crumbsToLookup) {
        switch (missingCrumb.type) {
          case BreadcrumbType.domain:
          case BreadcrumbType.myDomain: request.domain = missingCrumb.typeId! as number; break;
          case BreadcrumbType.specificationGroup: request.specificationGroup = missingCrumb.typeId! as number; break;
          case BreadcrumbType.specification: request.specification = missingCrumb.typeId! as number; break;
          case BreadcrumbType.actor: request.actor = missingCrumb.typeId! as number; break;
          case BreadcrumbType.community:
          case BreadcrumbType.myCommunity: request.community = missingCrumb.typeId! as number; break;
          case BreadcrumbType.ownOrganisation:
          case BreadcrumbType.organisation: request.organisation = missingCrumb.typeId! as number; break;
          case BreadcrumbType.ownSystem:
          case BreadcrumbType.system: request.system = missingCrumb.typeId! as number; break;
        }
      }
      this.breadcrumbService.getBreadcrumbLabels(request)
      .subscribe((data) => {
        if (data.domain) {
          const crumb = find(crumbsToLookup, (crumb) => crumb.type == BreadcrumbType.domain || crumb.type == BreadcrumbType.myDomain)
          if (crumb) {
            crumb.label = data.domain
          }
        }
        if (data.specificationGroup) {
          const crumb = find(crumbsToLookup, (crumb) => crumb.type == BreadcrumbType.specificationGroup)
          if (crumb) {
            crumb.label = data.specificationGroup
          }
        }
        if (data.specification) {
          const crumb = find(crumbsToLookup, (crumb) => crumb.type == BreadcrumbType.specification)
          if (crumb) {
            crumb.label = data.specification
          }
        }
        if (data.actor) {
          const crumb = find(crumbsToLookup, (crumb) => crumb.type == BreadcrumbType.actor)
          if (crumb) {
            crumb.label = data.actor
          }
        }
        if (data.community) {
          const crumb = find(crumbsToLookup, (crumb) => crumb.type == BreadcrumbType.community || crumb.type == BreadcrumbType.myCommunity)
          if (crumb) {
            crumb.label = data.community
          }
        }
        if (data.organisation) {
          const crumb = find(crumbsToLookup, (crumb) => crumb.type == BreadcrumbType.organisation || crumb.type == BreadcrumbType.ownOrganisation)
          if (crumb) {
            crumb.label = data.organisation
          }
        }
        if (data.system) {
          const crumb = find(crumbsToLookup, (crumb) => crumb.type == BreadcrumbType.system || crumb.type == BreadcrumbType.ownSystem)
          if (crumb) {
            crumb.label = data.system
          }
        }
        this.breadcrumbs = newCrumbs
        this.breadcrumbsToShow = this.completeBreadcrumbs(newCrumbs)
      })
    }
  }

  breadcrumbClicked(crumb: BreadcrumbItem) {
    crumb.action()
  }

  isAuthenticated(): boolean {
    return this.authProviderService.isAuthenticated()
  }

  mouseOver(element: MouseEvent) {
    if ((element.target as HTMLElement).offsetWidth && 
        (element.target as HTMLElement).scrollWidth &&
        (element.target as HTMLElement).offsetWidth < (element.target as HTMLElement).scrollWidth) {
      this.hasOverflow = true
    } else {
      this.hasOverflow = false
    }
  }  

}
