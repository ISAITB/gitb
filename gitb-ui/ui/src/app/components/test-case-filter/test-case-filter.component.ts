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

import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {TestCaseFilterApi} from './test-case-filter-api';
import {CheckboxOption} from '../checkbox-option-panel/checkbox-option';
import {Constants} from '../../common/constants';
import {DataService} from '../../services/data.service';
import {TestCaseFilterState} from './test-case-filter-state';
import {CheckboxOptionState} from '../checkbox-option-panel/checkbox-option-state';
import {TestCaseFilterOptions} from './test-case-filter-options';
import {CheckBoxOptionPanelComponentApi} from '../checkbox-option-panel/check-box-option-panel-component-api';
import {map, Observable, of, tap} from 'rxjs';
import {TestCaseTagFilterInfo} from '../../types/test-case-tag-filter-info';

@Component({
  selector: 'app-test-case-filter',
  standalone: false,
  templateUrl: './test-case-filter.component.html'
})
export class TestCaseFilterComponent implements TestCaseFilterApi, OnInit {

  @Input() options?: TestCaseFilterOptions
  @Output() apply = new EventEmitter<TestCaseFilterState>()
  @ViewChild('optionPanel') optionPanel?: CheckBoxOptionPanelComponentApi

  testDisplayOptions!: CheckboxOption[][]

  private showSuccessful?: boolean
  private showFailed?: boolean
  private showIncomplete?: boolean
  private showOptional?: boolean
  private showDisabled?: boolean

  // Tags are loaded lazily (once, on first opening of the control - see tagsLoaderProvider) rather than
  // eagerly like the other filter groups, since they need a dedicated request.
  private tagsLoaded = false
  private availableTags: TestCaseTagFilterInfo[] = []
  private untaggedAvailable = false
  // undefined means "not customised yet" (defaults to every tag checked), distinct from an explicit
  // (possibly empty) selection made by the user or restored from a saved display state.
  private selectedTagKeys?: Set<string>
  private includeUntagged = true

  constructor(private readonly dataService: DataService) {
  }

  ngOnInit(): void {
    this.updateOptions(this.options, false)
  }

  clearCachedTags(): void {
    this.tagsLoaded = false
  }

  /**
   * Bound as the panel's `optionProvider`: on first open it lazily fetches the statement's distinct
   * tags (if a loader was supplied) and folds them into the option groups; on later opens it returns the
   * already-built groups with no further request, so the tag list is fetched at most once.
   */
  tagsLoaderProvider = (): Observable<CheckboxOption[][]> => {
    if (this.tagsLoaded || this.options?.tagsLoader == undefined) {
      return of(this.testDisplayOptions)
    }
    return this.options.tagsLoader().pipe(
      tap(result => {
        this.availableTags = result.tags
        this.untaggedAvailable = result.untagged
        this.tagsLoaded = true
        this.updateOptions(this.options, true)
      }),
      map(() => this.testDisplayOptions)
    )
  }

  refreshOptions(options: TestCaseFilterOptions|undefined, keepCurrentState: boolean): void {
    this.updateOptions(options, keepCurrentState)
  }

  documentEscape(): void {
    this.optionPanel?.documentEscape()
  }

  documentClick(event: Event): void {
    this.optionPanel?.documentClick(event)
  }

