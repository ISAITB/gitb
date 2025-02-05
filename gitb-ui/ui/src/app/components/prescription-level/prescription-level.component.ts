import { Component, Input } from '@angular/core';

@Component({
    selector: 'app-prescription-level',
    templateUrl: './prescription-level.component.html',
    styleUrls: ['./prescription-level.component.less'],
    standalone: false
})
export class PrescriptionLevelComponent {

  @Input() optional? = false
  @Input() disabled? = false

  constructor() { }

}
