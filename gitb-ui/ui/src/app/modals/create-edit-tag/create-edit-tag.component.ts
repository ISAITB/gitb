import { AfterViewInit, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { TestCaseTag } from 'src/app/types/test-case-tag';

@Component({
  selector: 'app-create-edit-tag',
  templateUrl: './create-edit-tag.component.html'
})
export class CreateEditTagComponent extends BaseComponent implements OnInit, AfterViewInit {

  @Input() tag?: Partial<TestCaseTag>
  @Input() tagToUse!: Partial<TestCaseTag>
  @Output() createdTag = new EventEmitter<TestCaseTag>()
  @Output() updatedTag = new EventEmitter<TestCaseTag>()

  isUpdate!: boolean
  title!: string

  constructor(
    private modalInstance: BsModalRef,
    private dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('nameIdentifier', 200)
  }

  ngOnInit(): void {
    this.isUpdate = this.tag != undefined
    if (this.isUpdate) {
      this.title = 'Update tag'
      this.tagToUse = {
        id: this.tag?.id,
        name: this.tag?.name,
        description: this.tag?.description,
        foreground: this.tag?.foreground,
        background: this.tag?.background
      }
    } else {
      this.title = 'Create tag'
      this.tagToUse = {}
    }
    if (this.tagToUse.background == undefined) {
      this.tagToUse.background = '#FFFFFF'
    }
    if (this.tagToUse.foreground == undefined) {
      this.tagToUse.foreground = '#777777'
    }
  }

  saveDisabled() {
    return !this.textProvided(this.tagToUse.name)
  }

  save() {
    if (this.isUpdate) {
      this.updatedTag.emit(this.tagToUse! as TestCaseTag)
    } else {
      this.createdTag.emit(this.tagToUse! as TestCaseTag)
    }
    this.cancel();
  }

  cancel() {
    this.modalInstance.hide()
  }
}
