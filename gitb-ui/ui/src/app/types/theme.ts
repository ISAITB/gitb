/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
