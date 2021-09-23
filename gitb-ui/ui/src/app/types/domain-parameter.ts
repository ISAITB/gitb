export interface DomainParameter {

    id: number
    name: string
    description?: string
    kind: 'HIDDEN'|'BINARY'|'SIMPLE'
    kindLabel?: string
    inTests: boolean
    value?: string
    contentType?: string

    valueToShow?: string
    selected?: boolean
}
