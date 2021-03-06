package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.rcsb.strucmotif.math.Algebra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A transformation described by a 4x4 matrix.
 */
public class Transformation {
    public static final double[][] IDENTITY_MATRIX_4D = new double[][] {
            { 1, 0, 0, 0 },
            { 0, 1, 0, 0 },
            { 0, 0, 1, 0 },
            { 0, 0, 0, 1 }
    };
    private final double[][] rotation;
    private final double[] translation;
    private final double[][] transformation;

    public Transformation(double[] translation, double[][] rotation) {
        this(translation, rotation, Algebra.composeTransformationMatrix(rotation, translation));
    }

    public Transformation(double[] translation, double[][] rotation, double[][] transformation) {
        this.translation = translation;
        this.rotation = rotation;
        this.transformation = transformation;
    }

    public double[][] getRotation() {
        return rotation;
    }

    public double[] getTranslation() {
        return translation;
    }

    /**
     * The actual transformation matrix.
     * @return a 4x4 transformation matrix
     */
    public double[][] getTransformationMatrix() {
        return transformation;
    }

    /**
     * The flattened transformation operation (row-major indexing).
     * @return a vector of 16 values
     */
    public double[] getFlattenedTransformation() {
        // flatten transformation into 1d array
        double[] out = new double[16];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(transformation[i], 0, out, i * 4, 4);
        }
        return out;
    }

    /**
     * Transform a collection of residues.
     * @param residues the residues to transform
     * @return a transformed collection
     */
    public List<Residue> transformComponents(List<Residue> residues) {
        List<Residue> values = new ArrayList<>(residues.size());
        for (Residue residue : residues) {
            values.add(transformComponent(residue));
        }
        return values;
    }

    /**
     * Transform a residue.
     * @param residue the residue to transform
     * @return a transformed instance
     */
    public Residue transformComponent(Residue residue) {
        return StructureFactory.createResidue(residue.getResidueIdentifier(), residue.getAtoms(), transformation);
    }

    /**
     * Transform a collection of atoms.
     * @param atoms the atoms to transform
     * @return a transformed collection
     */
    public List<Atom> transformAtoms(List<Atom> atoms) {
        List<Atom> values = new ArrayList<>(atoms.size());
        for (Atom atom : atoms) {
            values.add(transformAtom(atom));
        }
        return values;
    }

    /**
     * Transform an atom.
     * @param atom the atom to transform
     * @return a transformed instance
     */
    public Atom transformAtom(Atom atom) {
        return StructureFactory.createAtom(atom.getAtomIdentifier(), Algebra.multiply4d(transformation, atom.getCoord()));
    }

    /**
     * Transform a collection of vectors.
     * @param vectors the vectors to transform
     * @return a transformed collection
     */
    public List<double[]> transformVectors(List<double[]> vectors) {
        List<double[]> values = new ArrayList<>(vectors.size());
        for (double[] vector : vectors) {
            values.add(transformVector(vector));
        }
        return values;
    }

    /**
     * Transform a vector.
     * @param vector the vector to transform
     * @return a transformed instance
     */
    public double[] transformVector(double[] vector) {
        return Algebra.multiply4d(transformation, vector);
    }

    /**
     * <code>true</code> if this is the identity matrix
     * @return a boolean
     */
    public boolean isNeutral() {
        return Arrays.deepEquals(IDENTITY_MATRIX_4D, transformation);
    }
}
