package org.rcsb.strucmotif.update;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.io.read.RenumberedReader;
import org.rcsb.strucmotif.io.write.StructureWriter;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.MongoResidueDB;
import org.rcsb.strucmotif.persistence.MongoTitleDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MotifSearchUpdate {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchUpdate.class);
    private static final String TASK_NAME = MotifSearchUpdate.class.getSimpleName();
    private static final String RCSB_ENTRY_LIST = "http://www.rcsb.org/pdb/json/getCurrent";

    enum Context {
        ARCHIVE,
        RESIDUE,
        INDEX
    }

    public static void main(String[] args) throws IOException {
        Injector injector = Guice.createInjector(MotifSearch.MOTIF_SEARCH_MODULE);
        StructureWriter renumberedWriter = injector.getInstance(StructureWriter.class);
        RenumberedReader renumberedReader = injector.getInstance(RenumberedReader.class);
        InvertedIndex motifLookup = injector.getInstance(InvertedIndex.class);
        MongoResidueDB residueDB = injector.getInstance(MongoResidueDB.class);
        MongoTitleDB titleDB = injector.getInstance(MongoTitleDB.class);

        String[] full = getFullIdentifierList();

        // perform all steps that add structures to archive, database, or lookup
        for (Context context : Context.values()) {
            String[] ids = getDeltaPlusIdentifierList(full, context);
                logger.info("[{}] Incremental mode - {} entries ({})",
                        TASK_NAME,
                        ids.length,
                        Arrays.toString(ids));

            switch (context) {
                case ARCHIVE:
                    new AddStructuresToArchiveTask(ids, renumberedWriter);
                    break;
                case RESIDUE:
                    if (!MotifSearch.NO_MONGO_DB) {
                        new AddStructuresToStructureDBTask(ids, renumberedReader, residueDB, titleDB);
                    }
                    break;
                case INDEX:
                    new AddStructuresToInvertedIndexTask(ids, motifLookup, renumberedReader);
                    break;
            }
        }

        // perform all steps that remove structures from archive, database, or lookup
        for (Context context : Context.values()) {
            String[] ids = getDeltaMinusIdentifierList(getFullIdentifierList(), context);
            logger.info("[{}] Incremental mode - {} entries ({})",
                    TASK_NAME,
                    ids.length,
                    Arrays.toString(ids));

            switch (context) {
                case ARCHIVE:
//                    new RemoveStructuresFromArchiveTask(ids);
                    break;
                case RESIDUE:
                    if (!MotifSearch.NO_MONGO_DB) {
//                        new RemoveStructuresFromStructureDBTask(ids, residueDB, titleDB);
                    }
                    break;
                case INDEX:
//                    new RemoveStructuresFromInvertedIndexTask(ids, motifLookup);
                    break;
            }
        }
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param fullIdentifierList the full collection of known IDs reported by RCSB
     * @param context currently performed operation
     * @return array of IDs that need to be processed for the given context
     */
    private static String[] getDeltaPlusIdentifierList(String[] fullIdentifierList, Context context) {
        try {
            if (context == Context.ARCHIVE) {
                Set<String> known = Files.lines(MotifSearch.ARCHIVE_LIST)
                        .collect(Collectors.toSet());
                return Stream.of(fullIdentifierList)
                        .map(String::toLowerCase)
                        .filter(id -> !known.contains(id))
                        .toArray(String[]::new);
            } else if (context == Context.INDEX) {
                Set<String> known = Files.lines(MotifSearch.LOOKUP_LIST)
                        .collect(Collectors.toSet());
                return Stream.of(fullIdentifierList)
                        .map(String::toLowerCase)
                        .filter(id -> !known.contains(id))
                        .toArray(String[]::new);
            } else if (context == Context.RESIDUE) {
                Set<String> known = Files.lines(MotifSearch.RESIDUE_LIST)
                        .collect(Collectors.toSet());
                return Stream.of(fullIdentifierList)
                        .map(String::toLowerCase)
                        .filter(id -> !known.contains(id))
                        .toArray(String[]::new);
            } else {
                throw new UnsupportedOperationException("context " + context + " not supported");
            }
        } catch (IOException e) {
            logger.warn("[{}] No existing data - starting from scratch",
                    TASK_NAME);
            return Stream.of(fullIdentifierList)
                    .map(String::toLowerCase)
                    .toArray(String[]::new);
        }
    }

    /**
     * Determine all IDs that need to be removed from the archive.
     * @param fullIdentifierList the full collection of known IDs reported by RCSB
     * @param context currently performed operation
     * @return array of IDs that need to be remove for the given context
     */
    private static String[] getDeltaMinusIdentifierList(String[] fullIdentifierList, Context context) {
        Set<String> fil = Stream.of(fullIdentifierList).map(String::toLowerCase).collect(Collectors.toSet());
        try {
            if (context == Context.ARCHIVE) {
                return Files.lines(MotifSearch.ARCHIVE_LIST)
                        .map(String::toLowerCase)
                        .filter(id -> !fil.contains(id))
                        .toArray(String[]::new);
            } else if (context == Context.INDEX) {
                return Files.lines(MotifSearch.LOOKUP_LIST)
                        .map(String::toLowerCase)
                        .filter(id -> !fil.contains(id))
                        .toArray(String[]::new);
            } else if (context == Context.RESIDUE) {
                return Files.lines(MotifSearch.RESIDUE_LIST)
                        .map(String::toLowerCase)
                        .filter(id -> !fil.contains(id))
                        .toArray(String[]::new);
            } else {
                throw new UnsupportedOperationException("context " + context + " not supported");
            }
        } catch (IOException e) {
            logger.warn("[{}] No existing data - no need for cleanup of obsolete entries",
                    TASK_NAME);
            return new String[0];
        }
    }

    private static String[] getFullIdentifierList() throws IOException {
        GetCurrentResponse response;
        try (InputStream inputStream = new URL(RCSB_ENTRY_LIST).openStream()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                response = new Gson().fromJson(inputStreamReader, GetCurrentResponse.class);
            }
        }
        return response.getIdList();
    }

    @SuppressWarnings("unused")
    static class GetCurrentResponse {
        private int resultCount;
        private String[] idList;

        int getResultCount() {
            return resultCount;
        }

        void setResultCount(int resultCount) {
            this.resultCount = resultCount;
        }

        String[] getIdList() {
            return idList;
        }

        void setIdList(String[] idList) {
            this.idList = idList;
        }
    }
}
