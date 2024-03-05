import { Component, OnInit } from '@angular/core';
import { BaseReportSettingsFormComponent } from '../base-report-settings-form.component';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'app-conformance-overview-report-form',
  templateUrl: './conformance-overview-report-form.component.html'
})
export class ConformanceOverviewReportFormComponent extends BaseReportSettingsFormComponent {

  constructor() { super() }

  loadData(): Observable<any> {
    return of(true)
  }

}
