package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class MotifSearchRuntimeImpl implements MotifSearchRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchRuntimeImpl.class);
    private final TargetAssembler targetAssembler;
    private final AlignmentService alignment;
    private final ThreadPool threadPool;
    private final MotifSearchConfig motifSearchConfig;

    @Autowired
    public MotifSearchRuntimeImpl(TargetAssembler targetAssembler, AlignmentService alignmentService, ThreadPool threadPool, MotifSearchConfig motifSearchConfig) {
        this.targetAssembler = targetAssembler;
        this.alignment = alignmentService;
        this.threadPool = threadPool;
        this.motifSearchConfig = motifSearchConfig;
    }

    @Override
    public MotifSearchResult performSearch(MotifSearchQuery query) {
        try {
            QueryStructure queryStructure = query.getQueryStructure();

            // all motifs which can be formed from this query
            List<ResiduePairDescriptor> queryResiduePairDescriptors = queryStructure.getResiduePairDescriptors();

            if (queryResiduePairDescriptors.isEmpty()) {
                throw new IllegalArgumentException("did not find any valid motifs in " + queryStructure.getStructure().getStructureIdentifier() +
                        " - maybe distance cutoff (" + motifSearchConfig.getDistanceCutoff() + ") exceeded? - maybe wrong selection?");
            }

            Parameters parameters = query.getParameters();
            logger.info("Query: {}, tolerances: [{}, {}, {}], exchanges: {}",
                    queryResiduePairDescriptors,
                    parameters.getBackboneDistanceTolerance(),
                    parameters.getSideChainDistanceTolerance(),
                    parameters.getAngleTolerance(),
                    query.getExchanges());

            MotifSearchResult result = new MotifSearchResult(query);

            // get all valid targets
            targetAssembler.assemble(result);

            HitScorer hitScorer = new RootMeanSquareDeviationHitScorer(queryStructure, parameters.getRmsdCutoff(), parameters.getAtomPairingScheme(), alignment);
            List<Hit> hits = scoreHits(parameters, result, hitScorer);
            logger.info("Accepted {} hits in {} ms",
                    hits.size(),
                    result.getTimings().getStructuresTime());

            // dereference target structure map
            result.getTargetStructures().clear();
            result.setTargetStructures(null);

            result.setHits(hits);
            result.getTimings().queryStop();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Hit> scoreHits(Parameters parameters, MotifSearchResult result, HitScorer hitScorer) throws ExecutionException, InterruptedException {
        result.getTimings().structuresStart();
        int limit = Math.min(parameters.getLimit(), motifSearchConfig.getMaxResults());
        List<Hit> hits = threadPool.submit(() -> result.getTargetStructures()
                .values()
                .parallelStream()
                .flatMap(targetStructure -> targetStructure.paths().map(path -> hitScorer.score(targetStructure, path)))
                // hits filtered by by threshold are reported as null
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList()))
                .get();
        result.getTimings().structuresStop();
        return hits;
    }
}
