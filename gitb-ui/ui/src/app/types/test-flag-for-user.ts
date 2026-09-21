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

/**
 * Minimal flag record cached client-side (loaded once at login, refreshed only when the currently
 * cached community's flags are edited by an admin). For administrators (Test Bed or community) this
 * carries the internal name/colour; for organisation users the server sends the effective
 * (public-or-fallback-to-internal) name/colour instead. `adminOnly` is present for every role -
 * including for organisation users - since an admin-only flag, once set on a session, is still shown
 * read-only to organisation users. Only the *assignment* control needs to filter admin-only flags out
 * (client-side); display (tag, column, filter) applies to every flag regardless of `adminOnly`.
 */
export interface TestFlagForUser {

    id: number
    name: string
    colour: string
    adminOnly: boolean

}
