import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-output-message-display',
  standalone: false,
  templateUrl: './output-message-display.component.html',
  styleUrl: './output-message-display.component.less'
})
export class OutputMessageDisplayComponent implements OnInit {

  @Input() message!: string
  @Input() messageType!: string
  lines!: string[]

  ngOnInit() {
    this.lines = this.message.split('\n')
  }

}
