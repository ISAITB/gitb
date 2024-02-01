import { Component, Input } from '@angular/core';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: 'app-sort-indicator',
  templateUrl: './sort-indicator.component.html'
})
export class SortIndicatorComponent {

  @Input() columnType!: string
  @Input() sortColumn!: string
  @Input() sortOrder!: string
  Constants = Constants

}
