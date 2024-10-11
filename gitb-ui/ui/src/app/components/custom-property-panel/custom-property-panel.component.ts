import { Component, Input } from '@angular/core';
import { CustomProperty } from 'src/app/types/custom-property.type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
  selector: 'app-custom-property-panel',
  templateUrl: './custom-property-panel.component.html',
  styleUrl: './custom-property-panel.component.less'
})
export class CustomPropertyPanelComponent {

  @Input() properties!: CustomProperty[]
  @Input() propertyType!: 'organisation'|'system'|'statement'
  @Input() owner!: number
  @Input() header!: string
  @Input() collapsed = false
  @Input() validation!: ValidationState
  @Input() topMargin = true
  @Input() readonly = false

  hovering = false
  headerCollapsed = false

  panelExpanding() {
    setTimeout(() => {
      this.headerCollapsed = false
    }, 1)
  }

}
