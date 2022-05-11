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
