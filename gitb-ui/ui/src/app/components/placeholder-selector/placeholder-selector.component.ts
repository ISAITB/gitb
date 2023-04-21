import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { KeyValue } from 'src/app/types/key-value';

@Component({
  selector: 'app-placeholder-selector',
  templateUrl: './placeholder-selector.component.html',
  styleUrls: [ './placeholder-selector.component.less' ]
})
export class PlaceholderSelectorComponent implements OnInit {

  @Input() placeholders: KeyValue[] = []

  constructor(
    private dataService: DataService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
  }

  selected(placeholder: KeyValue) {
    this.dataService.copyToClipboard(placeholder.key).subscribe(() => {
      this.popupService.success('Placeholder "<b>'+placeholder.key+'</b>" copied to clipboard.')
    })
  }

}
