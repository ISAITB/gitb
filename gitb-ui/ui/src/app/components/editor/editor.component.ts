import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-editor',
  template: '<editor [init]="editorConfig" [(ngModel)]="editorValue"></editor>',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EditorComponent),
      multi: true
    }
  ]
})
export class EditorComponent implements OnInit, ControlValueAccessor {

  @Input() type:'normal'|'minimal'|'pdf'|'line' = 'normal'
  @Input() height?: number
  editorConfig: any = {}
  _editorValue: string = ''
  onChange = (_: any) => {}
  onTouched = () => {}

  constructor(private dataService: DataService) {}

  set editorValue(value: string) {
    this._editorValue = value
    this.emitChanges()
  }

  get editorValue() {
    return this._editorValue
  }

  ngOnInit(): void {
    let heightToUse: number
    if (this.height == undefined) {
      if (this.type == 'line') {
        heightToUse = 150
      } else {
        heightToUse = 300
      }
    } else {
      heightToUse = this.height
    }
    let config:any = {
      height: heightToUse,
      menubar: false,
      branding: false,
      base_url: this.dataService.completePath('/assets/build/tinymce'),
      content_css: 'assets/build/styles.css',
      content_style: 'h1, h2, h3, h4, h5, h6 { color: #428bca; }',
      body_class: 'editor-body',
      suffix: '.min'
    }
    if (this.type == 'normal') {
      config.plugins = [
          'advlist autolink lists link image charmap print preview anchor',
          'searchreplace visualblocks code fullscreen',
          'insertdatetime media table paste code'
      ]
      config.toolbar = 'undo redo | insert | styleselect | bold italic | charmap | forecolor backcolor | fontsizeselect | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image | table tabledelete | tableprops tablerowprops tablecellprops | tableinsertrowbefore tableinsertrowafter tabledeleterow | tableinsertcolbefore tableinsertcolafter tabledeletecol'
    } else if (this.type == 'minimal') {
      config.plugins = [
        'advlist autolink lists link charmap print preview anchor',
        'searchreplace visualblocks code fullscreen',
        'insertdatetime media table paste code'
      ]
      config.toolbar = 'bold italic | charmap | bullist numlist outdent indent | link'
    } else if (this.type == 'pdf') {
      config.plugins = [
        'autolink lists link charmap anchor',
        'visualblocks code fullscreen',
        'paste code'
      ]
      config.toolbar = 'bold italic | charmap | bullist numlist | link'
    } else if (this.type == 'line') {
      config.plugins = [
        'autolink link charmap anchor',
        'visualblocks code fullscreen',
        'paste code'
      ]
      config.toolbar = 'bold italic | charmap | link'
    }
    this.editorConfig = config
  }

  emitChanges() {
    this.onChange(this._editorValue)
    this.onTouched()
  }

  writeValue(value: string): void {
    this._editorValue = value
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

}
