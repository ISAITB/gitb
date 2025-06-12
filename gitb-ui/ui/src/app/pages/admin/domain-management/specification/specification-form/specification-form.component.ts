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
