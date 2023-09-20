import { Injectable } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal'
import { HtmlComponent } from '../modals/html/html.component'

@Injectable({
  providedIn: 'root'
})
export class HtmlService {

  constructor(
    private modalService: BsModalService
  ) { }

  showHtml(headerText: string, html: string, size?: string):void {
    if (size == undefined) {
      size = 'modal-lg'
    }
    const initialState = {
      headerText: headerText,
      html: html
    }
    this.modalService.show(HtmlComponent, {
      initialState,
      class: size
    })

  }

}
