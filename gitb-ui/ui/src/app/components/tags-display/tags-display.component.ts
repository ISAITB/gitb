import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { sortBy } from 'lodash';
import { TestCaseTag } from 'src/app/types/test-case-tag';

@Component({
  selector: 'app-tags-display',
  templateUrl: './tags-display.component.html',
  styleUrls: [ './tags-display.component.less' ]
})
export class TagsDisplayComponent implements OnInit {

  @Input() tags?: TestCaseTag[]
  @Input() editable? = false

  @Output() edit = new EventEmitter<number>()
  @Output() delete = new EventEmitter<number>()
  @Output() create = new EventEmitter<void>()

  constructor() { }

  ngOnInit(): void {
    if (this.tags) {
      this.tags = sortBy(this.tags, ['name'])
    }
  }

  tagEdited(id: number) {
    this.edit.emit(id)
  }

  tagDeleted(id: number) {
    this.delete.emit(id)
  }

  createTag() {
    this.create.emit()
  }
}
