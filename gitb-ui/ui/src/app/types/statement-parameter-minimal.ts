export interface StatementParameterMinimal {

    id: number
    name: string
    kind: 'SIMPLE'|'SECRET'|'BINARY'
    kindLabel?: string
    selected?: boolean
    
}