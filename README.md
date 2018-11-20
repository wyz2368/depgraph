# depgraph, a dependency graph library

This is a library for running simulations of dependency graphs, which are similar to attack graphs.
They represent an attacker and defender seeking control over a computer network.

The code was written by Thanh Nguyen with help from Mason Wright.

## Checking out from GitLab

1. git clone git@strategicreasoning.eecs.umich.edu:masondw/depgraph.git
    * Clone the repository, editing above ssh link as needed
    * Can also use HTML link, like:
        * git clone https://strategicreasoning.eecs.umich.edu/masondw/depgraph.git
2. git checkout my_awesome_branch
    * Switch to your personal branch, assuming it exists
3. git branch
    * Verify you are using your own branch

## Eclipse build

1. File -> Import -> Git -> Projects form Git
    * 'Projects from Git' -> Next
2. Existing local repository -> Next
3. Add . . . -> Browse . . . -> browse for the depgraph repo, select it -> Finish
4. Select the repo you just chose -> Next
5. Import using the New Project Wizard -> Finish
    * You may have to select Wizards -> Java -> Java Wizard
6. Java Project -> Next
    * Java SE 8 [1.8.0_121] -> depgraph (title of your choice) -> Finish
    * If this fails, try again but uncheck "use default location", instead using the location of the git repo.
7. Configure the build path:
    * link source: depgraph/src
    * link source: depgraph/test
    * add extrernal jars: (available in Google Drive folder depgraphJars)
        * commons-math3-3.6.1.jar
        * cplex.jar
        * gson-2.2.4.jar
        * jgrapht-core-1.0.1.jar
        * jgrapht-ext-1.1.0.jar
        * jgraphx.jar
        * json-simple-3.0.2.jar
        * py4j0.10.6.jar
    * add external libraries:
        * JUnit 4
8. File -> Import -> General -> File System -> depgraph/graphs
    * Into folder: depgraph/graphs -> Finish
9. File -> Import -> General -> File System -> depgraph/simspecs
    * Into folder: depgraph/simspecs -> Finish
10. File -> Import -> General -> File System -> depgraph -> defaults.json (select 1 file only)
    * Into folder: depgraph -> Finish
11. File -> Import -> General -> File System -> depgraph/testDirs
    * Into folder: depgraph/testDirs -> Finish
12. Optional: Get helpful add-ons for Eclipse
    * Find Bugs: http://findbugs.sourceforge.net/
    * Checkstyle: http://checkstyle.sourceforge.net/

## Jar Dependencies
* Apache Commons Math 3.6.1: http://commons.apache.org/proper/commons-math/download_math.cgi
* CPLEX Optimizer: https://www.ibm.com/analytics/cplex-optimizer
* Google Gson 2.2.4: https://mvnrepository.com/artifact/com.google.code.gson/gson/2.2.4
* JGraphT 1.0.1: https://jgrapht.org/
* JGraphX: https://github.com/jgraph/jgraphx
* Json-Simple 3.0.2: https://cliftonlabs.github.io/json-simple/
* Py4J 0.10.6: https://www.py4j.org/install.html

# Deep RL code

## Dependencies for deep RL
* TensorFlow version 1.4.1, 1.5.0, or 1.7.0; others might work too
    * pip3 install tensorflow==1.5 # to install
    * python3 -c 'import tensorflow as tf; print(tf.__version__)' # to check version
* Python3 version 3.5.2; others might work too
    * python3 -V # to check version
* Matplotlib 2.2.2; others might work too
    * pip3 install matplotlib # to install