  private updateOptions(options: TestCaseFilterOptions|undefined, keepCurrentState: boolean): void {
    let showSuccessfulDefault: boolean|undefined
    let showFailedDefault: boolean|undefined
    let showIncompleteDefault: boolean|undefined
    if (keepCurrentState) {
      showSuccessfulDefault = this.showSuccessful
      showFailedDefault = this.showFailed
      showIncompleteDefault = this.showIncomplete
    }
    if (showSuccessfulDefault == undefined) showSuccessfulDefault = options == undefined || options.initialState == undefined || options.initialState.showSuccessful
    if (showFailedDefault == undefined) showFailedDefault = options == undefined || options.initialState == undefined || options.initialState.showFailed
    if (showIncompleteDefault == undefined) showIncompleteDefault = options == undefined || options.initialState == undefined || options.initialState.showIncomplete
    this.testDisplayOptions = [[
      {key: Constants.TEST_FILTER.SUCCEEDED, label: 'Succeeded tests', default: showSuccessfulDefault, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)},
      {key: Constants.TEST_FILTER.FAILED, label: 'Failed tests', default: showFailedDefault, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)},
      {key: Constants.TEST_FILTER.INCOMPLETE, label: 'Incomplete tests', default: showIncompleteDefault, iconClass: this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)}
    ]]
    const otherOptions: CheckboxOption[] = []
    if (options?.showOptional) {
      let showOptionalDefault: boolean|undefined
      if (keepCurrentState) {
        showOptionalDefault = this.showOptional
      }
      if (showOptionalDefault == undefined) showOptionalDefault = options.initialState == undefined || options.initialState.showOptional
      otherOptions.push({key: Constants.TEST_FILTER.OPTIONAL, label: 'Optional tests', default: showOptionalDefault})
    }
    if (options?.showDisabled) {
      let showDisabledDefault: boolean|undefined
      if (keepCurrentState) {
        showDisabledDefault = this.showDisabled
      }
      if (showDisabledDefault == undefined) showDisabledDefault = options.initialState != undefined && options.initialState.showDisabled
      otherOptions.push({key: Constants.TEST_FILTER.DISABLED, label: 'Disabled tests', default: showDisabledDefault})
    }
    if (otherOptions.length > 0) {
      this.testDisplayOptions.push(otherOptions)
    }
    let untaggedDefault: boolean|undefined
    if (keepCurrentState) {
      untaggedDefault = this.includeUntagged
    }
    if (untaggedDefault == undefined) untaggedDefault = options == undefined || options.initialState == undefined || options.initialState.untagged == undefined || options.initialState.untagged
    this.includeUntagged = untaggedDefault
    if (!keepCurrentState && this.selectedTagKeys == undefined && options?.initialState?.tagKeys != undefined) {
      this.selectedTagKeys = new Set(options.initialState.tagKeys)
    }
    if (this.tagsLoaded) {
      const tagOptions = this.buildTagOptions()
      if (tagOptions.length > 0) {
        this.testDisplayOptions.push(tagOptions)
      }
    }
    this.optionPanel?.refresh(this.testDisplayOptions)
  }

  private buildTagOptions(): CheckboxOption[] {
    const tagOptions: CheckboxOption[] = []
    const sortedTags = [...this.availableTags].sort((a, b) => a.name.localeCompare(b.name))
    for (const tag of sortedTags) {
      // A tag left at its default presentation (unset/white background) renders with the neutral colours
      // used elsewhere for a default tag (light grey outline on white) rather than the literal stored
      // foreground/background, which would otherwise paint an invisible (white-on-white) glyph.
      tagOptions.push({
        key: Constants.TEST_FILTER_TAG_KEY_PREFIX + tag.key,
        label: tag.name,
        default: this.selectedTagKeys == undefined || this.selectedTagKeys.has(tag.key),
        iconClass: 'fa-solid fa-circle-half-stroke',
        iconColour: tag.background,
        iconBackground: tag.foreground,
        iconShadowColor: (tag.background == '#fff' || tag.background == '#ffffff')?'#000':undefined
      })
    }
    // Untagged tests is only meaningful (and only shown) when there is at least one real tag to
    // contrast it against - otherwise "untagged" would trivially match everything. Listed last.
    if (tagOptions.length > 0 && this.untaggedAvailable) {
      tagOptions.push({
        key: Constants.TEST_FILTER.UNTAGGED,
        label: 'Untagged tests',
        default: this.includeUntagged
      })
    }
    return tagOptions
  }

  resultFilterUpdated(choices: CheckboxOptionState) {
    this.showSuccessful = choices[Constants.TEST_FILTER.SUCCEEDED]
    this.showFailed = choices[Constants.TEST_FILTER.FAILED]
    this.showIncomplete = choices[Constants.TEST_FILTER.INCOMPLETE]
    this.showOptional = choices[Constants.TEST_FILTER.OPTIONAL]
    this.showDisabled = choices[Constants.TEST_FILTER.DISABLED]
    let tagKeys: string[]|undefined
    let untagged: boolean|undefined
    if (this.tagsLoaded) {
      this.includeUntagged = this.untaggedAvailable ? (choices[Constants.TEST_FILTER.UNTAGGED] ?? true) : true
      const checkedTagKeys = new Set<string>()
      let allTagsChecked = true
      for (const tag of this.availableTags) {
        if (choices[Constants.TEST_FILTER_TAG_KEY_PREFIX + tag.key]) {
          checkedTagKeys.add(tag.key)
        } else {
          allTagsChecked = false
        }
      }
      this.selectedTagKeys = checkedTagKeys
      const nothingExcluded = allTagsChecked && (!this.untaggedAvailable || this.includeUntagged)
      if (!nothingExcluded) {
        tagKeys = Array.from(checkedTagKeys)
        untagged = this.includeUntagged
      }
    }
    this.apply.emit({
      showSuccessful: this.showSuccessful,
      showFailed: this.showFailed,
      showIncomplete: this.showIncomplete,
      showOptional: this.showOptional,
      showDisabled: this.showDisabled,
      tagKeys,
      untagged
    })
  }

  protected readonly Constants = Constants;
}
