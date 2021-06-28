import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ActorInfo } from '../actor-info';
import { StepData } from '../step-data';
import { map, max, flatten } from 'lodash'
import { ReportService } from 'src/app/services/report.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { TestStepReportModalComponent } from '../test-step-report-modal/test-step-report-modal.component';
import { HtmlService } from 'src/app/services/html.service';
import { DiagramEvents } from '../diagram-events';
import { Subscription } from 'rxjs';
import { StepReport } from '../report/step-report';

@Component({
  selector: 'app-sequence-diagram-message',
  templateUrl: './sequence-diagram-message.component.html'
})
export class SequenceDiagramMessageComponent implements OnInit, OnDestroy {

  @Input() message!: StepData
  @Input() actorInfo!: ActorInfo[]
  @Input() events!: DiagramEvents

  TEST_STATUS = Constants.TEST_STATUS
  depth!: number
  currentIterationIndex: number = -1
  classForMessageFixed!: string
  classForWrapper!: string 
  classForReverseOffset!: string
  eventSubscription?: Subscription
  expanded = true
  hoveringTitle = false

  constructor(
    private reportService: ReportService,
    private modalService: BsModalService,
    private htmlService: HtmlService
  ) { }

  ngOnInit(): void {
    this.depth = this.calculateDepth(this.message)
    this.classForMessageFixed = this.calculateFixedMessageClass()
    this.classForWrapper = 'message-wrapper offset-'+this.message.fromIndex+' '+this.message.type+'-type'
    this.classForReverseOffset = 'reverse-offset-'+this.message.fromIndex
    if (this.message.type == 'loop') {
      this.onSequenceChange(false)
      this.eventSubscription = this.events.subscribeToLoopSequenceUpdate(((event: {stepId: string}) => {
        if (this.message.sequences != undefined && this.message.id == event.stepId) {
          this.onSequenceChange(true)
        }
      }).bind(this))
    }
    if (this.message.title == undefined || this.message.title == null) {
      this.message.title = this.message.type
    }
  }

  ngOnDestroy(): void {
    if (this.eventSubscription != undefined) {
      this.eventSubscription.unsubscribe()
    }
  }

  private onSequenceChange(liveUpdate: boolean) {
    if (this.message.sequences) {
      if (liveUpdate) {
        // Show latest
        this.showLoopIteration(this.message.sequences.length - 1)
      } else {
        // Show first
        this.showLoopIteration(0)
      }
    } else {
      this.currentIterationIndex = -1
    }
  }

  classForThread(index: number) {
    return 'child-steps thread thread-'+index+' '+this.classForReverseOffset
  }

  classForMessage() {
    let classValue = this.classForMessageFixed!
    if (this.message.status == Constants.TEST_STATUS.PROCESSING) {
      classValue += ' processing'
    } else if (this.message.status == Constants.TEST_STATUS.SKIPPED) {
      classValue += ' skipped'
    } else if (this.message.status == Constants.TEST_STATUS.WAITING) {
      classValue += ' waiting'
    } else if (this.message.status == Constants.TEST_STATUS.ERROR) {
      classValue += ' error'
    } else if (this.message.status == Constants.TEST_STATUS.WARNING) {
      classValue += ' warning'
    } else if (this.message.status == Constants.TEST_STATUS.COMPLETED) {
      classValue += ' completed'
    }
    return classValue
  }

  calculateDepth(message: StepData): number {
    // if (message.level == undefined) {
    //   message.level = (message.id.split('.')).length
    // }
    if (message.type == 'loop') {
      let childDepths = map(message.steps, this.calculateDepth.bind(this))
      return (max(childDepths)!) + 1
    } else if (message.type == 'group') {
      let childDepths = map(message.steps, this.calculateDepth.bind(this))
      return (max(childDepths)!) + 1
    } else if (message.type == 'decision') {
      let childDepths: number[]
      if (message.else != undefined) {
        childDepths = map((message.then!.concat(message.else)), this.calculateDepth.bind(this))
      } else {
        childDepths = map(message.then, this.calculateDepth.bind(this))
      }
      return (max(childDepths)!) + 1
    } else if (message.type == 'flow') {
      let childDepths = map((flatten(message.threads)), this.calculateDepth.bind(this))
      return (max(childDepths)!) + 1
    } else if (message.type == 'interact') {
      let childDepths = map(message.interactions, this.calculateDepth.bind(this))
      return (max(childDepths)!) + 1
    } else if (message.type == 'instruction' || message.type == 'request') {
      return 1
    } else {
      return message.level!
    }
  }

  calculateFixedMessageClass() {
    let classValue = 'message span-'+this.message.span+' '
    if (this.message.fromIndex! > this.message.toIndex!) {
      classValue += 'backwards-message'
    } else if (this.message.fromIndex == this.message.toIndex) {
      classValue += 'self-message'
    }
    classValue += ' reverse-offset-'+this.message.span+' depth-'+this.depth+' level-'+this.message.level
    return classValue
  }

  showReport() {
    if (this.message.report != undefined) {
      if (this.message.report.tcInstanceId != undefined && this.message.report.path != undefined && this.message.report.result == undefined) {
        this.reportService.getTestStepReport(this.message.report.tcInstanceId, this.message.report.path)
        .subscribe((report) => {
          this.showTestStepReportModal(report)
        })
      } else {
        this.showTestStepReportModal(this.message.report)
      }
    }
  }

  showTestStepReportModal(report: StepReport) {
    const modalRef = this.modalService.show(TestStepReportModalComponent, {
      class: 'modal-lg',
      initialState: {
        step: this.message,
        report: report,
        sessionId: this.message.report!.tcInstanceId
      }
    })
  }

  showStepDocumentation(documentation: string) {
    this.htmlService.showHtml('Step information', documentation)
  }

  showLoopIteration(iteration: number) {
    this.currentIterationIndex = iteration
  }

  titleClick() {
    this.expanded = !this.expanded
  }

}
