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

package com.gitb.engine.validation.handlers.common;

import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;

/**
 * Created by senan on 10/10/14.
 */
public abstract class AbstractReportHandler {

    protected final TAR report;
    protected final ObjectFactory objectFactory;

    protected AbstractReportHandler() {
        objectFactory = new ObjectFactory();
        report = TestCaseUtils.createEmptyReport();
    }

    public abstract TAR createReport();
}
