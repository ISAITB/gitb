import { Component, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';

@Component({
  selector: 'app-hidden-icon',
  templateUrl: './hidden-icon.component.html',
  styleUrls: [ './hidden-icon.component.less' ]
})
export class HiddenIconComponent implements OnInit {

  Constants = Constants

  constructor() { }

  ngOnInit(): void {
  }

}
