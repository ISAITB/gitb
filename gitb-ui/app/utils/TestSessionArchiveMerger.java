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

package utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Merges the contents of a test session status update folder (e.g. "status-updates/2025/8") into the ZIP archive
 * that would normally have replaced it (e.g. "status-updates/2025/8.zip").
 * <p>
 * This addresses the exceptional (and otherwise impossible, given the archival process' active session checks)
 * case where a month folder is found to already have a matching archive at the point it is due for archival.
 * Rather than skip archival and leave the folder's test session data effectively invisible to reporting (which
 * always prefers a folder over an archive when both exist), the folder's contents are merged into the archive:
 * <ul>
 *     <li>Files missing from the archive are added to it.</li>
 *     <li>Files present in both overwrite the archived copy, with one exception: session {@code log.txt} files are
 *     never simply overwritten. Instead, the merged log is the archived log's content followed by the folder's
 *     log content, so that no previously recorded log output is lost.</li>
 * </ul>
 * The merge is performed on a working copy of the archive which only replaces the original once fully built, so
 * that a failure part-way through (I/O error, process termination, etc.) never leaves the original archive
 * corrupted or partially updated.
 */
public class TestSessionArchiveMerger {

    private static final String LOG_FILE_NAME = "log.txt";
    private static final String MERGE_TMP_SUFFIX = ".merge.tmp";

    private final Path folderToMerge;
    private final Path existingArchive;
    private final Path workFolder;

    /**
     * Constructor.
     *
     * @param folderToMerge The status update month folder whose contents are to be merged into the archive.
     * @param existingArchive The existing ZIP archive to merge into (updated in place once the merge succeeds).
     * @param workFolder A folder that can be freely used for temporary files while the merge is in progress. Not
     *                   created and not deleted by this class - the caller is expected to manage its lifecycle
     *                   (e.g. as part of the periodic clean-up of temporary files).
     */
    public TestSessionArchiveMerger(Path folderToMerge, Path existingArchive, Path workFolder) {
        this.folderToMerge = folderToMerge;
        this.existingArchive = existingArchive;
        this.workFolder = workFolder;
    }

    /**
     * The outcome of a merge operation, reported so that it can be logged in detail.
     *
     * @param filesAdded Files present in the folder but not in the archive, and therefore added to it as is.
     * @param filesReplaced Files present in both, overwritten in the archive with the folder's version (excludes
     *                      log files, which are never simply overwritten - see filesLogMerged).
     * @param filesLogMerged Session log files ("log.txt") present in both, merged by appending the folder's log
     *                       content to the archive's existing log content.
     */
    public record MergeResult(int filesAdded, int filesReplaced, int filesLogMerged) {
    }

