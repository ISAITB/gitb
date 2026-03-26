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

import {Directive, ElementRef, HostListener} from '@angular/core';

@Directive({
  selector: 'textarea[ctrlEnterSubmit]'
})
export class SubmitOnControlEnterDirective {

  constructor(private readonly element: ElementRef<HTMLTextAreaElement>) { }

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    // Detect Ctrl + Enter
    if (event.ctrlKey && event.key === 'Enter') {
      event.preventDefault();
      // Find nearest <form>
      const form = this.element.nativeElement.closest('form');
      // Submit like a real Enter key
      if (form) {
        form.requestSubmit();
      }
    }
  }

}
