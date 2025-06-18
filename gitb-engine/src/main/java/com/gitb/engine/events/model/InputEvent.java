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

package com.gitb.engine.events.model;

import com.gitb.tbs.UserInput;

import java.util.List;

/**
 * Created by tuncay on 9/23/14.
 */
public class InputEvent {
    private final String sessionId;
    private final String stepId;
    private final List<UserInput> userInputs;
    private final boolean admin;

    public InputEvent(String sessionId, String stepId, List<UserInput> userInputs, boolean admin) {
        this.sessionId = sessionId;
        this.stepId = stepId;
        this.userInputs = userInputs;
        this.admin = admin;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStepId() {
        return stepId;
    }

    public List<UserInput> getUserInputs() {
        return userInputs;
    }

    public boolean isAdmin() {
        return admin;
    }
}
