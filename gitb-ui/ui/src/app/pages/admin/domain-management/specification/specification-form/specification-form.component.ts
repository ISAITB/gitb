import { Component, Input } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { Specification } from 'src/app/types/specification';

@Component({
  selector: 'app-specification-form',
  templateUrl: './specification-form.component.html',
  styles: [
  ]
})
export class SpecificationFormComponent {

  @Input() specification!: Partial<Specification>

  constructor(public dataService: DataService) { }

}
