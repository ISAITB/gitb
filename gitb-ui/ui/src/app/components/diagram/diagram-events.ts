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

import { Observable, Subject, Subscription } from "rxjs";

export class DiagramEvents {

    private loopSequencesUpdated: Subject<{ stepId: string }>
    private $loopSequencesUpdated: Observable<{ stepId: string }>
    private testLoaded: Subject<{ testId: number }>
    private $testLoaded: Observable<{ testId: number }>

    constructor() {
        this.loopSequencesUpdated = new Subject<{ stepId: string }>()
        this.$loopSequencesUpdated = this.loopSequencesUpdated.asObservable()
        this.testLoaded = new Subject<{ testId: number }>()
        this.$testLoaded = this.testLoaded.asObservable()
    }

    signalLoopSequenceUpdate(event: { stepId: string }) {
        this.loopSequencesUpdated.next(event)
    }

    subscribeToLoopSequenceUpdate(listener: (event: { stepId: string }) => any): Subscription {
        return this.$loopSequencesUpdated.subscribe(listener)
    }

    signalTestLoad(event: { testId: number }) {
        this.testLoaded.next(event)
    }

    subscribeToTestLoad(listener: (event: { testId: number }) => any): Subscription {
        return this.$testLoaded.subscribe(listener)
    }

}
