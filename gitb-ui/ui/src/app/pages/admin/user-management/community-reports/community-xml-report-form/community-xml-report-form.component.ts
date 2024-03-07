import { Component, Input } from '@angular/core';
import { FileData } from 'src/app/types/file-data.type';
import { BaseReportSettingsFormComponent } from '../base-report-settings-form.component';
import { ReportService } from 'src/app/services/report.service';
import { PopupService } from 'src/app/services/popup.service';
import { BsModalService } from 'ngx-bootstrap/modal';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { Observable, from, map, of, share } from 'rxjs';
import { CodeEditorModalComponent } from 'src/app/components/code-editor-modal/code-editor-modal.component';

@Component({ template: '' })
export abstract class CommunityXmlReportFormComponent extends BaseReportSettingsFormComponent {

  idValue!: string
  reportType!: number
  previewTitle!: string
  previewFileName!: string
  previewDefaultOption!: boolean
  previewOptions!: Array<{label: string, data: {[key: string]: any}}>

  stylesheetExists = false
  useStylesheet = false
  updatePending = false
  previewPending = false
  animated = false
  fileNameToShow?: string
  uploadedFile?: FileData
  acceptedFileTypes: string[] = ['application/xml', 'text/xml', 'text/xsl', 'application/xslt+xml' ]

  constructor(
    private reportService: ReportService,
    private popupService: PopupService,
    private modalService: BsModalService,
    private confirmationDialogService: ConfirmationDialogService
  ) { super() }

  ngOnInit(): void {
    super.ngOnInit()
    this.idValue = this.getIdValue()
    this.reportType = this.getReportType()
    this.previewTitle = this.getPreviewTitle()
    this.previewFileName = this.getPreviewFileName()
    this.previewDefaultOption = this.hasPreviewDefaultOption()
    this.previewOptions = this.getPreviewOptions()
  }

  loadData(): Observable<any> {
    return this.reportService.reportStylesheetExists(this.communityId, this.reportType)
    .pipe(
      map((result) => {
        this.stylesheetExists = result.exists
        this.useStylesheet = result.exists
        if (result.exists) {
          this.fileNameToShow = 'stylesheet.xslt'
        }
      }), share()
    )
  }

  updateEnabled() {
    return !this.useStylesheet || this.stylesheetExists
  }

  handleExpanded(): void {
    this.animated = true
  }

  selectFile(file: FileData) {
    this.fileNameToShow = file.name
    this.uploadedFile = file
    this.stylesheetExists = true
  }

  update() {
    let updateObservable: Observable<boolean>
    if (this.stylesheetExists && !this.useStylesheet) {
      // Delete stylesheet
      updateObservable = this.confirmationDialogService.confirmDangerous("Confirm stylesheet deletion", "Are you sure you want to delete the configured stylesheet?", "Delete", "Cancel")
    } else {
      updateObservable = of(true)
    }
    updateObservable.subscribe((proceed) => {
      if (proceed) {
        this.updatePending = true
        this.reportService.updateReportStylesheet(this.communityId, this.useStylesheet, this.reportType, this.uploadedFile)
        .subscribe(() => {
          if (!this.useStylesheet) {
            this.fileNameToShow = undefined
            this.uploadedFile = undefined
            this.stylesheetExists = false
          }
          this.popupService.success('Report settings updated.')
          this.collapseForm()
        }).add(() => {
          this.updatePending = false
        })
      }
    })
  }

  preview(optionIndex?: number) {
    this.previewPending = true
    let extraData: {[key: string]: any}|undefined
    if (optionIndex != undefined) {
      extraData = this.previewOptions[optionIndex].data
    }
    this.reportService.exportDemoReportInXML(this.communityId, this.reportType, this.useStylesheet, this.uploadedFile, extraData)
    .subscribe((data) => {
      this.modalService.show(CodeEditorModalComponent, {
        class: 'modal-lg',
        initialState: {
          documentName: this.previewTitle,
          editorOptions: {
            value: data,
            readOnly: true,
            lineNumbers: true,
            smartIndent: false,
            electricChars: false,
            mode: 'application/xml',
            download: {
              fileName: this.previewFileName,
              mimeType: 'application/xml'
            }
          }
        }
      })
    }).add(() => {
      this.previewPending = false
    })
  }

  viewStylesheet() {
    if (this.stylesheetExists) {
      let contentObservable: Observable<string>
      if (this.uploadedFile?.file) {
        contentObservable = from(this.uploadedFile.file.text())
      } else {
        contentObservable = this.reportService.getReportStylesheet(this.communityId, this.Constants.XML_REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT)
      }
      contentObservable.subscribe((data) => {
        this.modalService.show(CodeEditorModalComponent, {
          class: 'modal-lg',
          initialState: {
            documentName: 'Stylesheet',
            editorOptions: {
              value: data,
              readOnly: true,
              lineNumbers: true,
              smartIndent: false,
              electricChars: false,
              mode: 'application/xml',
              download: {
                fileName: 'stylesheet.xslt',
                mimeType: 'application/xslt+xml'
              }
            }
          }
        })
      })
    }
  }

  abstract getIdValue(): string
  abstract getReportType(): number
  abstract getPreviewTitle(): string
  abstract getPreviewFileName(): string
  hasPreviewDefaultOption() {
    return true
  }
  getPreviewOptions(): Array<{label: string, data: {[key: string]: any}}>  {
    return []
  }

}
