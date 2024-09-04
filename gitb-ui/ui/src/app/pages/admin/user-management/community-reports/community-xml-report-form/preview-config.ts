import { PreviewOption } from "./preview-option"

export interface PreviewConfig {

    baseIdValue: string
    previewOptions: PreviewOption[][]
    reportType: number
    previewTitleXml: string
    previewFileNameXml: string
    previewFileNamePdf: string
  
}