import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { Domain } from 'src/app/types/domain';

@Component({
  selector: 'app-domain-form',
  templateUrl: './domain-form.component.html',
  styles: [
  ]
})
export class DomainFormComponent implements OnInit {

  @Input() domain!: Partial<Domain>

  constructor(public dataService: DataService) { }

  ngOnInit(): void {
  }

}
