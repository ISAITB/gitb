import { Directive, ElementRef, OnInit, Renderer2 } from '@angular/core';

@Directive({
    selector: 'input[type=text]',
    standalone: false
})
export class NoAutoCompleteDirective implements OnInit {

  constructor(private renderer: Renderer2, private element: ElementRef) { }

  ngOnInit(): void {
    this.renderer.setAttribute(this.element.nativeElement, 'autocomplete', 'off')
  }

}
