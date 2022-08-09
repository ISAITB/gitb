export interface StatementParameterMinimal {

    id: number
    testKey: string
    name: string
    kind: 'SIMPLE'|'SECRET'|'BINARY'
    kindLabel?: string
    selected?: boolean
    
}