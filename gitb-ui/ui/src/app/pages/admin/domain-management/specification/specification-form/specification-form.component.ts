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

import {Component, Input, OnInit} from '@angular/core';
import {DataService} from 'src/app/services/data.service';
import {Specification} from 'src/app/types/specification';
import {MultiSelectConfig} from '../../../../../components/multi-select-filter/multi-select-config';
import {SpecificationGroup} from '../../../../../types/specification-group';
import {of} from 'rxjs';
import {FilterUpdate} from '../../../../../components/test-filter/filter-update';

@Component({
    selector: 'app-specification-form',
    templateUrl: './specification-form.component.html',
    styles: [],
    standalone: false
})
export class SpecificationFormComponent implements OnInit {

  @Input() specification!: Partial<Specification>

  groupSelectionConfig!: MultiSelectConfig<SpecificationGroup>

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    this.groupSelectionConfig = {
      name: "groupChoice",
      singleSelection: true,
      singleSelectionClearable: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      textField: "fname",
      filterLabel: "-- Optional --",
      loader: () => of(this.specification.groups!)
    }
  }

  groupChanged(event: FilterUpdate<SpecificationGroup>) {
    if (event.values.active.length == 0) {
      this.specification.group = undefined
    } else {
      this.specification.group = event.values.active[0].id
    }
  }

}
