import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-form-section',
  templateUrl: './form-section.component.html',
  styleUrl: './form-section.component.less'
})
export class FormSectionComponent implements OnInit {

  @Input() sectionTitle!: string
  @Input() marginBefore = false
  @Input() marginAfter = false
  @Input() titleTooltip?: string
  @Input() collapsed? = false


  ngOnInit(): void {
    if (this.collapsed == undefined) {
      this.collapsed = false
    }
  }

}
