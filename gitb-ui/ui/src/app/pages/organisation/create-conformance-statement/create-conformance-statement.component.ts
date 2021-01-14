import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { SystemService } from 'src/app/services/system.service';
import { Actor } from 'src/app/types/actor';
import { Community } from 'src/app/types/community';
import { Domain } from 'src/app/types/domain';
import { Specification } from 'src/app/types/specification';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { remove, find } from 'lodash'
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from '../../base-component.component';

@Component({
  selector: 'app-create-conformance-statement',
  templateUrl: './create-conformance-statement.component.html',
  styles: [
  ]
})
export class CreateConformanceStatementComponent extends BaseComponent implements OnInit {

  domains: Domain[] = []
  specs: Specification[] = []
  actors: Actor[] = []
  systemId!: number
  selectedDomain?: Domain
  selectedSpec?: Specification
  selectedActor?: Actor
  showDomains = false
  showSpecs = false
  showActors = false
  showButtonPanel = false
  showConfirmation = false
  confirmDisabled = false
  tableColumns: TableColumnDefinition[] = [
    { field: 'sname', title: 'Short name' },
    { field: 'fname', title: 'Full name' },
    { field: 'description', title: 'Description' }
  ]
  actorTableColumns = [ 
    { field: 'actorId', title: 'ID' },
    { field: 'name', title: 'Name' },
    { field: 'description', title: 'Description' }
  ]
  domainId: number[] = []
  pending = false
  headerText = ''
  confirmAction?: () => void
  domainsDataStatus = {status: Constants.STATUS.PENDING}
  specsDataStatus = {status: Constants.STATUS.PENDING}
  actorsDataStatus = {status: Constants.STATUS.PENDING}

  constructor(
    private systemService: SystemService,
    public dataService: DataService,
    private popupService: PopupService,
    private errorService: ErrorService,
    private route: ActivatedRoute,
    private router: Router,
    private conformanceService: ConformanceService
  ) { super() }

  ngOnInit(): void {
    const community: Community = JSON.parse(localStorage.getItem('community')!)
    if (community.domainId != undefined) {
      this.domainId = [community.domainId]
    }
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
    this.getDomains()
  }

  finish() {
    this.pending = true
    this.clearAlerts()
    this.systemService.defineConformanceStatement(this.systemId, this.selectedSpec!.id, this.selectedActor!.id)
    .subscribe((data) => {
      if (data != undefined && data.error_description != undefined) {
        this.addAlertError(data.error_description)
        this.confirmDisabled = true
      } else {
        this.router.navigate(['organisation', 'systems', this.systemId, 'conformance'])
        this.popupService.success("Conformance statement created.")
      }
    }).add(() => {
      this.pending = false
    })
  }

  onDomainSelect(domain: Domain) {
    this.selectedDomain = domain
  }

  onSpecificationSelect(spec: Specification) {
    this.selectedSpec = spec
  }

  onActorSelect(actor: Actor) {
    this.selectedActor = actor
  }

  setDomainsView() {
    this.showDomains = true
    this.confirmAction = (() => { this.confirmDomain() }).bind(this)
    this.headerText = 'Select ' + this.dataService.labelDomainLower()
    this.showButtonPanel = true
  }

  setSpecsView() {
    this.showSpecs = true
    this.confirmAction = (() => { this.confirmSpec() }).bind(this)
    this.headerText = 'Select ' + this.dataService.labelSpecificationLower()
    this.showButtonPanel = true
  }

  setActorsView() {
    this.showActors = true
    this.confirmAction = (() => { this.confirmActor() }).bind(this)
    this.headerText = 'Select ' + this.dataService.labelActorLower()
    this.showButtonPanel = true
  }

  setConfirmationView() {
    this.showConfirmation = true
    this.confirmAction = (() => { this.confirmAll() }).bind(this)
    this.headerText = 'Summary'
    this.showButtonPanel = true
  }

  nextDisabled() {
    if (this.showDomains) {
      return this.selectedDomain == undefined
    } else if (this.showSpecs) {
      return this.selectedSpec == undefined
    } else if (this.showActors) {
      return this.selectedActor == undefined
    } else {
      return false
    }
  }

  getDomains() {
    this.domains = []
    this.conformanceService.getDomains(this.domainId)
    .subscribe((data) => {
      this.domains = data
      if (this.domains != undefined && this.domains.length == 1) {
        this.onDomainSelect(this.domains[0])
        this.confirmDomain()
      } else {
         this.setDomainsView()
      }
    }).add(() => {
      this.domainsDataStatus.status = Constants.STATUS.FINISHED
    })
  }

  getSpecs(domainId: number) {
    this.specs  = []
    this.conformanceService.getSpecifications(domainId)
    .subscribe((data) => {
      if (data != undefined) {
        remove(data, (spec) =>
          spec.hidden == true
        )          
        this.specs = data
        if (data.length == 1) {
          this.onSpecificationSelect(this.specs[0])
          this.confirmSpec()
        } else {
          this.setSpecsView()
        }
      }
    }).add(() => {
      this.specsDataStatus.status = Constants.STATUS.FINISHED
    })
  }

  getActors(specId: number) {
    this.actors = []
    this.conformanceService.getActorsWithSpecificationId(specId)
    .subscribe((data) => {
      if (data != undefined) {
        remove(data, (actor) =>
          actor.hidden == true
        )
        if (data.length == 1) {
          this.actors = data
          this.onActorSelect(this.actors[0])
          this.confirmActor()
        } else if (data.length > 1) {
          const defaultActor = find(data, (actor) =>
            actor.default == true
          )
          if (defaultActor != undefined) {
            this.onActorSelect(defaultActor)
            this.confirmActor()
          } else {
            this.actors = data
            this.setActorsView()
          }
        } else {
          this.setActorsView()
        }
      }
    }).add(() => {
      this.actorsDataStatus.status = Constants.STATUS.FINISHED
    })
  }

  confirmDomain() {
    this.showDomains = false
    this.getSpecs(this.selectedDomain!.id)
  }

  confirmSpec() {
    this.showSpecs = false
    this.getActors(this.selectedSpec!.id)
  }

  confirmActor() {
    this.showActors = false
    this.setConfirmationView()
  }

  confirmAll() {
    this.finish()
  }

  cancel() {
    this.router.navigate(['organisation', 'systems', this.systemId, 'conformance'])
  }

}
