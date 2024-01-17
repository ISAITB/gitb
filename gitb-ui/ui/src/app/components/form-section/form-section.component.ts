import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-form-section',
  templateUrl: './form-section.component.html',
  styleUrl: './form-section.component.less'
})
export class FormSectionComponent {

  @Input() sectionTitle!: string
  @Input() marginTop = true
  @Input() marginBottom = true
  collapsed = false

}
