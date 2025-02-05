import { Component, Input } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { SpecificationReferenceInfo } from 'src/app/types/specification-reference-info';

@Component({
    selector: 'app-specification-reference-form',
    templateUrl: './specification-reference-form.component.html',
    standalone: false
})
export class SpecificationReferenceFormComponent {

  @Input() reference!: SpecificationReferenceInfo
  Constants = Constants

}
