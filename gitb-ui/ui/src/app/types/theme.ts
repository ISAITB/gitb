import { FileData } from "./file-data.type"

export interface Theme {

    id: number,
    key: string,
    description?: string,
    active: boolean,
    custom: boolean,
    separatorTitleColor: string,
    modalTitleColor: string,
    tableTitleColor: string,
    cardTitleColor: string,
    pageTitleColor: string,
    headingColor: string,
    tabLinkColor: string,
    footerTextColor: string,
    headerBackgroundColor: string,
    headerBorderColor: string,
    headerSeparatorColor: string,
    headerLogoPath: string,
    footerBackgroundColor: string,
    footerBorderColor: string,
    footerLogoPath: string,
    footerLogoDisplay: string,
    faviconPath: string,
    primaryButtonColor: string,
    primaryButtonLabelColor: string,
    primaryButtonHoverColor: string,
    primaryButtonActiveColor: string,
    secondaryButtonColor: string,
    secondaryButtonLabelColor: string,
    secondaryButtonHoverColor: string,
    secondaryButtonActiveColor: string,

    headerLogoFile?: FileData
    footerLogoFile?: FileData
    faviconFile?: FileData

}
