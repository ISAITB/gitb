import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-test-step-report-sr',
  template: '<div class="step-report simple-step-report"></div>'
})
export class TestStepReportSRComponent {

  @Input() report: any

  constructor() { }

}
