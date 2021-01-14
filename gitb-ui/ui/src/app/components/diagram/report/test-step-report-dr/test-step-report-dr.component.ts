import { Component, Input } from '@angular/core';
import { StepReport } from '../step-report';

@Component({
  selector: 'app-test-step-report-dr',
  template: '<div class="step-report decision-step-report">'+
              '<div class="col-md-12">'+
                '<span><strong>Decision: </strong>{{report.decision}}</span>'+
              '</div>'+
            '</div>'
})
export class TestStepReportDRComponent {

  @Input() report!: StepReport

  constructor() { }

}
