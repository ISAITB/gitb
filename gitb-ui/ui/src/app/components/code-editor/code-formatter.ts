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

import {JSON_MIME_TYPES, JSON_SUFFIX, normalizeMimeType, TURTLE_MIME_TYPES, XML_MIME_TYPES, XML_SUFFIX} from './code-editor-language';

/**
 * Dependency-free pretty-printers for the text formats displayed in code editors (JSON and JSON-LD, XML and RDF/XML, Turtle).
 * They only change whitespace (and, for XML, the representation of equivalent escapes) and return undefined if the content
 * cannot be formatted safely, in which case the content should be shown as-is.
 */
export type FormatterType = 'json'|'xml'|'turtle'

const INDENT = '  '

export function formatterTypeFor(mimeType?: string): FormatterType|undefined {
  const mime = normalizeMimeType(mimeType)
  if (mime) {
    if (JSON_MIME_TYPES.has(mime) && mime != 'text/javascript') return 'json'
    if (JSON_SUFFIX.test(mime)) return 'json'
    if (XML_MIME_TYPES.has(mime) || XML_SUFFIX.test(mime)) return 'xml'
    if (TURTLE_MIME_TYPES.has(mime)) return 'turtle'
  }
  return undefined
}

export function prettyPrint(content: string, mimeType?: string): string|undefined {
  try {
    switch (formatterTypeFor(mimeType)) {
      case 'json': return formatJson(content)
      case 'xml': return formatXml(content)
      case 'turtle': return formatTurtle(content)
    }
  } catch (e) {
    console.warn('Content could not be pretty-printed', e)
  }
  return undefined
}

function sameWithoutWhitespace(a: string, b: string): boolean {
  return a.replace(/\s+/g, '') == b.replace(/\s+/g, '')
}

// JSON

function formatJson(source: string): string|undefined {
  JSON.parse(source) // Throws if invalid.
  // Re-indent the original tokens so that numbers, escapes, key order and duplicate keys are kept exactly as provided.
  const n = source.length
  let out = ''
  let level = 0
  let i = 0
  const newLine = () => '\n' + INDENT.repeat(level)
  while (i < n) {
    const c = source[i]
    if (c == '"') {
      let j = i + 1
      while (j < n && source[j] != '"') {
        if (source[j] == '\\') j++
        j++
      }
      out += source.substring(i, j + 1)
      i = j + 1
    } else if (c == '{' || c == '[') {
      let j = i + 1
      while (j < n && /\s/.test(source[j])) j++
      if (source[j] == (c == '{' ? '}' : ']')) {
        out += c + source[j]
        i = j + 1
      } else {
        level++
        out += c + newLine()
        i++
      }
    } else if (c == '}' || c == ']') {
      level--
      out += newLine() + c
      i++
    } else if (c == ',') {
      out += ',' + newLine()
      i++
    } else if (c == ':') {
      out += ': '
      i++
    } else if (/\s/.test(c)) {
      i++
    } else {
      out += c
      i++
    }
  }
  return sameWithoutWhitespace(source, out) ? out : undefined
}

// XML

function escapeXmlText(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\r/g, '&#13;')
}

function escapeXmlAttribute(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;')
    .replace(/\t/g, '&#9;').replace(/\n/g, '&#10;').replace(/\r/g, '&#13;')
}

function xmlAttributes(element: Element): string {
  let result = ''
  for (let i = 0; i < element.attributes.length; i++) {
    const attribute = element.attributes[i]
    result += ' ' + attribute.name + '="' + escapeXmlAttribute(attribute.value) + '"'
  }
  return result
}

/**
 * Serialise a node exactly as it is, without adding any whitespace.
 */
