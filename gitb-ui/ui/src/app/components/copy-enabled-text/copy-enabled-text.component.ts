import { Component, HostListener, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';

@Component({
  selector: 'app-copy-enabled-text',
  templateUrl: './copy-enabled-text.component.html',
  styleUrls: [ './copy-enabled-text.component.less' ]
})
export class CopyEnabledTextComponent implements OnInit {

  @Input() value: string|undefined
  hovering = false

  constructor(
    private dataService: DataService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
  }

  @HostListener('mouseenter')
  onMouseEnter() {
    this.hovering = true
  }

  @HostListener('mouseleave')
  onMouseLeave() {
    this.hovering = false
  }

  copy() {
    this.dataService.copyToClipboard(this.value).subscribe(() => {
      this.popupService.success('Value copied to clipboard.')
    })
  }
}