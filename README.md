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
8. File -> Import -> General -> File System -> depgraph/graphs
    * Into folder: depgraph/graphs -> Finish
9. File -> Import -> General -> File System -> depgraph/simspecs
    * Into folder: depgraph/simspecs -> Finish
10. File -> Import -> General -> File System -> depgraph -> defaults.json (select 1 file only)
    * Into folder: depgraph -> Finish
11. File -> Import -> General -> File System -> depgraph/testDirs
    * Into folder: depgraph/testDirs -> Finish

# Deep RL code

## Dependencies for deep RL

* TensorFlow, tested versions 1.4.1, 1.5.0, 1.7.0
    * python3 -c 'import tensorflow as tf; print(tf.__version__)' # to check version
    * pip3 install tensorflow==1.5 # to install
* Python3, tested version 3.5.2
    * python3 -V # to check version
* Matplotlib 2.2.2
    * pip3 install matplotlib # to install 
* Java 8, tested Java version 1.8.0_162
    * java -version # to check version
* OpenAI Gym, downloaded from my own fork on GitHub and built from source

## Installing my fork of OpenAI Gym

```
mkdir gym
cd gym
git clone https://github.com/masonwright14/gym.git
cd gym
pip3 install -e .[all]
```

* this will fail if Swig is not installed. in that case, do:
    * sudo apt-get install swig
* this will fail if cmake is not installed. in that case, do:
    * brew install cmake # if on Mac, with Homebrew

To check installation of my fork:

```
python3
import gym
env = gym.make('Hex9x9-v0')
env.reset()
env.render()
```

