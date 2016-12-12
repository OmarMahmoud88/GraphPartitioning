package partitioning;

import java.util.Arrays;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.EigenDecomposition;

import structure.Graph;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomSet;
import utilities.ArrayIndexComparator;

public class SpectralPartitioningGraphCutFiedlerVector extends Partitioning {

	private int eigIndex = 1;

	public SpectralPartitioningGraphCutFiedlerVector(Graph graph, int numberOfPartitions, int numberOfTrials, float imbalanceRatio) {
		super(graph, numberOfPartitions, numberOfTrials, imbalanceRatio);
	}

	@Override
	public PartitionGroup getPartitions(Graph gr, RandomSet<Integer> graphSubset, int numberOfPartitions, int numberOfTries) {
		double[][] lapMatrix = gr.getLaplacianMatrix();
		double[] vertixWeightDiagMatrix = gr.getVertixWeightsDiagonalMatrix();
		
		// multiply the inverse of vertixWeightDiagMatrix with lapMatrix
		double[][] resultMatrix = new double[lapMatrix.length][lapMatrix.length];
		// observe that vertixWeightDiagMatrix is diagonal matrix
		// its inverse is 1/aii
		for (int i = 0; i < resultMatrix.length; i++) {
			double diagElement1 = ((double)1)/Math.sqrt(vertixWeightDiagMatrix[i]); // the matrix is not symmetric anymore
			//double diagElement = 1; 
			for (int j = 0; j < resultMatrix[i].length; j++) {
				double diagElement2 = ((double)1)/Math.sqrt(vertixWeightDiagMatrix[j]);
				resultMatrix[i][j] = diagElement1 * lapMatrix[i][j]*diagElement2;
			}
		}
//		System.out.println("Matrix Calculated");
		DenseMatrix64F rMat = new DenseMatrix64F(resultMatrix);
		
		EigenDecomposition<DenseMatrix64F> eig = DecompositionFactory.eig(resultMatrix.length, true);
		eig.decompose(rMat);
//		System.out.println("Matrix Decomposed");
		double[] eigValues = new double[eig.getNumberOfEigenvalues()];
		for (int i = 0; i < eigValues.length; i++) {
			eigValues[i] = eig.getEigenvalue(i).getReal();
		}
		ArrayIndexComparator eigenValuesComparator = new ArrayIndexComparator(eigValues);
		Integer[] eigenValuesindices = eigenValuesComparator.createIndexArray();
		Arrays.sort(eigenValuesindices, eigenValuesComparator);
//		System.out.println("Eigen Values Sorted");
		//Complex64F eigValue = eig.getEigenvalue(eigenValuesindices[this.eigIndex]);
		DenseMatrix64F eigVector = null;
		double[] eigenVector = null;
		try {
			eigVector = eig.getEigenVector(eigenValuesindices[this.eigIndex]);
			eigenVector = eigVector.getData();
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("eigenValuesindices[this.eigIndex] = " + eigenValuesindices[this.eigIndex]);
			System.out.println("number of eigens = " + eig.getNumberOfEigenvalues());
			System.out.println("this.eigIndex = " + this.eigIndex);
			System.out.println("eigVector = " + eigVector);
			System.out.println("eigenVector = " + eigenVector);
		}
//		System.out.println("Fiedler Vector Acquired.");
		ArrayIndexComparator eigenVectorComparator = new ArrayIndexComparator(eigenVector);
		Integer[] eigenVectorIndicies = eigenVectorComparator.createIndexArray();
		Arrays.sort(eigenVectorIndicies, eigenVectorComparator);
//		System.out.println("Fiedler Vector Sorted.");
//		System.out.println("eigenValue = " + eigValue);
//		System.out.println(eigVector);
		
		PartitionGroup partsGroup = new PartitionGroup(gr);
		int partitionID = 1;
		int vectorIndex = 0;
		int PartitionWeightThreshold = gr.getTotalNodesWeights()/numberOfPartitions;
		while (partsGroup.getPartitionNumber() < numberOfPartitions) {
			Partition part = new Partition(gr, partitionID);
			while(part.getPartitionWeight() < PartitionWeightThreshold && vectorIndex<eigenVectorIndicies.length){
				int nodeID = eigenVectorIndicies[vectorIndex]+1;
				part.addNode(nodeID);
				vectorIndex++;
			}
//			System.out.println("Partition " + partitionID + " completed.");
			partsGroup.addPartition(part);
			partitionID ++;
		}
		// add remaining nodes
		while(vectorIndex < eigenVectorIndicies.length){
			int nodeID = eigenVectorIndicies[vectorIndex]+1;
			partsGroup.getPartition(numberOfPartitions).addNode(nodeID);
			vectorIndex ++;
		}
		return partsGroup;
	}

	public int getEigIndex() {
		return eigIndex;
	}

	public void setEigIndex(int eigIndex) {
		this.eigIndex = eigIndex;
	}

}
