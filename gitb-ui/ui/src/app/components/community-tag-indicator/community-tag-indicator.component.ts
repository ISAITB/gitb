import {Component, HostBinding, Input, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {DataService} from '../../services/data.service';
import {TagData} from '../../types/tag-data';
import {Observable, of, Subscription, tap} from 'rxjs';
import {Constants} from '../../common/constants';
import {CommunityService} from '../../services/community.service';

@Component({
  selector: '[app-community-tag-indicator]',
  standalone: false,
  templateUrl: './community-tag-indicator.component.html'
})
export class CommunityTagIndicatorComponent implements OnInit, OnDestroy {

  @Input() displayForCommunityAdmin = false
  @Input() lazy = false

  tags?: TagData[]
  private communityLoadedSubscription?: Subscription

  constructor(
    private readonly route: ActivatedRoute,
    private readonly communityService: CommunityService,
    protected readonly dataService: DataService
  ) {}

  ngOnInit(): void {
    if (this.lazy) {
      this.communityLoadedSubscription = this.dataService.onCommunityLoaded$.subscribe(() => {
        this.setupTags()
      })
    } else {
      this.setupTags()
    }
  }

  ngOnDestroy(): void {
    if (this.communityLoadedSubscription) this.communityLoadedSubscription.unsubscribe()
  }

  @HostBinding('class') class = 'd-flex align-items-center';
  @HostBinding('class.ms-auto') get hasTags() {
    return this.tags != undefined
  }

  private setupTags(): void {
    let tags$: Observable<TagData[]>
    if (this.dataService.isSystemAdmin) {
      if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
        // Load tags for a route that is community-specific.
        tags$ = this.loadTags(Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)))
      } else {
        tags$ = of([])
      }
    } else if (this.dataService.isCommunityAdmin) {
      if (this.displayForCommunityAdmin && this.dataService.community?.id != undefined) {
        tags$ = this.loadTags(this.dataService.community.id)
      } else {
        tags$ = of([])
      }
    } else {
      tags$ = of([])
    }
    tags$.subscribe(tags => {
      this.tags = tags
    })
  }

  private loadTags(id: number): Observable<TagData[]> {
    const cachedTags = this.dataService.retrieveCachedCommunityTags(id)
    if (cachedTags == undefined) {
      return this.communityService.getCommunityTags(id).pipe(
        tap(tags => {
          this.dataService.cacheCommunityTags(id, tags)
        })
      )
    } else {
      return of(cachedTags)
    }
  }

}