    /**
     * Perform the merge, updating the existing archive in place once complete.
     *
     * @return A summary of the changes applied.
     */
    public MergeResult merge() {
        try {
            Files.createDirectories(workFolder);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create merge work folder ["+workFolder+"].", e);
        }
        Path mergeArchive = existingArchive.resolveSibling(existingArchive.getFileName().toString()+MERGE_TMP_SUFFIX);
        Path logMergeFolder = workFolder.resolve("logs");
        try {
            // Start from a copy of the existing archive - all changes are applied to this copy so that the
            // original is only ever touched once, by the final atomic replace.
            Files.copy(existingArchive, mergeArchive, StandardCopyOption.REPLACE_EXISTING);
            int filesAdded;
            int filesReplaced;
            int filesLogMerged;
            try (ZipFile zipFile = new ZipFile(mergeArchive.toFile())) {
                Set<String> existingEntryNames = new HashSet<>();
                List<FileHeader> headers = zipFile.getFileHeaders();
                if (headers != null) {
                    for (FileHeader header: headers) {
                        if (!header.isDirectory()) {
                            existingEntryNames.add(header.getFileName());
                        }
                    }
                }
                // Classify every file in the folder to merge.
                List<Path> filesToAdd = new ArrayList<>();
                List<Path> filesToReplace = new ArrayList<>();
                List<Path> logsToMerge = new ArrayList<>();
                try (Stream<Path> walk = Files.walk(folderToMerge)) {
                    for (Path file: walk.filter(Files::isRegularFile).toList()) {
                        String entryName = toEntryName(folderToMerge, file);
                        if (!existingEntryNames.contains(entryName)) {
                            filesToAdd.add(file);
                        } else if (LOG_FILE_NAME.equals(file.getFileName().toString())) {
                            logsToMerge.add(file);
                        } else {
                            filesToReplace.add(file);
                        }
                    }
                }
                filesAdded = filesToAdd.size();
                filesReplaced = filesToReplace.size();
                filesLogMerged = logsToMerge.size();
                // Build the merged log files before removing the archived entries they are based on, and before
                // removing the archived entries so that their previous content can still be read.
                List<Path> mergedLogFiles = new ArrayList<>();
                if (!logsToMerge.isEmpty()) {
                    Files.createDirectories(logMergeFolder);
                    int index = 0;
                    for (Path folderLogFile: logsToMerge) {
                        String entryName = toEntryName(folderToMerge, folderLogFile);
                        Path mergedLogFile = logMergeFolder.resolve("log_"+(index++)+".txt");
                        mergeLogFile(zipFile, entryName, folderLogFile, mergedLogFile);
                        mergedLogFiles.add(mergedLogFile);
                    }
                }
                // Remove the archived entries that are being replaced (as-is files and logs), in a single call
                // so that the archive is rewritten once rather than once per removed entry.
                List<String> entriesToRemove = new ArrayList<>();
                for (Path file: filesToReplace) {
                    entriesToRemove.add(toEntryName(folderToMerge, file));
                }
                for (Path file: logsToMerge) {
                    entriesToRemove.add(toEntryName(folderToMerge, file));
                }
                if (!entriesToRemove.isEmpty()) {
                    zipFile.removeFiles(entriesToRemove);
                }
                // Add the new and replaced files as they are in the folder.
                for (Path file: filesToAdd) {
                    addFile(zipFile, file, toEntryName(folderToMerge, file));
                }
                for (Path file: filesToReplace) {
                    addFile(zipFile, file, toEntryName(folderToMerge, file));
                }
                // Add the merged log files under their original archive entry name.
                int index = 0;
                for (Path folderLogFile: logsToMerge) {
                    addFile(zipFile, mergedLogFiles.get(index++), toEntryName(folderToMerge, folderLogFile));
                }
            }
            // Only now that the merged copy is fully and successfully built, replace the original archive with it.
            replaceArchive(mergeArchive);
            return new MergeResult(filesAdded, filesReplaced, filesLogMerged);
        } catch (IOException e) {
            throw new IllegalStateException("Error while merging test session folder ["+folderToMerge+"] into existing archive ["+existingArchive+"].", e);
        } finally {
            FileUtils.deleteQuietly(mergeArchive.toFile());
            // The caller is expected to have passed a work folder unique to this invocation, so it is safe to
            // remove entirely rather than just its contents.
            FileUtils.deleteQuietly(workFolder.toFile());
        }
    }

    /**
     * Compute the ZIP entry name for a file relative to the folder being merged, matching the convention used by
     * {@link ZipArchiver} (forward slashes, no root folder prefix).
     */
    private String toEntryName(Path root, Path file) {
        return root.relativize(file).toString().replace(File.separatorChar, '/');
    }

    private void addFile(ZipFile zipFile, Path source, String entryName) throws ZipException {
        ZipParameters parameters = new ZipParameters();
        parameters.setFileNameInZip(entryName);
        parameters.setOverrideExistingFilesInZip(true);
        zipFile.addFile(source.toFile(), parameters);
    }

    /**
     * Build the merged log file content: the archived log's existing content followed by the folder's log content,
     * ensuring no data recorded in either is lost. A line separator is inserted between the two only if the
     * archived content is non-empty and does not already end with one, so that the folder's lines are never
     * concatenated onto the archive's last line.
     */
    private void mergeLogFile(ZipFile zipFile, String entryName, Path folderLogFile, Path mergedLogFile) throws IOException, ZipException {
        FileHeader header = zipFile.getFileHeader(entryName);
        try (OutputStream out = Files.newOutputStream(mergedLogFile)) {
            boolean archivedContentEndsWithNewline = true;
            if (header != null) {
                byte[] lastByte = null;
                try (InputStream in = zipFile.getInputStream(header)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    int totalRead = 0;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        totalRead += read;
                        if (read > 0) {
                            lastByte = new byte[] { buffer[read-1] };
                        }
                    }
                    archivedContentEndsWithNewline = totalRead == 0 || (lastByte != null && (lastByte[0] == '\n' || lastByte[0] == '\r'));
                }
            }
            if (!archivedContentEndsWithNewline) {
                out.write(System.lineSeparator().getBytes());
            }
            Files.copy(folderLogFile, out);
        }
    }

    /**
     * Atomically replace the existing archive with the merged copy where the file system allows it, otherwise
     * fall back to a plain (non-atomic) replace.
     */
    private void replaceArchive(Path mergeArchive) throws IOException {
        try {
            Files.move(mergeArchive, existingArchive, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.FileSystemException e) {
            // Some file systems do not support atomic moves in this location - fall back to a plain replace. The
            // merged copy is already fully built at this point, so this only narrows (rather than removes) the
            // failure window to the move itself.
            Files.move(mergeArchive, existingArchive, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
