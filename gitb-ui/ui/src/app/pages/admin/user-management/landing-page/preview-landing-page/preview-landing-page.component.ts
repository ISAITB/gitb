import { Component, Input, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';

@Component({
  selector: 'app-preview-landing-page',
  templateUrl: './preview-landing-page.component.html'
})
export class PreviewLandingPageComponent implements OnInit {

  @Input() previewContent!: string

  constructor(
    private modalRef: BsModalRef
  ) { }

  ngOnInit(): void {
    this.modalRef.setClass('landingPagePreview')
  }

  closePreview() {
    this.modalRef.hide()
  }

}
