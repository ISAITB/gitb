import { Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';

@Component({
  selector: 'app-test-option-button',
  templateUrl: './test-option-button.component.html',
  styleUrls: ['./test-option-button.component.less']
})
export class TestOptionButtonComponent {

  @Input() showCompletedValue = false
  @Input() showPendingValue = false
  @Input() startAutomaticallyValue = false
  @Output() showCompleted = new EventEmitter<boolean>()
  @Output() showPending = new EventEmitter<boolean>()
  @Output() startAutomatically = new EventEmitter<boolean>()
  optionFormVisible = false
  open = false

  constructor(private eRef: ElementRef) { }

  buttonClicked() {
    this.optionFormVisible = !this.optionFormVisible
    this.open = !this.open
  }

  @HostListener('document:click', ['$event'])
  clickRegistered(event: any) {
    if (!this.eRef.nativeElement.contains(event.target) && this.optionFormVisible) {
      this.buttonClicked()
    }
  }

  @HostListener('document:keyup.escape', ['$event'])  
  escapeRegistered(event: KeyboardEvent) {
    if (this.optionFormVisible) {
      this.buttonClicked()
    }
  }

}
