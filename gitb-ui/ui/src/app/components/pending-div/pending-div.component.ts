import { AfterViewInit, Component, Input } from '@angular/core';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'div[pending]',
  templateUrl: './pending-div.component.html',
  styleUrls: ['./pending-div.component.less']
})
export class PendingDivComponent implements AfterViewInit {

  _pending = false
  private visible = false
  private _focus?: string

  @Input() set pending(value: boolean|undefined) {
    this._pending = value == true
    this.applyFocus()
  }
  @Input() set focus(value:string|undefined) {
    this._focus = value
    this.applyFocus()
  }

  constructor(private dataService: DataService) {}

  ngAfterViewInit(): void {
    this.visible = true
    this.applyFocus()
  }

  private applyFocus() {
    if (!this._pending && this._focus && this.visible) {
      this.dataService.focus(this._focus)
    }
  }

}
