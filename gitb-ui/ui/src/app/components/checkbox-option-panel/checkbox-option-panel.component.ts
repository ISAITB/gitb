import { Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import { CheckboxOptionState } from './checkbox-option-state';
import { CheckboxOption } from './checkbox-option';

@Component({
  selector: 'app-checkbox-option-panel',
  templateUrl: './checkbox-option-panel.component.html',
  styleUrls: [ './checkbox-option-panel.component.less' ]
})
export class CheckboxOptionPanelComponent implements OnInit {

  // If multiple arrays are provided they are displayed with a separator between them.
  @Input() options!: CheckboxOption[][]
  @Input() label!: string
  @Input() refresh?: EventEmitter<CheckboxOption[][]>
  @Output() updated = new EventEmitter<CheckboxOptionState>()
  currentState!: CheckboxOptionState
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

  ngOnInit(): void {
    this.currentState = {}    
    this.applyConfig()
    if (this.refresh) {
      this.refresh.subscribe((newConfig) => {
        this.options = newConfig
        this.applyConfig()
      })
    }
  }

  private applyConfig() {
    for (let optionSet of this.options) {
      for (let option of optionSet) {
        this.currentState[option.key] = option.default
      }
    }    
  }
}
