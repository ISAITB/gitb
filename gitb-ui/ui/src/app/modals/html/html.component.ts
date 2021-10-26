import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BsModalRef } from 'ngx-bootstrap/modal'

@Component({
  selector: 'app-html',
  templateUrl: './html.component.html',
  styles: [
  ]
})
export class HtmlComponent implements OnInit {

  headerText: string = ''
  html: string = ''

  constructor(
    public modalRef: BsModalRef,
    public sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
  }

  close() {
    this.modalRef.hide()
  }

}
