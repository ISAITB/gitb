/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DiagramLoaderService } from './diagram-loader.service';
import { SessionData } from './session-data';

@Component({
    selector: 'app-test-session-presentation',
    templateUrl: './test-session-presentation.component.html',
    styleUrl: './test-session-presentation.component.less',
    standalone: false
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
