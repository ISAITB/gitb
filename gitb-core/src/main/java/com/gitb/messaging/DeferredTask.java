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

package com.gitb.messaging;

import java.util.function.Function;

public record DeferredTask<T>(T state, Function<T, Result<T>> executionHandler, Function<T, MessagingReport> expiryHandler, Long nextExecutionDelay) {

    public DeferredTask<T> withNewDelay(Long newExecutionDelay) {
        return new DeferredTask<>(state, executionHandler, expiryHandler, newExecutionDelay);
    }

    public DeferredTask<T> withNewState(T newState, Long newExecutionDelay) {
        return new DeferredTask<>(newState, executionHandler, expiryHandler, newExecutionDelay);
    }

    public record Result<T>(MessagingReport report, Long nextExecutionDelay, T nextState) {}

}
