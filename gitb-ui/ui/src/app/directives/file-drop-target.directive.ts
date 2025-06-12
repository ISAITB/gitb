import { Directive, HostListener } from '@angular/core';
import { DragSupportService } from '../services/drag-support.service';

@Directive({
    selector: '[appFileDropTarget]',
    standalone: false
})
export class FileDropTargetDirective {

  dragEnterCounter = 0

  constructor(
    private dragSupport: DragSupportService
  ) {
    this.dragSupport.onDragDropChange$.subscribe(() => {
      this.dragEnterCounter = 0
    })
  }

  @HostListener('dragenter', ['$event'])
  onDragEnter(event: DragEvent) {
    event.preventDefault()
    this.dragEnterCounter += 1
    this.dragSupport.dragEnter()
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent) {
    event.preventDefault()
    this.dragEnterCounter -= 1
    if (this.dragEnterCounter == 0) {
      this.dragSupport.dragLeave()
    }
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent) {
    event.preventDefault()
    this.dragSupport.dragDrop()
  }

}