function xmlRaw(node: Node): string {
  switch (node.nodeType) {
    case Node.ELEMENT_NODE: {
      const element = node as Element
      const open = '<' + element.tagName + xmlAttributes(element)
      if (element.childNodes.length == 0) return open + '/>'
      return open + '>' + Array.from(element.childNodes).map(xmlRaw).join('') + '</' + element.tagName + '>'
    }
    case Node.TEXT_NODE: return escapeXmlText(node.nodeValue ?? '')
    case Node.CDATA_SECTION_NODE: return '<![CDATA[' + node.nodeValue + ']]>'
    case Node.COMMENT_NODE: return '<!--' + node.nodeValue + '-->'
    case Node.PROCESSING_INSTRUCTION_NODE: return xmlProcessingInstruction(node as ProcessingInstruction)
  }
  return ''
}

function xmlProcessingInstruction(node: ProcessingInstruction): string {
  return '<?' + node.target + (node.data ? ' ' + node.data : '') + '?>'
}

function xmlIndented(element: Element, level: number): string {
  const pad = INDENT.repeat(level)
  const open = '<' + element.tagName + xmlAttributes(element)
  const children = Array.from(element.childNodes)
  if (children.length == 0) return pad + open + '/>'
  const hasElements = children.some(child => child.nodeType == Node.ELEMENT_NODE)
  const hasText = children.some(child => child.nodeType == Node.CDATA_SECTION_NODE || (child.nodeType == Node.TEXT_NODE && (child.nodeValue ?? '').trim() != ''))
  if (!hasElements || hasText || element.getAttribute('xml:space') == 'preserve') {
    // Text-only, mixed or whitespace-preserving content is kept as is.
    return pad + xmlRaw(element)
  }
  const lines: string[] = []
  for (const child of children) {
    if (child.nodeType == Node.ELEMENT_NODE) {
      lines.push(xmlIndented(child as Element, level + 1))
    } else if (child.nodeType == Node.COMMENT_NODE || child.nodeType == Node.PROCESSING_INSTRUCTION_NODE) {
      lines.push(pad + INDENT + xmlRaw(child))
    }
  }
  return pad + open + '>\n' + lines.join('\n') + '\n' + pad + '</' + element.tagName + '>'
}

