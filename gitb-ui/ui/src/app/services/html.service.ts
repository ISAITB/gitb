import { Injectable } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { BsModalService } from 'ngx-bootstrap/modal'
import { HtmlComponent } from '../modals/html/html.component'

@Injectable({
  providedIn: 'root'
})
export class HtmlService {

  constructor(
    private modalService: BsModalService
  ) { }

  showHtml(headerText: string, html: string):void {
    const initialState = {
      headerText: headerText,
      html: html
    }
    this.modalService.show(HtmlComponent, {
      initialState,
      class: 'modal-lg'
    })

  }

}
