export interface LoginResultOk {
    user_id: number
    path?: string
    access_token: string
    token_type: string
    expires_in: number
    registered: boolean
}
