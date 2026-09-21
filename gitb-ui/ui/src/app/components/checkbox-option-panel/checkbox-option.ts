/*
 * Copyright (C) 2026 European Union
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

import { NavigationTarget } from "../../types/navigation-target"

export interface CheckboxOption {

    key: string
    label: string
    default: boolean
    iconClass?: string
    /** Optional colour override for the icon (e.g. a flag's configured colour). */
    iconColour?: string
    /**
     * Optional colour behind the icon (e.g. a tag's background, with `iconColour` as its foreground).
     * When set, `iconClass` is stacked (real FontAwesome icon stacking, not a CSS circle - avoids
     * mismatched anti-aliasing) over a backing icon, `iconBackgroundClass` by default.
     */
    iconBackground?: string
    /** Backing shape for `iconBackground`, stacked behind `iconClass`. Defaults to `fa-solid fa-circle`. */
    iconBackgroundClass?: string
    /** Color to use for the icon's text shadow. */
    iconShadowColor?: string
    disabled?: boolean
    /** Set only for single-selection options that navigate (e.g. "View system"), so the option is rendered as a real link. */
    target?: NavigationTarget

}
