import { Component, Input, OnInit } from '@angular/core';
import { PreviewByIds } from './preview-by-ids';
import { PreviewByFile } from './preview-by-file';
import { PreviewForStatus } from './preview-for-status';
import { ConformanceService } from 'src/app/services/conformance.service';
import { Observable, of } from 'rxjs';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-preview-badge-modal',
  templateUrl: './preview-badge-modal.component.html',
  styleUrls: [ './preview-badge-modal.component.less']
})
export class PreviewBadgeModalComponent implements OnInit {

  @Input() config!: PreviewByIds|PreviewForStatus|PreviewByFile

  headerText: string = ''
  html?: string
  
  constructor(
    private conformanceService: ConformanceService,
    private modalRef: BsModalRef,
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.headerText = "Badge preview"
    this.modalRef.setClass("modal-m")
    this.getBadgeBlob().subscribe((data) => {
      if (data) {
        const reader = new FileReader();
        reader.onload = () => {
          const pathForBadge = reader.result as string
          this.html = "<div class='badgePreviewContainer'><img class='badgePreview' src='"+pathForBadge+"'></div>"
        }
        reader.readAsDataURL(data)
      }
    })
  }

  close() {
    this.modalRef.hide()
  }

  private isPreviewForStatus(config: PreviewByIds|PreviewForStatus|PreviewByFile): config is PreviewForStatus {
    return (config as PreviewForStatus).status !== undefined
  }

  private isPreviewByFile(config: PreviewByIds|PreviewForStatus|PreviewByFile): config is PreviewByFile {
    return (config as PreviewByFile).badgeFile !== undefined
  }

  private getBadgeBlob(): Observable<Blob|undefined> {
    if (this.isPreviewByFile(this.config)) {
      return of(this.config.badgeFile.file?.file!)
    } else if (this.isPreviewForStatus(this.config)) {
      return this.dataService.binaryResponseToBlob(this.conformanceService.getBadgeForStatus(this.config.specificationId, this.config.actorId, this.config.status))
    } else { // PreviewByIds
      return this.dataService.binaryResponseToBlob(this.conformanceService.conformanceBadgeByIds(this.config.systemId, this.config.actorId, this.config.snapshotId))
    }
  }

}
