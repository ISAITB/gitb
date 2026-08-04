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

import { Directive, inject, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavigationTarget } from '../types/navigation-target';

/**
 * Applies a `NavigationTarget` (as produced by the `RoutingService.linkToXyz()` methods) to the
 * host `<a>` element by driving Angular's own `RouterLink` (hosted on the same element). This
 * keeps templates from having to bind `routerLink`/`queryParams`/`state`/`replaceUrl` separately
 * every time a control navigates, and ensures the resulting `href` is built through the router
 * (required since the app uses hash-based routing - see `app-routing.module.ts`).
 */
@Directive({
    selector: 'a[navTarget]',
    standalone: true,
    hostDirectives: [RouterLink]
})
export class NavTargetDirective {

    private readonly routerLink = inject(RouterLink)

    /**
     * Accepts `undefined` for controls whose target is only known once some prerequisite data has
     * loaded (in which case the control is expected to also be marked [disable]d until then) - the
     * anchor is simply left without a destination in that case, matching a disabled control.
     */
    @Input({ required: true }) set navTarget(target: NavigationTarget|undefined) {
        if (target == undefined) {
            this.routerLink.routerLink = null
            return
        }
        // RoutingService's command arrays are all absolute (root-relative) path segments, matching
        // Router.navigate()'s default. RouterLink, unlike Router.navigate(), otherwise resolves a
        // commands array relative to the ActivatedRoute it's created in - so without forcing
        // relativeTo to null here, e.g. ['admin','domains','create'] bound from within
        // /admin/domains would resolve to /admin/domains/admin/domains/create instead of
        // /admin/domains/create.
        this.routerLink.relativeTo = null
        this.routerLink.routerLink = target.commands
        this.routerLink.queryParams = target.extras?.queryParams
        this.routerLink.state = target.extras?.state
        this.routerLink.replaceUrl = target.extras?.replaceUrl ?? false
    }

}
