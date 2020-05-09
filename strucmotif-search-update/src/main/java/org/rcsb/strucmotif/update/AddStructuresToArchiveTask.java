package org.rcsb.strucmotif.update;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.CifFile;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.io.write.StructureWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates dedicated, reduced representation of all structure files. Prepares structures to be added to the index.
 */
class AddStructuresToArchiveTask {
    private static final Logger logger = LoggerFactory.getLogger(AddStructuresToArchiveTask.class);
    private static final String TASK_NAME = AddStructuresToArchiveTask.class.getSimpleName();

    AddStructuresToArchiveTask(String[] args, StructureWriter<CifFile> renumberedWriter) throws IOException {
        logger.info("[{}] starting structural motif search archive update",
                TASK_NAME);

        List<String> identifiers = List.of(args);

        AtomicInteger counter = new AtomicInteger();
        int target = identifiers.size();

        // write structure
        FileWriter processedWriter = new FileWriter(MotifSearch.ARCHIVE_LIST.toFile(), true);
        identifiers.parallelStream()
                .forEach(id -> {
                    try {
                        logger.info("[{} / {}] renumbering {}",
                                counter.incrementAndGet(),
                                target,
                                id);

                        // ensure directories exist
                        Files.createDirectories(MotifSearch.ARCHIVE_PATH);

                        CifFile cifFile = CifIO.readById(id);
                        renumberedWriter.write(cifFile);
                        // need to concat externally to prevent malformed output (if ever used in parallel)
                        String concat = id + System.lineSeparator();
                        processedWriter.append(concat);
                        processedWriter.flush();
                    } catch (IOException e) {
                        logger.warn("[{} / {}] {} failed - no source file @ RCSB",
                                counter.get(),
                                target,
                                id, e);
                    }
                });

        processedWriter.close();

        logger.info("[{}] finished archive update",
                TASK_NAME);
    }
}
