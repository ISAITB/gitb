import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'app-filter-control',
  standalone: false,
  templateUrl: './filter-control.component.html'
})
export class FilterControlComponent {

  @Output() toggle = new EventEmitter<boolean>();
  @Output() refresh = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();
  visible = false

  doClear() {
    this.visible = false
    this.clear.emit();
  }

  doRefresh() {
    this.refresh.emit();
  }

  doToggle() {
    this.visible = !this.visible;
    this.toggle.emit(this.visible);
  }

}
