/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;

/**
 * File-backed {@link ContentAccessor} writing generated files under a content root (the
 * JBake content tree) and sweeping previously generated ones.
 *
 * <p>4-Concerns: Location = filesystem (rooted at the content directory); Consumed Type =
 * generated content. Pure I/O, no business logic.
 *
 * @see ContentAccessor
 */
public class FileContentAccessor implements ContentAccessor {

    private final Path contentRoot;

    /**
     * @param contentRoot the JBake content directory to write into
     */
    public FileContentAccessor(Path contentRoot) {
        this.contentRoot = contentRoot;
    }

    @Override
    public void write(String relativePath, String content) {
        Path target = contentRoot.resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceParseException("Failed writing " + target, e);
        }
    }

    @Override
    public int sweep(String marker) {
        if (!Files.isDirectory(contentRoot)) {
            return 0;
        }
        List<Path> stale = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(contentRoot)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                if (p.toString().endsWith(".html") && Files.readString(p, StandardCharsets.UTF_8).contains(marker)) {
                    stale.add(p);
                }
            }
        } catch (IOException e) {
            throw new ResourceParseException("Failed sweeping " + contentRoot, e);
        }
        for (Path p : stale) {
            try {
                Files.delete(p);
            } catch (IOException e) {
                throw new ResourceParseException("Failed deleting " + p, e);
            }
        }
        return stale.size();
    }
}
