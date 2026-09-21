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

import {AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, EventEmitter, forwardRef, Input, NgZone, OnChanges, OnDestroy, Output, SimpleChanges} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {indentWithTab} from '@codemirror/commands';
import {Compartment, EditorState, Extension, Prec, StateEffect, StateField} from '@codemirror/state';
import {Decoration, DecorationSet, EditorView, keymap, lineNumbers, ViewUpdate, WidgetType} from '@codemirror/view';
import {minimalSetup} from 'codemirror';
import {EditorOptions} from '../code-editor-modal/code-editor-options';
import {languageForMimeType} from './code-editor-language';

/**
 * Block widget that displays a pre-built element.
 */
class ElementWidget extends WidgetType {

  constructor(private readonly element: HTMLElement) { super() }

  override eq(other: ElementWidget): boolean {
    return other.element === this.element
  }

  override toDOM(): HTMLElement {
    return this.element
  }

  override ignoreEvent(): boolean {
    return true
  }

}

const addDecoration = StateEffect.define<{from: number, to: number, decoration: Decoration}>()
const clearDecorations = StateEffect.define<null>()

/**
 * Holds the decorations (line classes, marks, widgets) added through the component's API. Decorations are dropped
 * whenever the whole content is replaced.
 */
const decorationField = StateField.define<DecorationSet>({
  create: () => Decoration.none,
  update(decorations, transaction) {
    decorations = decorations.map(transaction.changes)
    for (const effect of transaction.effects) {
      if (effect.is(clearDecorations)) {
        decorations = Decoration.none
      } else if (effect.is(addDecoration)) {
        const {from, to, decoration} = effect.value
        // Decorations with a key are only added once.
        let exists = false
        if (decoration.spec.key) {
          decorations.between(from, to, (existingFrom, existingTo, existing) => {
            if (existingFrom == from && existingTo == to && existing.spec.key == decoration.spec.key) exists = true
          })
        }
        if (!exists) {
          decorations = decorations.update({add: [decoration.range(from, to)]})
        }
      }
    }
    return decorations
  },
  provide: field => EditorView.decorations.from(field)
})

/**
 * Code editor based on CodeMirror 6, usable with ngModel. Line numbers used by the API are 1-based.
 */
@Component({
  selector: 'app-code-editor',
  template: '',
  styles: [`
    :host { display: block; height: var(--code-editor-height, 300px); }
    :host ::ng-deep .cm-editor { height: 100%; }
    :host ::ng-deep .cm-editor.cm-focused { outline: none; }
  `],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CodeEditorComponent),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false
})
export class CodeEditorComponent implements AfterViewInit, OnChanges, OnDestroy, ControlValueAccessor {

  @Input() name?: string
  @Input() options?: Partial<Pick<EditorOptions, 'readOnly'|'lineNumbers'|'mode'>>
  @Input() submitOnCtrlEnter = false
  @Output() focusChange = new EventEmitter<boolean>()
  @Output() loaded = new EventEmitter<CodeEditorComponent>()

  view?: EditorView

  private value = ''
  private disabled = false
  private applyingModelValue = false
  private readonly languageCompartment = new Compartment()
  private readonly readOnlyCompartment = new Compartment()
  private readonly gutterCompartment = new Compartment()
  private readonly submitCompartment = new Compartment()
  private onChange = (_: string) => {}
  private onTouched = () => {}

  constructor(
    private readonly host: ElementRef<HTMLElement>,
    private readonly zone: NgZone
  ) { }

