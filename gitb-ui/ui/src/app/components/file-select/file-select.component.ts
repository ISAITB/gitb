import { Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output, ViewChild } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { DragSupportService } from 'src/app/services/drag-support.service';
import { PopupService } from 'src/app/services/popup.service';
import { FileData } from 'src/app/types/file-data.type';

@Component({
  selector: 'app-file-select',
  templateUrl: './file-select.component.html',
  styleUrls: [ './file-select.component.less' ]
})
export class FileSelectComponent implements OnInit {

  @Input() fileName?: string
  @Input() placeholder = 'Drop or browse for file ...'
  @Input() accepts?: string[]
  @Input() acceptsDrop?: string[]
  @Input() maxSize!: number
  @Input() extraActions = false
  @Input() disableUpload = false
  @Output() onUpload: EventEmitter<FileData> = new EventEmitter()
  @ViewChild('fileInput') fileInput?: ElementRef

  Constants = Constants
  
  maxSizeKbs?: number
  acceptString?: string
  dragActive = false
  dropActive = false

  constructor(
    private dataService: DataService,
    private dragSupport: DragSupportService,
    private popupService: PopupService
  ) { }

  ngOnInit(): void {
    if (this.accepts != undefined && this.accepts.length > 0) {
      this.acceptString = this.accepts.join(',')
    }
    if (this.acceptsDrop == undefined) {
      this.acceptsDrop = this.accepts
    }
    if (this.maxSize == undefined) {
      this.maxSizeKbs = this.dataService.configuration.savedFileMaxSize
      this.maxSize = Number(this.dataService.configuration.savedFileMaxSize) * 1024
    }
    this.dragSupport.onDragStartChange$.subscribe(() => {
      if (!this.disableUpload) {
        this.dragActive = true
      }
    })
    this.dragSupport.onDragLeaveChange$.subscribe(() => {
      if (!this.disableUpload) {
        this.dragActive = false
      }
    })
    this.dragSupport.onDragDropChange$.subscribe(() => {
      if (!this.disableUpload) {
        this.dragActive = false
      }
    })    
  }

  onFileChange() {
    const files: { [key: string]: File} = this.fileInput?.nativeElement.files
    this.selectFile(files[0])
  }

  private selectFile(file: File) {
    if (file != undefined) {
      if (this.maxSize > 0 && file.size >= this.maxSize) {
        this.popupService.warning('The maximum allowed size for files is '+this.maxSizeKbs+' KBs.')
      } else {
        this.onUpload.emit({
          name: file.name,
          size: file.size,
          type: file.type,
          file: file
        })
      }
    }
  }

  onButtonClick(): void {
    if (!this.disableUpload) {
      this.fileInput!.nativeElement.click()
    }
  }

  @HostListener('dragover', ['$event'])
  onDragOver(event: DragEvent) {
    if (!this.disableUpload) {
      event.preventDefault()
      this.dropActive = true
    }
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent) {
    if (!this.disableUpload) {
      event.preventDefault()
      this.dropActive = false
    }
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent) {
    if (!this.disableUpload) {
      event.preventDefault()
      event.stopPropagation()
      this.dropActive = false
      this.dragSupport.dragDrop()
      if (event.dataTransfer && event.dataTransfer.files) {
        if (event.dataTransfer.files.length > 0) {
          if (event.dataTransfer.files.length == 1) {
            const file = event.dataTransfer.files.item(0)
            if (file) {
              if (this.acceptsDrop && this.acceptsDrop.length > 0) {
                if (this.acceptsDrop.includes(file.type)) {
                  this.selectFile(file)
                } else {
                  this.popupService.warning("File is not of the expected type.")
                }
              } else {
                this.selectFile(file)
              }
            }
          } else {
            this.popupService.warning("Only a single file can be selected.")
          }
        }
      }
    }
  }

}
