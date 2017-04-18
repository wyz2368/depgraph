/**
 * 
 */
package lpwrapper;

import java.io.OutputStream;
import java.util.List;

/**
 * Common interface for {@link MIProblem} solvers. So far {@link MIProblemCplex}
 * , {@link MIProblemGlpk}.
 * 
 * @author Manish Jain
 * 
 */
public abstract class AMIProblem {
	protected boolean isLoaded = false;
	
	protected ProblemType probType;
	protected ObjectiveType objectiveType;

	protected long genTime, loadTime, runTime;

	protected int numRows, numCols;

	public final int getNumRows() {
		return this.numRows;
	}

	public final int getNumCols() {
		return this.numCols;
	}

	protected abstract void setProblemType();

	protected abstract void setColBounds();

	protected abstract void setRowBounds();

	protected abstract void generateData();

	protected abstract void saveBasisStatus();

	protected abstract void loadBasisStatus();

	protected abstract void resetColumnBound(int columnNumber,
			BoundsType boundType, double lowerBound, double upperBound);

	protected abstract void resetRowBound(int rowNumber, BoundsType boundType,
			double lowerBound, double upperBound);
	
	public AMIProblem() {
		this.genTime = 0;
		this.loadTime = 0;
		this.runTime = 0;
		this.numRows = 0;
		this.numCols = 0;
	}

	protected abstract void initialize();

	protected abstract void setProblemName(final String name);

	protected abstract void setObjectiveCoef(final int index, final double value);

	protected abstract void setProblemType(final ProblemType problemType,
		final ObjectiveType objectiveType);

	public abstract void addAndSetColumn(final String name, final BoundsType boundType,
		final double lowerBound, final double upperBound, final VariableType varType,
		final double objCoeff);

	/**
	 * 
	 * @param name
	 * @param boundType
	 * @param lowerBound
	 * @param upperBound
	 * @return return the index of the row.
	 */
	public abstract int addAndSetRow(String name, BoundsType boundType,
		double lowerBound, double upperBound);

	/**
	 * adds a new column and initializes it
	 * 
	 * @param name
	 * @param varType
	 * @param objCoeff
	 * @param lowerBound
	 * @param upperBound
	 * @param indices
	 * @param values
	 * @throws RuntimeException
	 */
	public abstract void setMatCol(String name, VariableType varType,
		double objCoeff, double lowerBound, double upperBound,
		List<Integer> indices, List<Double> values) throws RuntimeException;

	/**
	 * Doesn't add a new row.
	 */
	public abstract void setMatRow(int rowNo, List<Integer> indices,
		List<Double> values) throws RuntimeException;

	public abstract void updateObjective() throws Exception;

	/**
	 * Calls setColBounds(), setRowBounds and generateData() Loads the problem
	 * specified from indices 1 to finalIndex (both included) in ia,ja and ar
	 * into the lp object.
	 */
	public final void loadProblem() {
		long start = System.currentTimeMillis();
		try {			
			this.setProblemType();
			this.setColBounds();
			this.setRowBounds();
			this.generateData();
			this.updateObjective();
			this.isLoaded = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.genTime = System.currentTimeMillis() - start;
	}

	/**
	 * cleans the data structures.
	 */
	public abstract void end();

	public final void resetLP() {
		this.numRows = 0;
		this.numCols = 0;
		this.end();
		this.initialize();
		this.loadProblem();
	}

	public abstract StatusType getSolveStatus();
	
	public abstract boolean disableRow(int rowNumber) throws RuntimeException;

	public abstract boolean enableRow(int rowNumber) throws RuntimeException;

	public abstract void solve() throws LPSolverException;

	public abstract double getRowDual(int rowNumber);

	public abstract double getRowSlack(int rowNumber);

	public abstract double getColumnPrimal(int columnNumber);

	public abstract List<Double> getRowDualVector();

	public abstract List<Double> getColumnPrimalVector();

	public abstract double getLPObjective();

	public abstract void writeProb(String fileName);

	public abstract void writeSol(String fileName);

	public final long getGenTime() {
		return this.genTime;
	}

	public final long getLoadTime() {
		return this.loadTime;
	}

	public final long getRunTime() {
		return this.runTime;
	}

	public abstract void redirectOutput(OutputStream stream);

	public enum StatusType {
		OPTIMAL, INFEASIBLE, UNBOUNDED, UNKNOWN, FEASIBLE	
	}
	
	public enum BoundsType {
		LOWER, UPPER, DOUBLE, FIXED, FREE
	}

	public enum VariableType {
		CONTINUOUS, INTEGER
	}

	public enum ProblemType {
		LP, MIP
	}

	public enum ObjectiveType {
		MIN, MAX
	}

	public enum RowStatus {
		ENABLED, DISABLED
	}

	public abstract int getNumberIterations();

	public abstract int getMIPStarts();

	public abstract int getNodesExplored();

	public abstract int getNodesLeft();

	public abstract int getSimplexIterations();

	public abstract void importFile(String fileName);

	public abstract double getRowPrimal(int rowNumber);
}
