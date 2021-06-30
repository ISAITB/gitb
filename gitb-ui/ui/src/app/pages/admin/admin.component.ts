import { Component, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styles: [
  ]
})
export class AdminComponent implements OnInit {

  public communityId?: number

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    this.communityId = this.dataService.community?.id
  }

}
