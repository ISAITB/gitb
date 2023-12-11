import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';

@Component({
  selector: 'app-api-key-text',
  templateUrl: './api-key-text.component.html'
})
export class ApiKeyTextComponent implements OnInit {

  @Input() key!: string
  @Input() idName!: string
  @Input() name!: string
  @Input() supportUpdate = false
  @Input() supportDelete = false
  @Input() updatePending = false
  @Input() deletePending = false

  @Output() update = new EventEmitter<void>()
  @Output() delete = new EventEmitter<void>()

  Constants = Constants

  constructor(
    private dataService: DataService,
    private popupService: PopupService) { }

  ngOnInit(): void {
  }

  copy() {
    this.dataService.copyToClipboard(this.key).subscribe(() => {
      this.popupService.success('Value copied to clipboard.')
    })
  }

  doDelete() {
    this.delete.emit()
  }

  doUpdate() {
    this.update.emit()
  }

}
