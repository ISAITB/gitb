import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { Actor } from 'src/app/types/actor';

@Component({
    selector: 'app-actor-form',
    templateUrl: './actor-form.component.html',
    styles: [],
    standalone: false
})
export class ActorFormComponent implements OnInit {

  @Input() actor!: Partial<Actor>

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

}
