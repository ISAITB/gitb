import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BaseReportSettingsFormComponent } from '../base-report-settings-form.component';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'app-conformance-statement-report-form',
  templateUrl: './conformance-statement-report-form.component.html'
})
export class ConformanceStatementReportFormComponent extends BaseReportSettingsFormComponent {

  constructor() { super() }

  loadData(): Observable<any> {
    return of(true)
  }

}
