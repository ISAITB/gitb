import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { Specification } from 'src/app/types/specification';

@Component({
  selector: 'app-specification-form',
  templateUrl: './specification-form.component.html',
  styles: [
  ]
})
export class SpecificationFormComponent implements OnInit {

  @Input() specification!: Partial<Specification>

  constructor(
    public dataService: DataService,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
  }

}
