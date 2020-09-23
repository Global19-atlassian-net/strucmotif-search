package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.ResidueSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.StructureReader;
import org.rcsb.strucmotif.io.write.RenumberedStructureWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

@Service
public class StructureDataProviderImpl implements StructureDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureDataProviderImpl.class);
    private final StructureReader structureReader;
    private final RenumberedStructureWriter renumberedStructureWriter;
    private final MotifSearchConfig motifSearchConfig;

    @Autowired
    public StructureDataProviderImpl(StructureReader structureReader, RenumberedStructureWriter renumberedStructureWriter, MotifSearchConfig motifSearchConfig) {
        this.structureReader = structureReader;
        this.renumberedStructureWriter = renumberedStructureWriter;
        this.motifSearchConfig = motifSearchConfig;

        boolean dataSourceHealthy;
        try {
            dataSourceHealthy = Files.list(Paths.get(motifSearchConfig.getDataSource()))
                    .anyMatch(path -> path.toFile().getName().endsWith(".bcif.gz"));
        } catch (IOException e) {
            dataSourceHealthy = false;
        }
        logger.info("BinaryCIF data source at {} looks healthy: {}",
                motifSearchConfig.getDataSource(),
                dataSourceHealthy);
    }

    private URL getBcifFetchUrl(StructureIdentifier structureIdentifier) {
        try {
            return new URL(String.format(motifSearchConfig.getBcifFetchUrl(), structureIdentifier.getPdbId().toLowerCase()));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path getOriginalStructurePath(StructureIdentifier structureIdentifier) {
        return Paths.get(motifSearchConfig.getDataSource())
                .resolve(structureIdentifier.getPdbId().toLowerCase() + ".bcif.gz");
    }

    private Path getRenumberedStructurePath(StructureIdentifier structureIdentifier) {
        return Paths.get(motifSearchConfig.getRootPath())
                .resolve("renumbered")
                .resolve(structureIdentifier.getPdbId().toLowerCase() + ".bcif.gz");
    }

    private InputStream getRenumberedInputStream(StructureIdentifier structureIdentifier) {
        try {
            return Files.newInputStream(getRenumberedStructurePath(structureIdentifier));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream getOriginalInputStream(StructureIdentifier structureIdentifier) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            if (Files.exists(originalPath)) {
                return Files.newInputStream(originalPath);
            } else {
                return getBcifFetchUrl(structureIdentifier).openStream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<? extends ResidueSelection> selection) {
        return structureReader.readFromInputStream(inputStream, selection);
    }

    @Override
    public Structure readRenumbered(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        return readFromInputStream(getRenumberedInputStream(structureIdentifier), selection);
    }

    @Override
    public Structure readOriginal(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        return readFromInputStream(getOriginalInputStream(structureIdentifier), selection);
    }

    @Override
    public Structure readSome(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            return readFromInputStream(Files.newInputStream(originalPath), selection);
        } catch (IOException e1) {
            try {
                Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
                return readFromInputStream(Files.newInputStream(renumberedPath), selection);
            } catch (IOException e2) {
                try {
                    return readFromInputStream(getBcifFetchUrl(structureIdentifier).openStream(), selection);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    @Override
    public void writeRenumbered(StructureIdentifier structureIdentifier, MmCifFile mmCifFile) {
        // ensure directories exist
        try {
            Files.createDirectories(Paths.get(motifSearchConfig.getRootPath()).resolve("renumbered"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        renumberedStructureWriter.write(mmCifFile, getRenumberedStructurePath(structureIdentifier));
    }

    @Override
    public void deleteRenumbered(StructureIdentifier structureIdentifier) {
        try {
            Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
            if (Files.exists(renumberedPath)) {
                Files.delete(getRenumberedStructurePath(structureIdentifier));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
