import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';
import { TableColumnDefinition } from 'src/app/types/table-column-definition.type';
import { map } from 'lodash'
import { ConformanceStatementRepresentation } from './conformance-statement-representation';
import { ConformanceStatement } from 'src/app/types/conformance-statement';

@Component({
  selector: 'app-conformance-statements',
  templateUrl: './conformance-statements.component.html',
  styles: [
  ]
})
export class ConformanceStatementsComponent implements OnInit {

  systemId!: number
  conformanceStatementRepresentations: ConformanceStatementRepresentation[] = []
  dataStatus = {status: Constants.STATUS.FINISHED}
  tableColumns!: TableColumnDefinition[]
  showCreate = false

  constructor(
    private systemService: SystemService,
    private dataService: DataService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
    this.showCreate = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowStatementManagement)
    this.tableColumns = [
      { field: 'domainFull', title: this.dataService.labelDomain() },
      { field: 'specificationFull', title: this.dataService.labelSpecification() },
      { field: 'actorFull', title: this.dataService.labelActor() },
      { field: 'results', title: 'Test results' },
      { field: 'status', title: 'Status', iconFn: this.dataService.iconForTestResult }
    ]
    this.getConformanceStatements()
  }

  private processStatements(statements: ConformanceStatement[]) {
    this.conformanceStatementRepresentations = map(statements, (conformanceStatement) => {
      return {
        actorId: conformanceStatement.actorId,
        actor: conformanceStatement.actor,
        actorFull: conformanceStatement.actorFull,
        specificationId: conformanceStatement.specificationId,
        specification: conformanceStatement.specification,
        specificationFull: conformanceStatement.specificationFull,
        domainId: conformanceStatement.domainId,
        domain: conformanceStatement.domain,
        domainFull: conformanceStatement.domainFull,
        results: this.dataService.testStatusText(Number(conformanceStatement.results.completed), Number(conformanceStatement.results.failed), Number(conformanceStatement.results.undefined)),
        status: this.dataService.conformanceStatusForTests(Number(conformanceStatement.results.completed), Number(conformanceStatement.results.failed), Number(conformanceStatement.results.undefined))
      }
    })
  }

  getConformanceStatements() {
    if (this.route.snapshot.data?.statements != undefined) {
      this.processStatements(this.route.snapshot.data.statements)
    } else {
      this.dataStatus.status = Constants.STATUS.PENDING
      this.systemService.getConformanceStatements(this.systemId)
      .subscribe((data) => {
        this.processStatements(data)
      }).add(() => {
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  onConformanceStatementSelect(statement: ConformanceStatementRepresentation) {
    this.router.navigate(['organisation', 'systems', this.systemId, 'conformance', 'detail', statement.actorId, statement.specificationId])
  }

}
