import { Component, Input } from '@angular/core';

@Component({
    selector: 'app-pending-block',
    templateUrl: './pending-block.component.html',
    styles: '.large { font-size: xx-large; }',
    standalone: false
})
export class PendingBlockComponent {

  @Input() pending = true
  @Input() icon = false
  @Input() large = false

  constructor() { }

}
