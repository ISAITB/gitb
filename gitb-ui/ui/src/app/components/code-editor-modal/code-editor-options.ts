export interface EditorOptions {

    value?: string
    readOnly: boolean
    lineNumbers: boolean
    smartIndent: boolean
    electricChars: boolean
    copy?: boolean
    mode?: string
    styleClass?: string
    download?: {
        fileName: string
        mimeType: string
    }
}
