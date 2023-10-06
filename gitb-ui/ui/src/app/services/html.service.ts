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

  showHtml(headerText: string|undefined, html: string, size?: string, customClass?: string):void {
    if (size == undefined) {
      size = 'modal-lg'
    }
    const initialState = {
      headerText: headerText,
      html: html,
      customClass: customClass
    }
    this.modalService.show(HtmlComponent, {
      initialState,
      class: size,
    })
  }

}
