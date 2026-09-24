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
 * The kinds ("kind") supported for organisation, system and actor endpoint configuration properties.
 * SIMPLE, MULTILINE_TEXT, CODE and RICH_TEXT all hold a plain string value and are transported
 * identically - they only differ in how the value is captured/rendered in the UI (a text input, a
 * textarea, a code editor and a rich text editor, respectively). BINARY is held as a file and SECRET
 * is held encrypted.
 */
export type PropertyKind = 'SIMPLE'|'BINARY'|'SECRET'|'MULTILINE_TEXT'|'CODE'|'RICH_TEXT'
