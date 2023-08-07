import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { SystemService } from 'src/app/services/system.service';
import { map } from 'lodash'
import { ConformanceStatementRepresentation } from './conformance-statement-representation';
import { ConformanceStatement } from 'src/app/types/conformance-statement';
import { RoutingService } from 'src/app/services/routing.service';

@Component({
  selector: 'app-conformance-statements',
  templateUrl: './conformance-statements.component.html',
  styleUrls: [ './conformance-statements.component.less' ]
})
export class ConformanceStatementsComponent implements OnInit {

  systemId!: number
  organisationId!: number
  conformanceStatementRepresentations: ConformanceStatementRepresentation[] = []
  dataStatus = {status: Constants.STATUS.FINISHED}
  showCreate = false
  showDomain = false
  columnCount = -1
  Constants = Constants

  constructor(
    private systemService: SystemService,
    public dataService: DataService,
    private route: ActivatedRoute,
    public routingService: RoutingService
  ) { }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
    this.organisationId = Number(this.route.snapshot.paramMap.get('org_id'))
    this.showCreate = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || (this.dataService.isVendorAdmin && this.dataService.community!.allowStatementManagement)
    this.showDomain = this.dataService.isSystemAdmin || this.dataService.community?.domainId == undefined
    this.columnCount = this.showDomain?6:5
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
        updateTime: conformanceStatement.updateTime,
        counters: { 
          completed: Number(conformanceStatement.results.completed), failed: Number(conformanceStatement.results.failed), other: Number(conformanceStatement.results.undefined),
          completedOptional: Number(conformanceStatement.results.completedOptional), failedOptional: Number(conformanceStatement.results.failedOptional), otherOptional: Number(conformanceStatement.results.undefinedOptional)
        },
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
    this.routingService.toConformanceStatement(this.organisationId, this.systemId, statement.actorId, statement.specificationId)
  }

}
