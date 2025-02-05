import { Component, Input } from '@angular/core';
import { Alert } from 'src/app/types/alert.type';

@Component({
    selector: 'app-alert-display',
    templateUrl: './alert-display.component.html',
    styles: ' .inTab ::ng-deep .alert { margin-bottom: 10px; } ',
    standalone: false
})
export class AlertDisplayComponent {

  @Input() alerts?: Alert[]
  @Input() outerClass = 'row'
  @Input() innerClass = ''

  constructor() { }

  clearAlert(alertIndex: number): void {
    if (this.alerts) {
      this.alerts.splice(alertIndex, 1)
    }
  }

}