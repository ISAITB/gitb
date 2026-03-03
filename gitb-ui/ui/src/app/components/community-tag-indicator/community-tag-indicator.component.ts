import {Component, HostBinding, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {DataService} from '../../services/data.service';
import {TagData} from '../../types/tag-data';
import {Observable, of, tap} from 'rxjs';
import {Constants} from '../../common/constants';
import {CommunityService} from '../../services/community.service';

@Component({
  selector: '[app-community-tag-indicator]',
  standalone: false,
  templateUrl: './community-tag-indicator.component.html'
})
export class CommunityTagIndicatorComponent implements OnInit {

  constructor(
    private readonly route: ActivatedRoute,
    private readonly communityService: CommunityService,
    private readonly dataService: DataService
  ) {}

  tags?: TagData[]

  ngOnInit(): void {
    let tags$: Observable<TagData[]>
    if ((this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) && this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      tags$ = this.loadTags(Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)))
    } else {
      tags$ = of([])
    }
    tags$.subscribe(tags => {
      this.tags = tags
    })
  }

  @HostBinding('class.ms-auto') get hasTags() {
    return this.tags != undefined
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
