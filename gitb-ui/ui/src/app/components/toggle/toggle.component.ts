import { AfterViewInit, Component, ElementRef, EventEmitter, forwardRef, Input, Output, Renderer2, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-toggle',
  templateUrl: './toggle.component.html',
  styleUrls: ['./toggle.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ToggleComponent),
      multi: true
    }
  ]  
})
export class ToggleComponent implements AfterViewInit, ControlValueAccessor {

  @Input() on = ' '
  @Input() off = ' '
  @Input() width?: string
  @Input() height?: string
  @Input() toggleClass = ''
  @Input() size = ''
  @Input() disabled = false

  @Output('toggle') $toggle = new EventEmitter<boolean>()

  @ViewChild('wrapperElement') wrapperElement!: ElementRef
  @ViewChild('onElement') onElement!: ElementRef
  @ViewChild('offElement') offElement!: ElementRef
  @ViewChild('handleElement') handleElement!: ElementRef

  _value:boolean = false
  onChange = (_: any) => {}
  onTouched = () => {}
  wrapperStyle = {}
  rendered = false

  constructor(
    private renderer: Renderer2
  ) { }

  ngAfterViewInit(): void {
    this.rendered = true
    this.evaluateSize()
    this.renderer.addClass(this.onElement.nativeElement, 'toggle-on')
    this.renderer.addClass(this.offElement.nativeElement, 'toggle-off')    
    this.computeStyle()
    this.toggle()
  }
 
  evaluateSize() {
    // If width and height are already set, return immediately
    if (this.width !== undefined && this.height !== undefined) {
      return
    }
    // Calculate the proper width
    if (this.width === undefined) {
      this.width = (Math.max(
        this.onElement.nativeElement.scrollWidth,
        this.offElement.nativeElement.scrollWidth
      ) + 2 + 12) + 'px';
    }
    // Calculate the proper height
    if (this.height === undefined) {
      this.height = (Math.max(
        this.onElement.nativeElement.scrollHeight,
        this.offElement.nativeElement.scrollHeight
      ) + 2) + 'px';
    }
  }

  computeStyle() {
    let styleToSet: any = {}
    styleToSet.width = this.width
    styleToSet.height = this.height
    this.wrapperStyle = styleToSet
  }

  toggle() {
    if (this.rendered) {
      if (this._value) {
        this.renderer.removeClass(this.wrapperElement.nativeElement, 'off')
        this.renderer.removeClass(this.wrapperElement.nativeElement, 'btn-default')
        this.renderer.addClass(this.wrapperElement.nativeElement, 'btn-primary')
      } else {
        this.renderer.addClass(this.wrapperElement.nativeElement, 'off')
        this.renderer.addClass(this.wrapperElement.nativeElement, 'btn-default')
        this.renderer.removeClass(this.wrapperElement.nativeElement, 'btn-primary')
      }
    }
  }

  onSwitch(event: any) {
    if (this.disabled) {
      return false
    } else {
      this._value = !this._value
      this.toggle()      
      this.emitChanges()
      return true
    }
  }

  set toggleValue(value: boolean) {
    this._value = value
    this.emitChanges()
  }

  get toggleValue() {
    return this._value
  }

  emitChanges() {
    this.onChange(this._value)
    this.$toggle.emit(this._value)
    this.onTouched()
  }

  writeValue(val: boolean): void {
    let newValue = false
    if (val) {
      newValue = true
    }
    if (this._value != newValue) {
      this._value = newValue
      this.toggle()
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

}
