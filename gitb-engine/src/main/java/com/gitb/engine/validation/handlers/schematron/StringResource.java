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

package com.gitb.engine.validation.handlers.schematron;

import com.helger.commons.io.resource.IReadableResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Default phloc IReadableResource implementations are limited to ClassPath, FileSystem and URL resources.
 * Since GITB engine can provide Schematron resources as Strings (or Streams), current implementations are
 * not sufficient for Schematron processing. This class implements IReadableResource interface to overcome
 * the limitations by treating Strings as readable resources.
 */
public class StringResource implements IReadableResource {

    private final String resource;
    private final String path;

    public StringResource(String resource, String path) {
        this.resource = resource;
        this.path     = path;
    }

    @Nonnull
    @Override
    public IReadableResource getReadableCloneForPath(@Nonnull String s) {
        return new StringResource(s,null);
    }

    @Nullable
    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.resource.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isReadMultiple() {
        return false;
    }

    @Nonnull
    @Override
    public String getResourceID() {
        return path;
    }

    @Nonnull
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Nullable
    @Override
    public URL getAsURL() {
        return null;
    }

    @Nullable
    @Override
    public File getAsFile() {
        return null;
    }

}
