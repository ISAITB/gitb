export interface EmailSettings {

    // Base settings.
    enabled: boolean
    from?: string
    to?: string[]
    host?: string
    port?: number
    authenticate?: boolean
    username?: string
    password?: string
    sslEnabled?: boolean
    sslProtocols?: string[]
    startTlsEnabled?: boolean
    // Contact form settings
    defaultSupportMailbox?: string
    maxAttachmentCount?: number
    maxAttachmentSize?: number
    allowedAttachmentTypes?: string[]
    // Notification settings
    testInteractionReminder?: number 
    contactFormEnabled?: boolean
    contactFormCopyDefaultMailbox?: boolean

    updatePassword?: boolean
    newPassword?: string

}
