import { Component } from '@angular/core';
import { BaseReportSettingsFormComponent } from '../base-report-settings-form.component';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'app-test-step-report-form',
  templateUrl: './test-step-report-form.component.html'
})
export class TestStepReportFormComponent extends BaseReportSettingsFormComponent {

  constructor() { super() }

  loadData(): Observable<any> {
    return of(true)
  }

}
