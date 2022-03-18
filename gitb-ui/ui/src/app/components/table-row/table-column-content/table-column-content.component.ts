import { Component, Input, OnInit } from '@angular/core';
import { TableColumnData } from 'src/app/types/table-column-data.type';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';

@Component({
  selector: 'app-table-column-content',
  templateUrl: './table-column-content.component.html'
})
export class TableColumnContentComponent implements OnInit {

  @Input() item!: TableColumnData
  @Input() column!: TableColumnDefinition

  constructor() { }

  ngOnInit(): void {
  }

}
