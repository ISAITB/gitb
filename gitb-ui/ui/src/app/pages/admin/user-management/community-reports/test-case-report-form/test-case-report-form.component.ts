import { Component } from '@angular/core';
import { BaseReportSettingsFormComponent } from '../base-report-settings-form.component';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'app-test-case-report-form',
  templateUrl: './test-case-report-form.component.html'
})
export class TestCaseReportFormComponent extends BaseReportSettingsFormComponent {

  constructor() { super() }

  loadData(): Observable<any> {
    return of(true)
  }

}
