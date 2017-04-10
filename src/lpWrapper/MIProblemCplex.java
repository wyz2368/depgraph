package lpWrapper;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.CpxException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class MIProblemCplex extends AMIProblem {
	protected IloCplex cplex;
	// IloObjective objectiveFunction;

	protected List<IloNumVar> columns;
	protected Map<Integer, IloRange> rows;

	protected Map<Integer, ROW_STATUS> rowsInModel;
	protected List<Double> objectiveCoeff;
	protected List<IloNumVarType> columnType;

	protected IloCplex.BasisStatus[] colStatuses;
	protected IloCplex.BasisStatus[] rowStatuses;

	private boolean objectiveModified;
	
	protected void initialize() {
		try {
			if (cplex != null) {
				cplex.end();
			}
			objectiveModified = false;
			cplex = new IloCplex();
			cplex.setName("MIProblem");
			// objectiveFunction = cplex.getObjective();
			// cplex.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Dual);
//			cplex.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Primal);
			// cplex.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Barrier);
			// cplex.setParam(IloCplex.DoubleParam.EpMrk, 0.999);

			this.redirectOutput(null);
		} catch (IloException e) {
			e.printStackTrace();

			throw new RuntimeException(e.getMessage());
		}
		columns = new ArrayList<IloNumVar>(this.numCols);
		rows = new HashMap<Integer, IloRange>(this.numRows);
		rowsInModel = new HashMap<Integer, MIProblemCplex.ROW_STATUS>(
				this.numRows);
		objectiveCoeff = new ArrayList<Double>(this.numCols);
		columnType = new ArrayList<IloNumVarType>(this.numCols);
		colStatuses = null;
		rowStatuses = null;
	}

	public STATUS_TYPE getSolveStatus() {
		CplexStatus cplexStat;
		try {
			cplexStat = cplex.getCplexStatus();
			if (cplexStat == CplexStatus.Optimal || cplexStat == CplexStatus.OptimalTol) {
				return STATUS_TYPE.OPTIMAL;
			} else if (cplexStat == CplexStatus.Infeasible) {
				return STATUS_TYPE.INFEASIBLE;
			} else if (cplexStat == CplexStatus.Unbounded) {
				return STATUS_TYPE.UNBOUNDED;
			} else {
				// System.out.println("Cplex Status: " + cplex.getCplexStatus());
				return STATUS_TYPE.UNKNOWN;
			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Unable to get cplex status.");
		}
	}

	public MIProblemCplex() {
		super();
		this.cplex = null;
		this.initialize();
	}

	protected void setProblemName(String name) {
		cplex.setName(name);
	}
//	public void writeProb(String fileName) {
//        try {
//            cplex.exportModel(fileName + ".lp");
//        } catch (IloException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e.getMessage());
//        }
//    }

	protected void setObjectiveCoef(int index, double value) {
		this.objectiveModified = true;
		this.objectiveCoeff.set(index - 1, value);
	}

	protected double getObjectiveCoef(int index) {
		return this.objectiveCoeff.get(index - 1);
	}

	/**
	 * Returns the number of iterations in the last solve.
	 * 
	 * @return
	 */
	public int getNumberIterations() {
		return this.cplex.getNiterations();
	}

	/**
	 * Returns the number of MIP starts associated with the current problem.
	 * 
	 * @return
	 */
	public int getMIPStarts() {
		try {
			return this.cplex.getNMIPStarts();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("MIP starts could not be obtained.");
		}
	}

	/**
	 * Returns the no. of branch-and-cut nodes explored in solving the active
	 * model.
	 * 
	 * @return
	 */
	public int getNodesExplored() {
		return this.cplex.getNnodes();
	}

	/**
	 * Returns the number of unexplored nodes in the branch-and-cut tree.
	 * 
	 * @return
	 */
	public int getNodesLeft() {
		return this.cplex.getNnodesLeft();
	}

	/**
	 * Returns the number of phase I simplex iterations from the last solve.
	 * 
	 * @return
	 */
	public int getSimplexIterations() {
		return this.cplex.getNphaseOneIterations();
	}

	public void resetColumnBound(int columnNumber, BOUNDS_TYPE boundType,
			double lowerBound, double upperBound) {
		try {
			switch (boundType) {
			case FREE:
				this.columns.get(columnNumber - 1).setLB(-Configuration.MM);
				this.columns.get(columnNumber - 1).setUB(Configuration.MM);
				break;
			case LOWER:
				this.columns.get(columnNumber - 1).setLB(-Configuration.MM);
				this.columns.get(columnNumber - 1).setUB(Configuration.MM);
				this.columns.get(columnNumber - 1).setLB(lowerBound);
				break;
			case UPPER:
				this.columns.get(columnNumber - 1).setLB(-Configuration.MM);
				this.columns.get(columnNumber - 1).setUB(Configuration.MM);
				this.columns.get(columnNumber - 1).setUB(upperBound);
				break;
			case DOUBLE:
				this.columns.get(columnNumber - 1).setLB(-Configuration.MM);
				this.columns.get(columnNumber - 1).setUB(Configuration.MM);
				this.columns.get(columnNumber - 1).setLB(lowerBound);
				this.columns.get(columnNumber - 1).setUB(upperBound);
				break;
			case FIXED:
				this.columns.get(columnNumber - 1).setLB(-Configuration.MM);
				this.columns.get(columnNumber - 1).setUB(Configuration.MM);
				this.columns.get(columnNumber - 1).setLB(lowerBound);
				this.columns.get(columnNumber - 1).setUB(lowerBound);
				break;
			default:
				throw new RuntimeException("No such bound type.");
			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Column Number is wrong.");
		}
	}

	protected void setProblemType(PROBLEM_TYPE problemType,
			OBJECTIVE_TYPE objectiveType) {
		this.probType = problemType;
		this.objectiveType = objectiveType;
		try {
			this.updateObjective();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Update Objective Failed");
		}
	}

	public boolean disableRow(int rowNumber) throws RuntimeException {
		try {
			if (this.rowsInModel.get(rowNumber - 1) == ROW_STATUS.ENABLED) {
				this.cplex.remove(this.rows.get(rowNumber - 1));
				this.rowsInModel.put(rowNumber - 1, ROW_STATUS.DISABLED);
				return true;
			}
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return false;
	}

	public boolean deleteRow(int rowNumber) throws RuntimeException {
		try {
			if (this.rowsInModel.get(rowNumber - 1) == ROW_STATUS.ENABLED) {
				this.cplex.remove(this.rows.get(rowNumber - 1));
				this.rows.remove(rowNumber - 1);

				return true;
			}
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return false;
	}

	public void importFile(String fileName) {
		try {
			this.cplex = new IloCplex();
			cplex.importModel(fileName);
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Error reading file");
		}
	}

	public boolean enableRow(int rowNumber) throws RuntimeException {
		try {
			if (this.rowsInModel.get(rowNumber - 1) == ROW_STATUS.DISABLED) {
				this.cplex.add(this.rows.get(rowNumber - 1));
				this.rowsInModel.put(rowNumber - 1, ROW_STATUS.ENABLED);
				return true;
			}
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return false;
	}

	private IloNumExpr getObjectiveFunction() throws IloException {
		Double[] objCoefDblObj = this.objectiveCoeff.toArray(new Double[] {});
		double[] objCoeffDblBasicType = new double[objCoefDblObj.length];
		for (int i = 0; i < objCoefDblObj.length; i++) {
			double val = objCoefDblObj[i].doubleValue();
			if (Math.abs(val) < Configuration.EPSILON) {
				val = 0.0;
			}
			objCoeffDblBasicType[i] = val;
		}
		return (cplex.scalProd(columns.toArray(new IloNumVar[] {}),
				objCoeffDblBasicType));
	}

	public void removeObjective() {
		try {
			cplex.delete(cplex.getObjective());
			for ( int i = 0; i < this.objectiveCoeff.size(); i++) {
				this.objectiveCoeff.set(i, 0.0);
			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Couldn't delete objective.");
		}
	}
	
	public void updateObjective() throws Exception {
		IloNumExpr objectiveFunctionExpr = this.getObjectiveFunction();

		cplex.delete(cplex.getObjective());
		switch (this.objectiveType) {
		case MAX:
			cplex.addMaximize(objectiveFunctionExpr);
			break;
		case MIN:
			cplex.addMinimize(objectiveFunctionExpr);
			break;
		default:
			throw new IllegalArgumentException("I don't know this type, kid!");
		}
	}

	public void addAndSetColumn(String name, BOUNDS_TYPE boundType,
			double lowerBound, double upperBound, VARIABLE_TYPE varType,
			double objCoeff) {
		try {
			// IloColumn newCol = cplex.column(this.objectiveFunction,
			// objCoeff);
			IloNumVar col;
			switch (varType) {
			case CONTINUOUS:
				col = cplex.numVar(lowerBound, upperBound, IloNumVarType.Float,
						name);
				columnType.add(IloNumVarType.Float);
				break;
			case INTEGER:
				col = cplex.numVar(lowerBound, upperBound, IloNumVarType.Int,
						name);
				columnType.add(IloNumVarType.Int);
				break;
			default:
				throw new IllegalArgumentException("Unknown Variable Type");
			}
			this.columns.add(col);
			objectiveCoeff.add(objCoeff);
			this.numCols++;

			this.updateObjective();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public void resetRowBound(int rowNumber, BOUNDS_TYPE boundType,
			double lowerBound, double upperBound) {
		try {

			/*
			 * this.rows.get(rowNumber - 1).setLB(-Configuration.MM);
			 * this.rows.get(rowNumber - 1).setUB(Configuration.MM);
			 */
			switch (boundType) {
			case LOWER:
				this.rows.get(rowNumber - 1).setLB(-Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setUB(Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setLB(lowerBound);
				break;
			case UPPER:
				this.rows.get(rowNumber - 1).setLB(-Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setUB(Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setUB(upperBound);
				break;
			case DOUBLE:
				this.rows.get(rowNumber - 1).setLB(-Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setUB(Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setLB(lowerBound);
				this.rows.get(rowNumber - 1).setUB(upperBound);
				break;
			case FIXED:
				this.rows.get(rowNumber - 1).setLB(-Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setUB(Double.MAX_VALUE);
				this.rows.get(rowNumber - 1).setLB(lowerBound);
				this.rows.get(rowNumber - 1).setUB(lowerBound);
				break;
			default:
				throw new RuntimeException("No such bound type.");
			}
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Row Number is wrong. (row number = "
					+ rowNumber + ")");
		}

	}

	public int addAndSetRow(String name, BOUNDS_TYPE boundType,
			double lowerBound, double upperBound) {
		try {
			IloRange newRow;
			IloNumExpr rowExpr = cplex.constant(0.0);
			switch (boundType) {
			case UPPER:
				newRow = cplex.addGe(upperBound, rowExpr, name);
				break;
			case LOWER:
				newRow = cplex.addLe(lowerBound, rowExpr, name);
				break;
			case FIXED:
				newRow = cplex.addEq(lowerBound, rowExpr, name);
				break;
			default:
				throw new IllegalArgumentException("Unknown Type");
			}
			rows.put(this.numRows, newRow);
			rowsInModel.put(this.numRows, ROW_STATUS.ENABLED);
			this.numRows++;
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException("Error in Set Row: "
					+ (this.numRows + 1));
		}
		return rows.size();
	}

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
	public void setMatCol(String name, VARIABLE_TYPE varType, double objCoeff,
			double lowerBound, double upperBound, List<Integer> indices,
			List<Double> values) throws RuntimeException {
		// do addColumn
		// setColumn
		// setMatCol
		IloColumn newColumn;
		try {
			newColumn = cplex.column(cplex.getObjective(), objCoeff);
			for (int i = 0; i < indices.size(); i++) {
				newColumn = newColumn.and(cplex.column(
						this.rows.get(indices.get(i) - 1), values.get(i)));
			}
			IloNumVar col;
			switch (varType) {
			case CONTINUOUS:
				col = cplex.numVar(newColumn, lowerBound, upperBound,
						IloNumVarType.Float, name);
				columnType.add(IloNumVarType.Float);
				break;
			case INTEGER:
				col = cplex.numVar(newColumn, lowerBound, upperBound,
						IloNumVarType.Int, name);
				columnType.add(IloNumVarType.Int);
				break;
			default:
				throw new IllegalArgumentException("Unknown Variable Type");
			}
			objectiveCoeff.add(objCoeff);
			columns.add(col);
			this.numCols++;
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Doesn't add a new row
	 */
	public void setMatRow(int rowNo, List<Integer> indices, List<Double> values)
			throws RuntimeException {
		if (indices.size() != values.size()) {
			throw new RuntimeException();
		}
		IloNumExpr constraintExpr;
		try {
			constraintExpr = cplex.constant(0.0);
			for (int i = 0; i < indices.size(); i++) {
				constraintExpr = cplex.sum(
						constraintExpr,
						cplex.prod(values.get(i).doubleValue(),
								columns.get(indices.get(i) - 1)));
			}
			rows.get(rowNo - 1).setExpr(constraintExpr);
		} catch (Exception e) {	
//			System.out.println("rowNo: " + rowNo);
//			System.out.println(rows.size());
//			System.out.println(rows.get(rowNo - 1));
			
			e.printStackTrace();
			
//			System.exit(1); // Manish
			
			throw new RuntimeException(e.getMessage());						
		}
	}

	@Override
	protected void saveBasisStatus() {
		if (!Configuration.WARMSTARTLPS || this.columns.size() == 0
				|| this.rows.size() == 0 || this.probType == PROBLEM_TYPE.MIP)
			return;
		try {
			// IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
			// IloNumVar[] vars = lp.getNumVars();
			// colStatuses = cplex.getBasisStatuses(vars);
			colStatuses = cplex.getBasisStatuses(this.columns
					.toArray(new IloNumVar[] {}));
			rowStatuses = cplex.getBasisStatuses(this.rows.values().toArray(
					new IloRange[] {}));
		} catch (IloException e) {
			e.printStackTrace();
			System.err.println("Save Basis Failed: " + e.getMessage());
			colStatuses = null;
			rowStatuses = null;
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	protected void loadBasisStatus() {
		if (!Configuration.WARMSTARTLPS || colStatuses == null
				|| rowStatuses == null || this.probType == PROBLEM_TYPE.MIP)
			return;
		try {
			cplex.setBasisStatuses(this.columns.toArray(new IloNumVar[] {}),
					colStatuses, 0, colStatuses.length, this.rows.values()
							.toArray(new IloRange[] {}), rowStatuses, 0,
					rowStatuses.length);
		} catch (IloException e) {
			e.printStackTrace();
			System.err.println("Load Basis Failed: " + e.getMessage());
			colStatuses = null;
			rowStatuses = null;
			throw new RuntimeException(e.getMessage());
		}
	}

	public void solve() throws LPSolverException {	
		if (this.isLoaded == false)
			this.loadProblem();
		if ( this.objectiveModified == true ) {
			try {
				this.updateObjective();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new LPSolverException();				
			}
			this.objectiveModified = false;
		}
		long start = System.currentTimeMillis();
		try {
			this.updateObjective();
			this.loadBasisStatus();
			boolean retval = cplex.solve();
			// MANISH DEBUG PRINT
			// cplex.getCplexStatus().equals(CplexStatus.Optimal)
			if (retval == false /*
								 * ||
								 * !cplex.getCplexStatus().equals(CplexStatus.
								 * Optimal)
								 */) {
				// writeProb("CPLEXfail");
				if (Configuration.PRINT_ERROR) {
					System.err.println("CPLEX error: " + cplex.getObjValue()
							+ ";" + cplex.getCplexStatus());
				}
				throw new LPSolverException();
			}
			this.saveBasisStatus();
		} catch (Exception e) {
			this.runTime = System.currentTimeMillis() - start;
			if (Configuration.PRINT_ERROR) {
				e.printStackTrace();
			}
			throw new LPSolverException();
		}
		this.runTime = System.currentTimeMillis() - start;
	}

	public double getRowDual(int rowNumber) {
		try {
			double dual = cplex.getDual(rows.get(rowNumber - 1));
			return dual;
		} catch (UnknownObjectException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public double getRowSlack(int rowNumber) {
		try {
			return cplex.getSlack(rows.get(rowNumber - 1));
		} catch (UnknownObjectException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public double getColumnPrimal(int columnNumber) {
		double xVal;
		try {
			xVal = cplex.getValue(columns.get(columnNumber - 1));
			return xVal;
		} catch (UnknownObjectException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (CpxException e) {
			e.printStackTrace();
			return 0.0;
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}

	}

	public List<Double> getRowDualVector() {
		double[] duals;
		try {
			duals = cplex.getDuals(rows.values().toArray(new IloRange[] {}));
			List<Double> dualVect = new ArrayList<Double>(duals.length);
			for (int i = 0; i < duals.length; i++) {
				dualVect.add(duals[i]);
			}
			return dualVect;
		} catch (UnknownObjectException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public List<Double> getColumnPrimalVector() {
		try {
			double[] xVals = cplex.getValues(columns
					.toArray(new IloNumVar[] {}));
			List<Double> primalVect = new ArrayList<Double>(xVals.length);
			for (int i = 0; i < xVals.length; i++) {
				primalVect.add(xVals[i]);
			}
			return primalVect;
		} catch (UnknownObjectException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public double getLPObjective() {
		try {
			return cplex.getObjValue();
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public void writeProb(String fileName) {
		try {
			cplex.exportModel(fileName + ".lp");
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public void writeSol(String fileName) {
		try {
			cplex.writeSolution(fileName);
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * clean the data structures.
	 */
	public void end() {
		cplex.end();
		columns.clear();
		rows.clear();
		objectiveCoeff.clear();
		columnType.clear();
		colStatuses = null;
		rowStatuses = null;
	}

	/**
	 * 
	 * @param stream
	 *            can be <code>null</code> if no output should be provided.
	 */
	public void redirectOutput(OutputStream stream) {
		cplex.setOut(stream);
		cplex.setWarning(stream);
	}

	@Override
	public double getRowPrimal(int rowNumber) {
		try {
			return this.cplex.getValue(this.rows.get(rowNumber - 1).getExpr());
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
}
