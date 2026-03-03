import {Component, HostBinding, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Constants} from '../../common/constants';
import {TagData} from '../../types/tag-data';
import {Observable, of, tap} from 'rxjs';
import {ConformanceService} from '../../services/conformance.service';
import {DataService} from '../../services/data.service';

@Component({
  selector: '[app-domain-tag-indicator]',
  standalone: false,
  templateUrl: './domain-tag-indicator.component.html'
})
export class DomainTagIndicatorComponent implements OnInit {

  constructor(
    private readonly route: ActivatedRoute,
    private readonly conformanceService: ConformanceService,
    private readonly dataService: DataService
  ) {}

  tags?: TagData[]

  ngOnInit(): void {
    let tags$: Observable<TagData[]>
    if ((this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) && this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID)) {
      tags$ = this.loadTags(Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID)))
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
    const cachedTags = this.dataService.retrieveCachedDomainTags(id)
    if (cachedTags == undefined) {
      return this.conformanceService.getDomainTags(id).pipe(
        tap(tags => {
          this.dataService.cacheDomainTags(id, tags)
        })
      )
    } else {
      return of(cachedTags)
    }
  }

}
