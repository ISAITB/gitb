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
