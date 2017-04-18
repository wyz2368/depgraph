package lpwrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

// -Djava.library.path=/path/of/cplex/installation

public final class Configuration {
	public static final int MIP_PRESOLVE = 1;
	public static final int MIP_TIMELIMIT = -1;
	public static final double MIP_TOLERANCE = 0.001;

	public static final int MM = 100000; // Integer.MAX_VALUE;

	public static final boolean FAILURE = false;
	public static final boolean SUCCESS = true;
	public static final double EPSILON = 1e-4;

	public static final long SEED = System.currentTimeMillis();
	public static final Random RAND = new Random(SEED);

	public static final boolean PRINT_ERROR = false;

	public static final boolean WARMSTARTLPS = true;
	public static final boolean TRUNCATELPS = true;

	private static boolean loadedGlpk = false;
	private static boolean loadedCplex = false;
	
	private Configuration() {
		// private constructor
	}

	public static void loadLibrariesGLPK(final String configFileName) throws IOException {
		if (loadedGlpk) {
			return;
		}
		if (configFileName == null) {
			throw new IllegalArgumentException();
		}
		FileReader fstream = new FileReader(configFileName);
		@SuppressWarnings("resource")
		BufferedReader in = new BufferedReader(fstream);

		String gLPKFileName = null;
		String gLPKFileJavaName = null;

		String line = in.readLine();
		while (line != null) {
			line = line.trim();		
			if (line.length() > 0 && !line.startsWith("#")) {
				// not a comment
				String[] list = line.split("=");
				if (list.length != 2) {
					throw new RuntimeException(
							"Unrecognized format for the config file.\n");
				}
				if (list[0].equals("GLPKLIB_FILE")) {
					gLPKFileName = list[1];
				} else if (list[0].equals("GLPKJAVABINDING_FILE")) {
					gLPKFileJavaName = list[1];
				} else {
					System.err
							.println("Unrecognized statement in Config File: "
									+ line);
				}
				line = in.readLine();
			}

		}

		// Finally, load the libs.
		File gLPKFile = new File(gLPKFileName);
		File gLPKFileJava = new File(gLPKFileJavaName);
		System.load(gLPKFile.getAbsolutePath());
		System.load(gLPKFileJava.getAbsolutePath());
		loadedGlpk = true;
	}

	public static void loadLibrariesCplex() throws IOException {
		Configuration.loadLibrariesCplex(
			"/Users/thanhnguyen/Documents/WORKS/ATTACK_GRAPH/CODES/CPLEX/CplexConfig");
		// Configuration.loadLibrariesCplex("/home/thanhhng/CPLEX/CplexConfig");
	}
	
	public static void loadLibrariesCplex(final String configFileName) throws IOException {
		if (loadedCplex) {
			return;
		}
		if (configFileName == null) {
			throw new IllegalArgumentException();
		}
		FileReader fstream = new FileReader(configFileName);
		@SuppressWarnings("resource")
		BufferedReader in = new BufferedReader(fstream);
		
		String cPlexFileString = null;

		String line = in.readLine();
		while (line != null) {
			line = line.trim();
			if (line.length() > 0 && !line.startsWith("#")) {
				// not a comment
				String[] list = line.split("=");
				if (list.length != 2) {
					throw new RuntimeException(
							"Unrecognized format for the config file.\n");
				}
				String osType = System.getProperty("os.arch");
				if (list[0].equals("LIB_FILE")) {
					cPlexFileString = list[1];
				} else if (osType.contains("32") && list[0].equals("LIB_FILE_32")) {
					cPlexFileString = list[1];
				} else if (osType.contains("64") && list[0].equals("LIB_FILE_64")) {
					cPlexFileString = list[1];
					// } 
					// else if (list[0].equals("LICENSE_FILE")) {
						// CplexLicenseString = list[1];
				} else {
					System.err
						.println("Unrecognized statement in Config File: "
							+ line);
				}				
			}
			line = in.readLine();

		}

		// Finally, load the libs.
		File cPlexFile = new File(cPlexFileString);
		
		System.load(cPlexFile.getAbsolutePath());
		// File CplexLicenseFile = new File(CplexLicenseString);
		// try {
			// IloCplex.putenv("ILOG_LICENSE_FILE=" + CplexLicenseFile.getAbsolutePath());
		// } catch (IloException e) {
			// System.err.println("Couldn't load Cplex license from file: " + CplexLicenseFile.getAbsolutePath());
			// e.printStackTrace();
		// }
		loadedCplex = true;
	}
}
