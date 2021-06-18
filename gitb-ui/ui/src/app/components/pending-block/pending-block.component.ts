import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-pending-block',
  templateUrl: './pending-block.component.html'
})
export class PendingBlockComponent {

  @Input() pending = true
  @Input() icon = false

  constructor() { }

}
