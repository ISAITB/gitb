import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DiagramEvents } from '../diagram-events';
import { StepData } from '../step-data';

@Component({
    selector: 'app-sequence-diagram-message-status',
    templateUrl: './sequence-diagram-message-status.component.html',
    styles: [],
    standalone: false
})
export class SequenceDiagramMessageStatusComponent implements OnInit {

  @Input() message!: StepData
  @Input() events!: DiagramEvents
  classValue!: string

  TEST_STATUS = Constants.TEST_STATUS

  constructor() { }

  ngOnInit(): void {
    this.classValue = this.calculateClass()
  }

  private calculateClass() {
    let classValue = 'status-wrapper '
    if (this.message.fromIndex! > this.message.toIndex!) {
      classValue += 'backwards-message '
    } else {
      if (this.message.fromIndex == this.message.toIndex) {
        classValue += 'self-message '
      }
    }
    classValue += this.message.type+'-type'
    return classValue
  }

}
