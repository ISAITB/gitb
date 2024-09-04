import { Component, ElementRef, ViewChild } from '@angular/core';
import { BaseReportSettingsFormComponent } from '../base-report-settings-form.component';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { Observable, map, of, share } from 'rxjs';
import { CodeEditorModalComponent } from 'src/app/components/code-editor-modal/code-editor-modal.component';
import { ConformanceService } from 'src/app/services/conformance.service';
import { PreviewOption } from './preview-option';
import { PreviewConfig } from './preview-config';
import { ErrorService } from 'src/app/services/error.service';

@Component({ template: '' })
export abstract class CommunityXmlReportFormComponent extends BaseReportSettingsFormComponent {

  @ViewChild("serviceField") serviceField?: ElementRef;

  config!: PreviewConfig

  idValueSign!: string
  idValueStylesheet!: string
  idValueCustomPdf!: string
  idValueCustomPdfSignature!: string
  idValueCustomPdfWithCustomXml!: string

  useStylesheet = false

  updatePending = false
  previewPending = false
  animated = false

  constructor(
    conformanceService: ConformanceService,
    reportService: ReportService,
    private popupService: PopupService,
    modalService: BsModalService,
    private confirmationDialogService: ConfirmationDialogService,
    errorService: ErrorService
  ) { super(conformanceService, modalService, reportService, errorService) }

  ngOnInit(): void {
    super.ngOnInit()
    this.config = this.getPreviewConfig()
    this.idValueSign = this.config.baseIdValue + 'Sign'
    this.idValueCustomPdf = this.config.baseIdValue + 'UseCustomPdf'
    this.idValueStylesheet = this.config.baseIdValue + 'UseStylesheet'
    this.idValueCustomPdfWithCustomXml = this.config.baseIdValue + 'UseCustomOdfWithCustomXml'
  }

  loadData(): Observable<any> {
    return this.reportService.loadReportSettings(this.communityId, this.config.reportType)
    .pipe(
      map((result) => {
        this.reportSettings = result
        if (result.stylesheetExists) {
          this.useStylesheet = true
          this.stylesheetNameToShow = 'stylesheet.xslt'
        }
      }), share()
    )
  }

  updateEnabled() {
    return this.reportSettings && (!this.useStylesheet || this.reportSettings.stylesheetExists) && (!this.reportSettings.customPdfs || this.textProvided(this.reportSettings.customPdfService))
  }

  handleExpanded(): void {
    this.animated = true
    if (this.reportSettings && this.reportSettings.customPdfs) {
      this.focusServiceField()
    }
  }

  serviceBlockExpanded() {
    this.focusServiceField()
  }

  update() {
    if (this.reportSettings) {
      let updateObservable: Observable<boolean>
      if (this.reportSettings.stylesheetExists && !this.useStylesheet) {
        // Delete stylesheet
        updateObservable = this.confirmationDialogService.confirmDangerous("Confirm stylesheet deletion", "Are you sure you want to delete the configured stylesheet?", "Delete", "Cancel")
      } else {
        updateObservable = of(true)
      }
      updateObservable.subscribe((proceed) => {
        if (proceed) {
          this.updatePending = true
          this.reportService.updateReportSettings(this.communityId, this.config.reportType, this.useStylesheet, this.uploadedStylesheet, this.reportSettings!)
          .subscribe(() => {
            if (!this.useStylesheet) {
              this.stylesheetNameToShow = undefined
              this.uploadedStylesheet = undefined
              this.reportSettings!.stylesheetExists = false
              this.resetStylesheet.emit()
            }
            this.popupService.success('Report settings updated.')
          }).add(() => {
            this.updatePending = false
          })
        }
      })
    }
  }

  preview(option: PreviewOption) {
    if (option.isXml) {
      this.previewPending = true
      this.reportService.exportDemoReportXml(this.communityId, this.config.reportType, this.useStylesheet, this.uploadedStylesheet, option.data)
      .subscribe((data) => {
        this.modalService.show(CodeEditorModalComponent, {
          class: 'modal-lg',
          initialState: {
            documentName: this.config.previewFileNameXml,
            editorOptions: {
              value: data,
              readOnly: true,
              lineNumbers: true,
              smartIndent: false,
              electricChars: false,
              mode: 'application/xml',
              download: {
                fileName: this.config.previewFileNameXml,
                mimeType: 'application/xml'
              }
            }
          }
        })
      }).add(() => {
        this.previewPending = false
      })
    } else {
      this.previewPending = true
      this.reportService.exportDemoReportPdf(this.communityId, this.config.reportType, this.reportSettings!, this.useStylesheet, this.uploadedStylesheet, option.data)
      .subscribe((response) => {
        this.handlePdfPreviewResult(response, this.config.previewFileNamePdf)
      }).add(() => {
        this.previewPending = false
      })
    }
  }

  abstract getPreviewConfig(): PreviewConfig

}
