/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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

  constructor(private readonly dataService: DataService) {
  }

  ngOnInit(): void {
    this.accountDescription = this.dataService.getRoleDescription(true, this.account)
  }

  cardClicked(): void {
    this.select.emit(this.account)
  }

}
