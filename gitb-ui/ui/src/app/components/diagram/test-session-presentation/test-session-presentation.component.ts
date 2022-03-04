import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DiagramLoaderService } from './diagram-loader.service';
import { SessionData } from './session-data';

@Component({
  selector: 'app-test-session-presentation',
  templateUrl: './test-session-presentation.component.html'
})
export class TestSessionPresentationComponent implements OnInit {

  @Input() session!: SessionData
  @Output() ready = new EventEmitter<SessionData>()

  constructor(
    private diagramLoaderService: DiagramLoaderService
  ) { }

  ngOnInit(): void {
    if (this.session.diagramState) {
      this.ready.emit(this.session)
    } else {
      this.diagramLoaderService.loadTestSessionData(this.session)
      .subscribe((data) => {
        this.session.diagramState = data
        this.ready.emit(this.session)
      })
    }
  }

}
