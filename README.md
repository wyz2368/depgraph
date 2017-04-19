# depgraph, a dependency graph library

This is a library for running simulations of dependency graphs, which are similar to attack graphs.
They represent an attacker and defender seeking control over a computer network.

The code was written by Thanh Nguyen with help from Mason Wright.

## Eclipse build

1. File -> Import -> Git -> Projects form Git
    * 'Projects from Git' -> Next
2. Existing local repository -> Next
3. Add . . . -> Browse . . . -> browse for the depgraph repo, select it -> Finish
4. Select the repo you just chose -> Next
5. Import using the New Project Wizard -> Finish
6. Java Project -> Next
    * Java SE 8 [1.8.0_121] -> depgraph (title of your choice) -> Finish
7. Configure the build path:
    * link source: depgraph/src
    * link source: depgraph/test
    * add extrernal jars:
        * commons-math3-3.6.1.jar
        * cplex.jar
        * gson-2.2.4.jar
        * java-json.jar
        * jgrapht-core-1.0.1.jar

