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

package com.gitb.vs.tdl;

import com.gitb.vs.tdl.rules.AbstractCheck;
import com.gitb.vs.tdl.rules.RuleFactory;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Class that implements the validator's logic irrespective of the API used.
 */
public class Validator {

    private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

    private final String tmpFolderPath;
    private final ExternalConfiguration externalConfiguration;

    private ValidationReport report = new ValidationReport();

    public Validator(String tmpFolderPath, ExternalConfiguration externalConfiguration) {
        this.tmpFolderPath = tmpFolderPath;
        this.externalConfiguration = externalConfiguration;
    }

    private Path storeTestSuite(InputStream testSuite) throws IOException {
        String uuid = UUID.randomUUID().toString();
        File tempFolder = Paths.get(tmpFolderPath, uuid).toFile();
        tempFolder.mkdirs();
        if (Utils.unzip(testSuite, tempFolder)) {
            return tempFolder.toPath();
        }
        return null;
    }

    /**
     * Validate the input.
     *
     * @param testSuite The test suite to validate.
     * @return The result of the validation.
     */
    public ValidationReport validate(InputStreamSource testSuite) {
        // Basic prerequisite processing.
        Path testSuitePath = null;
        try {
            Context context = new Context();
            boolean archiveOk = false;
            try (InputStream is =  testSuite.getInputStream()) {
                if (checkFileType(testSuite)) {
                    // Extract the test suite.
                    testSuitePath = storeTestSuite(is);
                    if (testSuitePath != null) {
                        archiveOk = true;
                        context.setTestSuiteRootPath(testSuitePath);
                        // Set additional context values for the validation.
                        context.setExternalConfiguration(externalConfiguration);
                        for (AbstractCheck check: RuleFactory.getInstance().getChecks()) {
                            check.doCheck(context, report);
                        }
                    }
                }
            } catch (IOException e) {
                archiveOk = false;
                LOG.warn("Unexpected failure while parsing test suite file", e);
            }
            if (!archiveOk) {
                report.addItem(ErrorCode.INVALID_ZIP_ARCHIVE, "");
            }
        } finally {
            // Cleanup.
            if (testSuitePath != null) {
                FileUtils.deleteQuietly(testSuitePath.toFile());
            }
        }
        return report.sort();
    }

    public boolean checkFileType(InputStreamSource streamSource) {
        Tika tika = new Tika();
        try (InputStream is = streamSource.getInputStream()) {
            String type = tika.detect(is);
            return externalConfiguration.getAcceptedMimeTypes().contains(type);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
