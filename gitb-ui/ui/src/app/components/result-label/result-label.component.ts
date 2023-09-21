import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-result-label',
  templateUrl: './result-label.component.html',
  styleUrls: [ './result-label.component.less' ]
})
export class ResultLabelComponent implements OnInit {

  @Input() status!: string

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

}
