import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { FileData } from 'src/app/types/file-data.type';

@Component({
  selector: 'app-file-select',
  templateUrl: './file-select.component.html' 
})
export class FileSelectComponent implements OnInit {

  @Input() fileName?: string
  @Input() placeholder = 'Select file ...'
  @Input() label?: string
  @Input() accepts?: string[]
  @Input() maxSize!: number
  @Output() onUpload: EventEmitter<FileData> = new EventEmitter()
  @ViewChild('fileInput') fileInput?: ElementRef

  isButton = false
  maxSizeKbs?: number
  acceptString?: string

  constructor(
    private dataService: DataService,
    private errorService: ErrorService
  ) { }

  ngOnInit(): void {
    this.isButton = this.label != undefined
    if (this.accepts != undefined && this.accepts.length > 0) {
      this.acceptString = this.accepts.join(',')
    }
    if (this.maxSize == undefined) {
      this.maxSizeKbs = this.dataService.configuration.savedFileMaxSize
      this.maxSize = Number(this.dataService.configuration.savedFileMaxSize) * 1024
    }
  }

  onFileChange() {
    const files: { [key: string]: File} = this.fileInput?.nativeElement.files
    const file = files[0]
    if (this.maxSize > 0 && file.size >= this.maxSize) {
      this.errorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for files is '+this.maxSizeKbs+' KBs.')
    } else {
      this.onUpload.emit({
        name: file.name,
        size: file.size,
        type: file.type,
        file: file
      })
    }
  }

  onButtonClick(): void {
    this.fileInput!.nativeElement.click()
  }

}
