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

package com.gitb.engine;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the request payload (body, or a multipart part) of an incoming HTTP/SOAP call for as long as it is held
 * awaiting a matching {@code receive} step (see {@link CallbackManager#lookupHandlingData}). A held call may sit
 * for up to {@link TestEngineConfiguration#CALLBACK_WAIT_TIMEOUT} and there may be many of them concurrently
 * (bounded by {@link TestEngineConfiguration#CALLBACK_WAIT_LIMIT}), so - unlike a call that is matched immediately
 * and whose payload is consumed microseconds later - a held payload is, when
 * {@link TestEngineConfiguration#TEMP_CALLBACK_STORAGE_ENABLED} allows it, spilled to disk rather than pinning
 * heap for the duration. Callers only ever see a {@link PayloadRef}; where the bytes actually live is an
 * implementation detail resolved on {@link #read(PayloadRef)}.
 */
public class CallbackPayloadStore {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackPayloadStore.class);
    private static final CallbackPayloadStore INSTANCE = new CallbackPayloadStore();
    private static final PayloadRef EMPTY = new PayloadRef(new UUID(0L, 0L), 0, false);

    private final Map<UUID, byte[]> inMemoryPayloads = new ConcurrentHashMap<>();
    private final Path storageLocation;

    public record PayloadRef(UUID id, long size, boolean onDisk) {
    }

    private CallbackPayloadStore() {
        storageLocation = Path.of(TestEngineConfiguration.TEMP_CALLBACK_STORAGE_LOCATION);
        if (TestEngineConfiguration.TEMP_CALLBACK_STORAGE_ENABLED) {
            try {
                Files.createDirectories(storageLocation);
                FileUtils.cleanDirectory(storageLocation.toFile());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to prepare the incoming call payload storage folder", e);
            }
        }
    }

    public static CallbackPayloadStore getInstance() {
        return INSTANCE;
    }

    /**
     * Stores the fully-read content of the given stream and returns a reference to it. When {@code spillToDisk}
     * is set and disk storage is enabled the content is streamed straight to a file (it never exists as a whole
     * in memory); otherwise it is kept in a heap-backed map. An empty stream stores nothing and yields a shared
     * {@link PayloadRef} that {@link #read(PayloadRef)} resolves to an empty array and {@link #release(PayloadRef)}
     * is a no-op for.
     */
    public PayloadRef store(InputStream in, boolean spillToDisk) throws IOException {
        if (spillToDisk && TestEngineConfiguration.TEMP_CALLBACK_STORAGE_ENABLED) {
            UUID id = UUID.randomUUID();
            Path target = storageLocation.resolve(id.toString());
            long size = Files.copy(in, target);
            if (size == 0) {
                Files.deleteIfExists(target);
                return EMPTY;
            }
            return new PayloadRef(id, size, true);
        } else {
            byte[] content = IOUtils.toByteArray(in);
            if (content.length == 0) {
                return EMPTY;
            }
            UUID id = UUID.randomUUID();
            inMemoryPayloads.put(id, content);
            return new PayloadRef(id, content.length, false);
        }
    }

    public byte[] read(PayloadRef ref) {
        if (ref.onDisk()) {
            try {
                return Files.readAllBytes(storageLocation.resolve(ref.id().toString()));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read a stored incoming call payload", e);
            }
        } else {
            return inMemoryPayloads.getOrDefault(ref.id(), new byte[0]);
        }
    }

    /**
     * Releases the storage (file or in-memory entry) backing the given reference. Safe to call more than once,
     * and safe to call from any completion path (served, dropped after the wait window, or an executor
     * rejection) since it is only ever the last thing done with a reference.
     */
    public void release(PayloadRef ref) {
        if (ref == null || ref == EMPTY) {
            return;
        }
        if (ref.onDisk()) {
            try {
                Files.deleteIfExists(storageLocation.resolve(ref.id().toString()));
            } catch (IOException e) {
                LOG.warn("Unable to delete a stored incoming call payload [{}]", ref.id(), e);
            }
        } else {
            inMemoryPayloads.remove(ref.id());
        }
    }

    /**
     * Releases everything currently stored. Called on engine shutdown.
     */
    public void destroy() {
        inMemoryPayloads.clear();
        if (TestEngineConfiguration.TEMP_CALLBACK_STORAGE_ENABLED) {
            try {
                if (Files.exists(storageLocation)) {
                    FileUtils.cleanDirectory(storageLocation.toFile());
                }
            } catch (IOException e) {
                LOG.warn("Unable to clean the incoming call payload storage folder", e);
            }
        }
    }

}
