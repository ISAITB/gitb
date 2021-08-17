export interface AnyContent {

    name?: string
    value?: string
    valueToUse?: string
    embeddingMethod?: 'BASE64'|'STRING'|'URI'
    item?: AnyContent[]
    mimeType?: string

}
