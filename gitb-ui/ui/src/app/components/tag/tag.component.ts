import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';

@Component({
    selector: 'app-tag',
    templateUrl: './tag.component.html',
    styleUrls: ['./tag.component.less'],
    standalone: false
})
export class TagComponent implements OnInit {

  @Input() id?: number
  @Input() label?: string
  @Input() value?: string
  @Input() tooltipText?: string
  @Input() foreground?: string
  @Input() background?: string
  @Input() styleClass?: string
  @Input() editable? = false
  @Input() icon? = false
  @Input() pill? = false

  @Output() edit = new EventEmitter<number>()
  @Output() delete = new EventEmitter<number>()

  Constants = Constants
  setDefaultBorder!: boolean

  constructor() { }

  ngOnInit(): void {
    this.setDefaultBorder = this.background == undefined 
      || this.background!.toLowerCase() == '#fff'
      || this.background!.toLowerCase() == '#ffffff'
  }

  editTag() {
    this.edit.emit(this.id)
  }

  deleteTag() {
    this.delete.emit(this.id)
  }
}
