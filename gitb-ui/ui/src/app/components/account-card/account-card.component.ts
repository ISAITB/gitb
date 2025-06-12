import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import { UserAccount } from 'src/app/types/user-account';
import {DataService} from '../../services/data.service';
import {Constants} from '../../common/constants';

@Component({
  selector: 'app-account-card',
  standalone: false,
  templateUrl: './account-card.component.html',
  styleUrl: './account-card.component.less'
})
export class AccountCardComponent implements OnInit {

  @Input() account!: UserAccount;
  @Input() selected = false
  @Input() first = false
  @Output() select = new EventEmitter<UserAccount>();

  accountDescription!: string
  Constants = Constants

  constructor(private dataService: DataService) {
  }

  ngOnInit(): void {
    this.accountDescription = this.dataService.getRoleDescription(true, this.account)
  }

  cardClicked(): void {
    this.select.emit(this.account)
  }

}
