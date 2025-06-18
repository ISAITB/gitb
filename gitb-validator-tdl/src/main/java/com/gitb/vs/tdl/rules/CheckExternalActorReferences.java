/*
 * Copyright (C) 2025 European Union
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

package com.gitb.vs.tdl.rules;

import com.gitb.core.Actor;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.ValidationReport;

public class CheckExternalActorReferences extends AbstractCheck {

    @Override
    public void doCheck(Context context, ValidationReport report) {
        for (Actor actor: context.getTestSuiteActors().values()) {
            if (actor.getId() != null && actor.getName() == null) {
                // This is a reference to an external actor.
                if (!context.getExternalConfiguration().getExternalActorIds().contains(actor.getId())) {
                    report.addItem(ErrorCode.INVALID_EXTERNAL_ACTOR_REFERENCE, getTestSuiteLocation(context), actor.getId());
                }
            }
        }
    }
}
