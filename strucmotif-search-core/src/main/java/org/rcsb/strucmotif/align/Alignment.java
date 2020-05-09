package org.rcsb.strucmotif.align;

import org.rcsb.strucmotif.domain.AlignmentResult;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

public interface Alignment {
    /**
     * Align 2 sets of residues to one another using quaternions.
     * @param reference the reference set of residues
     * @param candidate the candidate set of residues to evaluate
     * @param alignmentScheme the atom names to consider for each residue during alignment
     * @return an Alignment instance which provides the aligned instances, transformation operations and scores
     */
    AlignmentResult align(List<Residue> reference, List<Residue> candidate, AtomPairingScheme alignmentScheme);
}
