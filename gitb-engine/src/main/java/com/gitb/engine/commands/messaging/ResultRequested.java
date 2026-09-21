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

package com.gitb.engine.commands.messaging;

import com.gitb.messaging.Message;

import java.util.concurrent.CompletableFuture;

/**
 * Sent to a {@code receive} step's actor (one whose TDL step defines a {@code result} element) once its matching
 * incoming call has actually arrived. Carries the parsed incoming request so it can be run through the step's
 * {@code result/steps} and {@code result/output}, and a handle through which the resolved response is to be
 * communicated back to the messaging servlet awaiting it.
 */
public record ResultRequested(Message request, CompletableFuture<Message> responseHandle) {
}
