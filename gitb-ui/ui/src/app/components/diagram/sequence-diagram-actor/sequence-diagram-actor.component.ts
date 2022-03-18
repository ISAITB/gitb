import { Component, Input, OnInit } from '@angular/core';
import { ActorInfo } from '../actor-info';

@Component({
  selector: 'app-sequence-diagram-actor',
  templateUrl: './sequence-diagram-actor.component.html',
  styles: [
  ]
})
export class SequenceDiagramActorComponent implements OnInit {

  @Input() actor!: string
  @Input() actorInfo!: ActorInfo[]

  actorForDisplay!: string

  constructor() { }

  ngOnInit(): void {
    this.actorForDisplay = this.actor
    for (let info of this.actorInfo) {
      if (this.actor == info.id) {
        if (info.name != undefined) {
          this.actorForDisplay = info.name
        } else {
          this.actorForDisplay = info.id
        }
        if (info.role != undefined) {
          this.actorForDisplay += " (" + info.role + ")"
        }
      }
    }
  }

}
