import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DragSupportService {

  private dragEnterSource = new Subject<void>()
  public onDragStartChange$ = this.dragEnterSource.asObservable()
  private dragLeaveSource = new Subject<void>()
  public onDragLeaveChange$ = this.dragLeaveSource.asObservable()
  private dragDropSource = new Subject<void>()
  public onDragDropChange$ = this.dragDropSource.asObservable()

  constructor() { }

  dragEnter() {
    this.dragEnterSource.next()
  }

  dragLeave() {
    this.dragLeaveSource.next()
  }
  
  dragDrop() {
    this.dragDropSource.next()
  }  
}
