import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseComponent } from 'src/app/pages/base-component.component';

@Component({ template: '' })
export abstract class BaseReportSettingsFormComponent extends BaseComponent implements OnInit {

  @Input() communityId!: number
  @Input() selected!: EventEmitter<boolean>
  @Input() expanded!: EventEmitter<void>
  @Output() loaded = new EventEmitter<boolean>()
  @Output() collapse = new EventEmitter<boolean>()

  constructor() { super() }

  ngOnInit(): void {
    this.selected.subscribe(() => {
      this.loadData().subscribe(() => {
        this.loaded.emit(true)
      })
    })
    this.expanded.subscribe(() => {
      this.handleExpanded()
    })
  }

  abstract loadData(): Observable<any>

  handleExpanded() {
    // Do nothing by default
  }

  collapseForm() {
    this.collapse.emit(true)
  }

}
