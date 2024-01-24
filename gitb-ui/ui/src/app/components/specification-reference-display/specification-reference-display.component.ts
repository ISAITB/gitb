import { Component, Input } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { SpecificationReferenceInfo } from 'src/app/types/specification-reference-info';

@Component({
  selector: 'app-specification-reference-display',
  templateUrl: './specification-reference-display.component.html',
  styleUrl: './specification-reference-display.component.less'
})
export class SpecificationReferenceDisplayComponent {

  @Input() reference!: SpecificationReferenceInfo
  @Input() withBorder = true
  Constants = Constants

}
