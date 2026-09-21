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

import {StreamLanguage} from '@codemirror/language';
import {Extension} from '@codemirror/state';
import {http} from '@codemirror/legacy-modes/mode/http';
import {html, xml} from '@codemirror/legacy-modes/mode/xml';
import {xQuery} from '@codemirror/legacy-modes/mode/xquery';
import {json} from '@codemirror/legacy-modes/mode/javascript';
import {yaml} from '@codemirror/legacy-modes/mode/yaml';
import {turtle} from '@codemirror/legacy-modes/mode/turtle';

const XML = StreamLanguage.define(xml)
const JSON = StreamLanguage.define(json)
const HTML = StreamLanguage.define(html)
const HTTP = StreamLanguage.define(http)
const XQUERY = StreamLanguage.define(xQuery)
const YAML = StreamLanguage.define(yaml)
const TURTLE = StreamLanguage.define(turtle)

const XML_MIME_TYPES = new Set(['text/xml', 'application/xml', 'application/rdf+xml'])
const JSON_MIME_TYPES = new Set(['application/json', 'application/ld+json', 'text/javascript'])
const YAML_MIME_TYPES = new Set(['application/x-yaml', 'text/yaml'])
const TURTLE_MIME_TYPES = new Set(['application/turtle', 'application/x-turtle', 'text/turtle'])
const HTML_MIME_TYPES = new Set(['text/html', 'application/x-ejs', 'application/x-aspx', 'application/x-jsp', 'application/x-erb'])
const XML_SUFFIX = /^[\w-]+\/[\w-]+\+xml$/

/**
 * Map a MIME type to the highlighting language to use. Types without a supported language are shown as plain text.
 */
export function languageForMimeType(mimeType?: string): Extension {
  const mime = mimeType?.trim().toLowerCase()
  if (mime) {
    if (XML_MIME_TYPES.has(mime) || XML_SUFFIX.test(mime)) return XML
    if (HTML_MIME_TYPES.has(mime)) return HTML
    if (mime == 'message/http') return HTTP
    if (mime == 'application/xquery') return XQUERY
    if (JSON_MIME_TYPES.has(mime)) return JSON
    if (YAML_MIME_TYPES.has(mime)) return YAML
    if (TURTLE_MIME_TYPES.has(mime)) return TURTLE
  }
  return []
}