  ngAfterViewInit(): void {
    // Run outside Angular to avoid a change detection cycle for every editor DOM event.
    this.zone.runOutsideAngular(() => {
      this.view = new EditorView({
        parent: this.host.nativeElement,
        state: EditorState.create({
          doc: this.value,
          extensions: [
            minimalSetup,
            keymap.of([indentWithTab]),
            decorationField,
            this.languageCompartment.of(this.languageExtension()),
            this.readOnlyCompartment.of(this.readOnlyExtension()),
            this.gutterCompartment.of(this.gutterExtension()),
            this.submitCompartment.of(this.submitExtension()),
            EditorView.updateListener.of(update => this.viewUpdated(update))
          ]
        })
      })
    })
    // Emit asynchronously so that parents can subscribe in their own ngAfterViewInit.
    setTimeout(() => this.loaded.emit(this))
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.view) {
      const effects: StateEffect<unknown>[] = []
      if (changes['options']) {
        effects.push(
          this.languageCompartment.reconfigure(this.languageExtension()),
          this.readOnlyCompartment.reconfigure(this.readOnlyExtension()),
          this.gutterCompartment.reconfigure(this.gutterExtension())
        )
      }
      if (changes['submitOnCtrlEnter']) {
        effects.push(this.submitCompartment.reconfigure(this.submitExtension()))
      }
      if (effects.length > 0) {
        this.view.dispatch({effects})
      }
    }
  }

  ngOnDestroy(): void {
    this.view?.destroy()
  }

  // ControlValueAccessor

  writeValue(value: string|undefined|null): void {
    this.value = value ?? ''
    if (this.view && this.view.state.doc.toString() != this.value) {
      this.applyingModelValue = true
      try {
        this.replaceContent(this.value)
      } finally {
        this.applyingModelValue = false
      }
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled
    this.view?.dispatch({effects: this.readOnlyCompartment.reconfigure(this.readOnlyExtension())})
  }

  // API

  get lineCount(): number {
    return this.view?.state.doc.lines ?? 0
  }

  getValue(): string {
    return this.view?.state.doc.toString() ?? this.value
  }

  setValue(value: string) {
    if (this.view) {
      this.replaceContent(value)
    }
  }

  appendText(text: string) {
    if (this.view) {
      this.view.dispatch({changes: {from: this.view.state.doc.length, insert: text}})
    }
  }

  focus() {
    this.view?.focus()
  }

  /**
   * Re-measure the editor (e.g. after it was made visible).
   */
  refresh() {
    this.view?.requestMeasure()
  }

  setHeight(pixels: number) {
    this.host.nativeElement.style.setProperty('--code-editor-height', pixels + 'px')
  }

  scrollToLine(line: number) {
    if (this.view) {
      this.view.dispatch({effects: EditorView.scrollIntoView(this.lineAt(line).from, {y: 'center'})})
    }
  }

  addLineClass(line: number, className: string) {
    this.decorate(line, Decoration.line({class: className, key: 'line:' + className}), false)
  }

  /**
   * Apply a class to the text of the line.
   */
  markLine(line: number, className: string) {
    this.decorate(line, Decoration.mark({class: className, key: 'mark:' + className}), true)
  }

  /**
   * Display an element in its own block above the line.
   */
  addLineWidget(line: number, element: HTMLElement) {
    this.decorate(line, Decoration.widget({widget: new ElementWidget(element), block: true, side: -1}), false)
  }

  // Internals

  private lineAt(line: number) {
    const doc = this.view!.state.doc
    return doc.line(Math.min(Math.max(line, 1), doc.lines))
  }

  private decorate(line: number, decoration: Decoration, wholeLine: boolean) {
    if (this.view) {
      const lineInfo = this.lineAt(line)
      this.view.dispatch({effects: addDecoration.of({from: lineInfo.from, to: wholeLine ? lineInfo.to : lineInfo.from, decoration})})
    }
  }

  private replaceContent(content: string) {
    this.view!.dispatch({
      changes: {from: 0, to: this.view!.state.doc.length, insert: content},
      effects: clearDecorations.of(null)
    })
  }

  private viewUpdated(update: ViewUpdate) {
    if (update.docChanged && !this.applyingModelValue) {
      this.value = update.state.doc.toString()
      this.zone.run(() => this.onChange(this.value))
    }
    if (update.focusChanged) {
      const focused = update.view.hasFocus
      this.zone.run(() => {
        this.focusChange.emit(focused)
        if (!focused) this.onTouched()
      })
    }
  }

  private languageExtension(): Extension {
    return languageForMimeType(this.options?.mode)
  }

  private readOnlyExtension(): Extension {
    return EditorState.readOnly.of(this.options?.readOnly === true || this.disabled)
  }

  private gutterExtension(): Extension {
    return this.options?.lineNumbers ? lineNumbers() : []
  }

  private submitExtension(): Extension {
    if (this.submitOnCtrlEnter) {
      return Prec.highest(keymap.of([{
        key: 'Ctrl-Enter',
        run: () => {
          // Trigger the ngSubmit of the enclosing form.
          this.host.nativeElement.closest('form')?.requestSubmit()
          return true
        }
      }]))
    }
    return []
  }

}
