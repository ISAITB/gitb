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
      min_height: 100,
      menubar: false,
      branding: false,
      base_url: this.dataService.completePath('/assets/build/tinymce'),
      cache_suffix: this.dataService.configuration.versionNumber,
      content_css: 'assets/build/styles.css,api/theme/css',
      body_class: 'editor-body',
      suffix: '.min',
      convert_unsafe_embeds: true      
    }
    if (this.type == 'normal') {
      config.plugins = 'advlist autolink lists link image charmap preview anchor searchreplace visualblocks code fullscreen insertdatetime media table code'
      config.toolbar = 'undo redo | insert | styles | bold italic | charmap | forecolor backcolor | fontsize | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image | table tabledelete | tableprops tablerowprops tablecellprops | tableinsertrowbefore tableinsertrowafter tabledeleterow | tableinsertcolbefore tableinsertcolafter tabledeletecol'
      config.table_appearance_options = true
    } else if (this.type == 'minimal') {
      config.plugins = 'advlist autolink lists link charmap preview anchor searchreplace visualblocks code fullscreen insertdatetime media table code'
      config.toolbar = 'bold italic | charmap | bullist numlist outdent indent | link'
    } else if (this.type == 'pdf') {
      // Same as normal
      config.plugins = 'advlist autolink lists link image charmap preview anchor searchreplace visualblocks code fullscreen insertdatetime media table code'
      config.toolbar = 'undo redo | insert | styles | bold italic | charmap | forecolor backcolor | fontsize | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image | table tabledelete | tableprops tablerowprops tablecellprops | tableinsertrowbefore tableinsertrowafter tabledeleterow | tableinsertcolbefore tableinsertcolafter tabledeletecol'
      config.table_appearance_options = true
    } else if (this.type == 'line') {
      config.plugins =  'autolink link charmap anchor visualblocks code fullscreen code'
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
