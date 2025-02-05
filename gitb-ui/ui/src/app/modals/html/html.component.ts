import { Component, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal'

@Component({
    selector: 'app-html',
    templateUrl: './html.component.html',
    styles: [],
    standalone: false
})
export class HtmlComponent implements OnInit {

  headerText?: string
  html: string = ''
  customClass?: string

  constructor(
    public modalRef: BsModalRef
  ) { }

  ngOnInit(): void {
    if (this.customClass) {
      this.modalRef.setClass(this.customClass)
    }
  }

  close() {
    this.modalRef.hide()
  }

}
