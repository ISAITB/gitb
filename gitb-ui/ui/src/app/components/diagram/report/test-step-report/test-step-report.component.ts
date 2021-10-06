import { Component, Input, OnInit } from '@angular/core';
import { StepData } from '../../step-data';
import { StepReport } from '../step-report';

@Component({
  selector: 'app-test-step-report',
  templateUrl: './test-step-report.component.html',
  styles: [
  ]
})
export class TestStepReportComponent implements OnInit {

  @Input() step!: StepData
  @Input() report!: StepReport
  @Input() sessionId!: string

  classValue!: string

  constructor() { }

  ngOnInit(): void {
    this.classValue = 'row test-step-report '+this.step.type+'-type'
  }

}