function formatXml(source: string): string|undefined {
  if (/<!DOCTYPE[^>]*\[/i.test(source)) return undefined // Internal DTD subsets cannot be reproduced.
  const doc = new DOMParser().parseFromString(source, 'application/xml')
  if (doc.getElementsByTagName('parsererror').length > 0) return undefined
  const parts: string[] = []
  const declaration = /^\s*(<\?xml[^?]*\?>)/.exec(source)
  if (declaration) parts.push(declaration[1])
  for (const node of Array.from(doc.childNodes)) {
    switch (node.nodeType) {
      case Node.DOCUMENT_TYPE_NODE: {
        const type = node as DocumentType
        let text = '<!DOCTYPE ' + type.name
        if (type.publicId) {
          text += ' PUBLIC "' + type.publicId + '" "' + type.systemId + '"'
        } else if (type.systemId) {
          text += ' SYSTEM "' + type.systemId + '"'
        }
        parts.push(text + '>')
        break
      }
      case Node.COMMENT_NODE:
      case Node.PROCESSING_INSTRUCTION_NODE:
        parts.push(xmlRaw(node))
        break
      case Node.ELEMENT_NODE:
        parts.push(xmlIndented(node as Element, 0))
        break
    }
  }
  return parts.length > 0 ? parts.join('\n') : undefined
}

// Turtle

interface TurtleToken {
  kind: 'iri'|'string'|'lang'|'datatype'|'word'|'punct'|'comment'
  text: string
  ownLine?: boolean
}

function tokenizeTurtle(source: string): TurtleToken[] {
  const tokens: TurtleToken[] = []
  const n = source.length
  let i = 0
  let lineHasToken = false
  while (i < n) {
    const c = source[i]
    if (c == '\n') {
      lineHasToken = false
      i++
    } else if (/\s/.test(c)) {
      i++
    } else if (c == '#') {
      let j = i
      while (j < n && source[j] != '\n' && source[j] != '\r') j++
      tokens.push({kind: 'comment', text: source.substring(i, j).trimEnd(), ownLine: !lineHasToken})
      i = j
    } else {
      lineHasToken = true
      if (c == '<') {
        const end = source.indexOf('>', i)
        if (end < 0) throw new Error('Unterminated IRI')
        tokens.push({kind: 'iri', text: source.substring(i, end + 1)})
        i = end + 1
      } else if (c == '"' || c == "'") {
        const triple = c + c + c
        let j: number
        if (source.startsWith(triple, i)) {
          j = i + 3
          while (j < n && !source.startsWith(triple, j)) j += (source[j] == '\\') ? 2 : 1
          if (j >= n) throw new Error('Unterminated long string')
          j += 3
        } else {
          j = i + 1
          while (j < n && source[j] != c) j += (source[j] == '\\') ? 2 : 1
          if (j >= n) throw new Error('Unterminated string')
          j += 1
        }
        tokens.push({kind: 'string', text: source.substring(i, j)})
        i = j
      } else if (c == '@' && tokens.length > 0 && tokens[tokens.length - 1].kind == 'string') {
        const match = /^@[a-zA-Z]+(-[a-zA-Z0-9]+)*/.exec(source.substring(i))
        if (!match) throw new Error('Invalid language tag')
        tokens.push({kind: 'lang', text: match[0]})
        i += match[0].length
      } else if (c == '^' && source[i + 1] == '^') {
        tokens.push({kind: 'datatype', text: '^^'})
        i += 2
      } else if (';,[]()'.includes(c) || (c == '.' && !/[0-9]/.test(source[i + 1] ?? ''))) {
        tokens.push({kind: 'punct', text: c})
        i++
      } else {
        let j = i
        while (j < n && !/[\s;,\[\]()<>"'#^]/.test(source[j])) j++
        if (j == i) j = i + 1
        let word = source.substring(i, j)
        let trailingDots = 0
        while (word.length > trailingDots && word[word.length - 1 - trailingDots] == '.') trailingDots++
        word = word.substring(0, word.length - trailingDots)
        if (word.length > 0) tokens.push({kind: 'word', text: word})
        for (let d = 0; d < trailingDots; d++) tokens.push({kind: 'punct', text: '.'})
        i = j
      }
    }
  }
  return tokens
}

interface TurtleFrame {
  state: 'subject'|'predicate'|'object'
  base: number
}

/**
 * Reflow Turtle: directives on a line each, one subject block per statement with each predicate on its own line,
 * object lists kept inline, blank nodes expanded over several lines and collections kept inline. Tokens are never changed.
 */
function formatTurtle(source: string): string|undefined {
  const tokens = tokenizeTurtle(source)
  const lines: string[] = []
  const stack: TurtleFrame[] = [{state: 'subject', base: 0}]
  let text = ''
  let textIndent = 0
  let pendingBlank = false
  let lastKind: 'directive'|'triple'|undefined
  let directive: {remaining: number, withDot: boolean}|undefined
  let collectionDepth = 0
  let collectionBrackets = 0
  let glue = false

  const endLine = () => {
    if (text != '') {
      lines.push(INDENT.repeat(textIndent / INDENT.length) + text)
      text = ''
    }
  }
  const startLine = (indent: number) => {
    endLine()
    textIndent = indent
  }
  const put = (value: string, separate = true) => {
    text = (text == '' || !separate) ? text + value : text + ' ' + value
  }
  const blank = () => {
    endLine()
    if (lines.length > 0 && lines[lines.length - 1] != '') lines.push('')
  }
  const attachToLastLine = (value: string): boolean => {
    if (text == '' && lines.length > 0 && lines[lines.length - 1] != '') {
      lines[lines.length - 1] += value
      return true
    }
    return false
  }
  const startStatement = (kind: 'directive'|'triple') => {
    endLine()
    if (pendingBlank || (kind == 'triple' && lastKind == 'directive')) blank()
    pendingBlank = false
  }
  const endStatement = (kind: 'directive'|'triple') => {
    endLine()
    pendingBlank = kind == 'triple'
    lastKind = kind
    stack[stack.length - 1].state = 'subject'
  }
  // Prepare the writing of a term (word, IRI, string, collection or blank node) and advance the state of the frame.
  const beginTerm = (frame: TurtleFrame) => {
    if (frame.state == 'subject') {
      startStatement('triple')
      startLine(frame.base)
      frame.state = 'predicate'
    } else if (frame.state == 'predicate') {
      startLine(frame.base + INDENT.length)
      frame.state = 'object'
    } else if (text == '') {
      startLine(frame.base + INDENT.length)
    }
  }

  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i]
    const frame = stack[stack.length - 1]
    if (token.kind == 'comment') {
      if (token.ownLine || (text == '' && !(lines.length > 0 && lines[lines.length - 1] != ''))) {
        endLine()
        if (stack.length == 1 && pendingBlank) {
          blank()
          pendingBlank = false
        }
        startLine(frame.state == 'subject' ? 0 : frame.base + INDENT.length)
        put(token.text)
        endLine()
      } else if (!attachToLastLine(' ' + token.text)) {
        put(token.text)
        endLine()
      }
      continue
    }
    if (directive) {
      if (token.kind == 'punct' && token.text == '.') {
        put('.')
        directive = undefined
        endStatement('directive')
      } else {
        put(token.text)
        directive.remaining--
        if (directive.remaining <= 0 && !directive.withDot) {
          directive = undefined
          endStatement('directive')
        }
      }
      continue
    }
    if (token.kind == 'lang') {
      put(token.text, false)
    } else if (token.kind == 'datatype') {
      put(token.text, false)
      glue = true
    } else if (token.kind == 'punct') {
      switch (token.text) {
        case ';':
          if (collectionDepth > 0) {
            put(';')
          } else {
            if (!attachToLastLine(' ;')) put(';')
            endLine()
            frame.state = 'predicate'
          }
          break
        case ',':
          if (!attachToLastLine(',')) put(',', false)
          if (collectionDepth == 0) frame.state = 'object'
          break
        case '.':
          if (collectionDepth > 0 || stack.length > 1) throw new Error('Unexpected statement end')
          if (!attachToLastLine(' .')) put('.')
          endStatement('triple')
          break
        case '[':
          if (collectionDepth > 0) {
            collectionBrackets++
            put('[')
          } else {
            beginTerm(frame)
            if (tokens[i + 1]?.kind == 'punct' && tokens[i + 1].text == ']') {
              put('[]')
              i++
            } else {
              put('[')
              stack.push({state: 'predicate', base: textIndent})
            }
          }
          break
        case ']':
          if (collectionDepth > 0 && collectionBrackets > 0) {
            collectionBrackets--
            put(']')
          } else {
            if (stack.length < 2) throw new Error('Unbalanced bracket')
            const closed = stack.pop()!
            startLine(closed.base)
            put(']')
          }
          break
        case '(':
          if (collectionDepth == 0) beginTerm(frame)
          if (tokens[i + 1]?.kind == 'punct' && tokens[i + 1].text == ')') {
            put('()')
            i++
          } else {
            put('(')
            collectionDepth++
          }
          break
        case ')':
          if (collectionDepth == 0) throw new Error('Unbalanced parenthesis')
          collectionDepth--
          put(')')
          break
      }
    } else if (glue) {
      put(token.text, false)
      glue = false
    } else if (collectionDepth > 0) {
      put(token.text)
    } else if (token.kind == 'word' && stack.length == 1 && frame.state == 'subject' && /^(@?prefix|@?base)$/i.test(token.text)) {
      startStatement('directive')
      startLine(0)
      put(token.text)
      directive = {remaining: /prefix/i.test(token.text) ? 2 : 1, withDot: token.text.startsWith('@')}
    } else {
      beginTerm(frame)
      put(token.text)
    }
  }
  endLine()
  if (stack.length != 1 || collectionDepth != 0 || directive) return undefined
  const result = lines.join('\n')
  return sameWithoutWhitespace(source, result) ? result : undefined
}
