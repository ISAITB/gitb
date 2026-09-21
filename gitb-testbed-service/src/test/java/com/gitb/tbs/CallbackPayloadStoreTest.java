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

package com.gitb.tbs;

import com.gitb.engine.CallbackPayloadStore;
import com.gitb.engine.TestEngineConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct round-trip tests for {@link CallbackPayloadStore}, the store used to hold an incoming HTTP/SOAP call's
 * payload while it is parked awaiting a matching {@code receive} step (see {@link CallbackHoldingServletTest} for
 * the same behaviour exercised end-to-end through the real controllers).
 */
class CallbackPayloadStoreTest extends BaseIntegrationTest {

    private final CallbackPayloadStore store = CallbackPayloadStore.getInstance();

    @Test
    void diskBackedContentRoundTripsIntactAndIsRemovedOnRelease() throws Exception {
        byte[] content = randomBytes(2 * 1024 * 1024); // 2MB - large enough that an in-memory copy would be wasteful while held.
        var ref = store.store(new ByteArrayInputStream(content), true);
        try {
            assertTrue(ref.onDisk(), "A spilled payload should be recorded as stored on disk");
            assertEquals(content.length, ref.size());
            Path storedFile = Path.of(TestEngineConfiguration.TEMP_CALLBACK_STORAGE_LOCATION, ref.id().toString());
            assertTrue(Files.exists(storedFile), "Expected the spilled payload to exist as a file");
            assertArrayEquals(content, store.read(ref), "Content read back from disk should be identical to what was stored");
        } finally {
            store.release(ref);
        }
        Path storedFile = Path.of(TestEngineConfiguration.TEMP_CALLBACK_STORAGE_LOCATION, ref.id().toString());
        assertFalse(Files.exists(storedFile), "Expected the file to be removed once released");
    }

    @Test
    void inMemoryContentRoundTripsIntactWithoutTouchingDisk() throws Exception {
        byte[] content = "small in-memory payload".getBytes();
        var ref = store.store(new ByteArrayInputStream(content), false);
        try {
            assertFalse(ref.onDisk(), "A payload stored without spilling should not be recorded as on disk");
            Path wouldBeFile = Path.of(TestEngineConfiguration.TEMP_CALLBACK_STORAGE_LOCATION, ref.id().toString());
            assertFalse(Files.exists(wouldBeFile), "An in-memory payload should never have a backing file");
            assertArrayEquals(content, store.read(ref));
        } finally {
            store.release(ref);
        }
    }

    @Test
    void emptyContentYieldsASharedRefWithNoBackingStorageEitherWay() throws Exception {
        var diskRef = store.store(InputStream.nullInputStream(), true);
        var memoryRef = store.store(InputStream.nullInputStream(), false);
        assertEquals(0, diskRef.size());
        assertEquals(0, memoryRef.size());
        assertFalse(diskRef.onDisk(), "An empty payload should never actually be spilled to disk, regardless of the spill flag");
        assertArrayEquals(new byte[0], store.read(diskRef));
        // Releasing (possibly more than once) a ref backed by nothing must be a safe no-op.
        store.release(diskRef);
        store.release(diskRef);
        store.release(memoryRef);
    }

    @Test
    void spillIsSkippedWhenDiskStorageIsDisabled() throws Exception {
        boolean original = TestEngineConfiguration.TEMP_CALLBACK_STORAGE_ENABLED;
        TestEngineConfiguration.TEMP_CALLBACK_STORAGE_ENABLED = false;
        try {
            byte[] content = randomBytes(1024);
            var ref = store.store(new ByteArrayInputStream(content), true);
            try {
                assertFalse(ref.onDisk(), "Spilling should be skipped in favour of memory when disk storage is disabled");
                assertArrayEquals(content, store.read(ref));
            } finally {
                store.release(ref);
            }
        } finally {
            TestEngineConfiguration.TEMP_CALLBACK_STORAGE_ENABLED = original;
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}
