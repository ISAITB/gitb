import { AfterViewInit, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Specification } from 'src/app/types/specification';

@Component({
  selector: 'app-create-specification',
  templateUrl: './create-specification.component.html',
  styles: [
  ]
})
export class CreateSpecificationComponent extends BaseComponent implements OnInit, AfterViewInit {

  specification: Partial<Specification> = {}
  pending = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private route: ActivatedRoute,
    private router: Router
  ) { super() }

  ngAfterViewInit(): void {
		this.dataService.focus('shortName')
  }

  ngOnInit(): void {
  }

	saveDisabled() {
		return !(this.textProvided(this.specification.sname) && this.textProvided(this.specification.fname))
  }

	createSpecification() {
		if (!this.saveDisabled()) {
			let domainId = Number(this.route.snapshot.paramMap.get('id'))
      this.pending = true
      this.conformanceService.createSpecification(this.specification.sname!, this.specification.fname!, this.specification.description, this.specification.hidden, domainId)
      .subscribe(() => {
        this.router.navigate(['admin', 'domains', domainId])
        this.popupService.success(this.dataService.labelSpecification()+' created.')
      }).add(() => {
        this.pending = false
      })
    }
  }

	cancel() {
    let domainId = Number(this.route.snapshot.paramMap.get('id'))
    this.router.navigate(['admin', 'domains', domainId])
  }

}