import { Component, Input } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { SpecificationGroup } from 'src/app/types/specification-group';

@Component({
  selector: 'app-specification-group-form',
  templateUrl: './specification-group-form.component.html',
  styles: [
  ]
})
export class SpecificationGroupFormComponent {

  @Input() group!: Partial<SpecificationGroup>

  constructor(public dataService: DataService) { }

}
