import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';
import { Organisation } from 'src/app/types/organisation.type';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { EditableSystem } from './editable-system';
import { find } from 'lodash';
import { BsModalService } from 'ngx-bootstrap/modal';
import { CreateEditSystemModalComponent } from 'src/app/modals/create-edit-system-modal/create-edit-system-modal.component';
import { System } from 'src/app/types/system';

@Component({
  selector: 'app-system-list',
  templateUrl: './system-list.component.html',
  styles: [
  ]
})
export class SystemListComponent implements OnInit {

  systems: EditableSystem[] = []
  organisation!: Organisation
  editing = false
  showAction = false
  dataStatus = {status: Constants.STATUS.FINISHED}
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description' },
    { field: 'version', title: 'Version' }
  ]
  systemIdToEdit?: number
  viewProperties = false

  constructor(
    public dataService: DataService,
    private systemService: SystemService,
    private route: ActivatedRoute,
    private router: Router,
    private modalService: BsModalService
  ) {}

  ngOnInit(): void {
    this.organisation = JSON.parse(localStorage.getItem(Constants.LOCAL_DATA.ORGANISATION)!)
    const systemIdToEditParam = this.route.snapshot.queryParamMap.get('id')
    if (systemIdToEditParam != undefined) {
      this.systemIdToEdit = Number(systemIdToEditParam)
    }
    const viewPropertiesParam = this.route.snapshot.queryParamMap.get('viewProperties')
    if (viewPropertiesParam != undefined) {
      this.viewProperties = Boolean(viewPropertiesParam)
    }
    this.showAction = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowPostTestSystemUpdates)

    if (this.route.snapshot.data?.systems != undefined) {
      this.processSystems(this.route.snapshot.data.systems, true)
    } else {
      this.getSystems(true)
    }
  }

  private processSystems(systems: System[], initialLoad: boolean) {
    this.systems = systems
    for (let system of this.systems) {
      system.editable = false
      if (this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin) {
        system.editable = true
        this.showAction = true
      } else if (this.dataService.isVendorAdmin) {
        if (this.dataService.community!.allowPostTestSystemUpdates) {
          system.editable = true
          this.showAction = true
        } else if (!system.hasTests) {
          system.editable = true
          this.showAction = true
        }
      }
    }
    if (initialLoad && this.systemIdToEdit != undefined) {
      const systemToEdit = find(this.systems, (s) => {
        return s.id == this.systemIdToEdit
      })
      if (systemToEdit != undefined) {
        this.onSystemEdit(systemToEdit)
      }
    }
  }

	getSystems(initialLoad: boolean) {
		const checkIfHasTests = this.dataService.isVendorAdmin && !this.dataService.community!.allowPostTestSystemUpdates
    this.dataStatus.status = Constants.STATUS.PENDING
		this.systemService.getSystemsByOrganisation(this.organisation.id, checkIfHasTests)
    .subscribe((data) => {
      this.processSystems(data, initialLoad)
    }).add(() => {
      this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

	createSystem() {
    const modal = this.modalService.show(CreateEditSystemModalComponent, {
      initialState: {
        organisationId: this.organisation.id,
        communityId: this.organisation.community
      },
      class: 'modal-lg'
    })
    modal.content!.reload.subscribe(() => {
      this.getSystems(false)
    })
  }

	onSystemSelect(system: EditableSystem) {
		if (!this.editing) {
      this.router.navigate(['organisation', 'systems', system.id, 'conformance'])
    }
  }

	onSystemEdit(system: EditableSystem) {
		if (this.isSystemEditable(system)) {
			this.editing = true
      const modal = this.modalService.show(CreateEditSystemModalComponent, {
        initialState: {
          system: system,
          organisationId: this.organisation.id,
          communityId: this.organisation.community,
          viewProperties: this.viewProperties
        },
        class: 'modal-lg'
      })
      modal.content!.reload.subscribe(() => {
        this.getSystems(false)
      })
      modal.onHidden.subscribe(() => {
        this.editing = false
      })
    }
  }

	showBack() {
		return this.organisation != undefined && this.dataService.vendor != undefined && this.organisation.id != this.dataService.vendor.id
  }

	isSystemEditable(system: EditableSystem|undefined) {
		return system != undefined && system.editable!
  }

	showCreate() {
		return this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowSystemManagement)
  }

	back() {
    this.router.navigate(['admin', 'users', 'community', this.organisation.community, 'organisation', this.organisation.id])
  }

}
