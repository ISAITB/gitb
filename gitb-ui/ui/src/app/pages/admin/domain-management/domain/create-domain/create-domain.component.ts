import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Domain } from 'src/app/types/domain';

@Component({
  selector: 'app-create-domain',
  templateUrl: './create-domain.component.html',
  styles: [
  ]
})
export class CreateDomainComponent extends BaseComponent implements OnInit, AfterViewInit {

  domain: Partial<Domain> = {}
  pending = false

  constructor(
    public dataService: DataService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private router: Router
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('shortName')
  }

  ngOnInit(): void {
  }

	saveDisabled() {
    return !this.textProvided(this.domain.sname) || !this.textProvided(this.domain.fname)
  }

	createDomain() {
		if (!this.saveDisabled()) {
      this.pending = true
			this.conformanceService.createDomain(this.domain.sname!, this.domain.fname!, this.domain.description)
      .subscribe(() => {
        this.popupService.success(this.dataService.labelDomain()+' created.')
        this.router.navigate(['admin', 'domains'])
      }).add(() => {
        this.pending = false
      })
    }
  }

	cancel() {
    this.router.navigate(['admin', 'domains'])
  }

}
