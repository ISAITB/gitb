import { Component, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { Organisation } from 'src/app/types/organisation.type';

@Component({
  selector: 'app-organisation-index',
  templateUrl: './organisation-index.component.html',
  styles: [
  ]
})
export class OrganisationIndexComponent implements OnInit {

  organisation!: Organisation
  
  constructor(public dataService: DataService) { }

  ngOnInit(): void {
    this.organisation = JSON.parse(localStorage.getItem(Constants.LOCAL_DATA.ORGANISATION)!)
  }

}
