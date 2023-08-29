import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-tag',
  templateUrl: './tag.component.html',
  styleUrls: [ './tag.component.less' ]
})
export class TagComponent implements OnInit {

  @Input() label?: string
  @Input() value?: string

  constructor() { }

  ngOnInit(): void {
  }

}
