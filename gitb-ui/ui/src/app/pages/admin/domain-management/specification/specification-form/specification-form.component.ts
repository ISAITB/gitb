import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { SpecificationService } from 'src/app/services/specification.service';
import { Specification } from 'src/app/types/specification';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
  selector: 'app-specification-form',
  templateUrl: './specification-form.component.html',
  styles: [
  ]
})
export class SpecificationFormComponent implements OnInit {

  @Input() specification!: Partial<Specification>
  groups: SpecificationGroup[] = []

  constructor(
    public dataService: DataService,
    private route: ActivatedRoute,
    private specificationService: SpecificationService
  ) { }

  ngOnInit(): void {
    const domainId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID))
    this.specificationService.getSpecificationGroups(domainId)
    .subscribe((data) => {
      this.groups = data
    })
  }

}
