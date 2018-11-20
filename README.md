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
    * `python3 -c 'import tensorflow as tf; print(tf.__version__)'` # to check version
    * `pip3 install tensorflow==1.5` # to install
* Python3, tested version 3.5.2
    * `python3 -V` # to check version
* [Matplotlib](https://matplotlib.org/), tested version 2.2.2
    * `pip3 install matplotlib` # to install 
    * `python3; import matplotlb` # to check installation
* Java 8, tested version 1.8.0_162
    * `java -version` # to check version
* [cloudpickle](https://github.com/cloudpipe/cloudpickle)
    * `pip3 install cloudpickle` # to install
* [OpenAI Gym](https://github.com/masonwright14/gym), downloaded from my own fork on GitHub and built from source
* [OpenAI Baselines](https://github.com/masonwright14/baselines), downloaded from my own fork on GitHub and built from source
* [Py4J](https://www.py4j.org/)
    * `pip3 install py4j` # to install
* [Gambit](http://www.gambit-project.org/), tested version 15.1.1; built from source
* [GameAnalysis](https://github.com/egtaonline/gameanalysis)
    * `pip3 install gameanalysis --user` # to install
        * must be installed under Python3, not Python2
        * may need to add to `PATH` in `.bash_profile`: `export PATH="/home/masondw/.local/bin:$PATH"`
    * `ga -h` # to check installation 

## Installing Mason's fork of OpenAI Gym

```
mkdir gym
cd gym
git clone https://github.com/masonwright14/gym.git
cd gym
pip3 install -e .[all]
```

* this will fail if Swig is not installed. in that case, do:
    * `sudo apt-get install swig` # to install
* this will fail if cmake is not installed. in that case, do:
    * `brew install cmake` # to install if on Mac, with Homebrew

To check installation of my fork of OpenAI Gym:

```
python3
import gym
env = gym.make('Hex9x9-v0')
env.reset()
env.render()
```

## Installing Mason's fork of OpenAI Baselines

```
mkdir baselines
cd baselines
git clone https://github.com/masonwright14/baselines.git
cd baselines
pip3 install -e .
```

* this will fail if mpi is not installed. in that case, do:
    * `brew install mpich` # to install if on Mac
    * `pip3 install mpi4py` # to install if don't have Homebrew

To check installation of my fork of OpenAI Baselines:

```
cd ~/baselines/baselines/baselines/deepq/experiments
python3 train_cartpole.py # won't work if don't have a display
```

## Installing Gambit 15.1.1

Download: https://sourceforge.net/projects/gambit/files/gambit15/15.1.1

Unzip the package as gambit-15.1.1

```
cd gambit-15.1.1
./configure
make
sudo make install
```

* Compilation may fail with gcc-7 version.
   * gcc -v # should return version 5 or 6
   * g++ -v # should return version 5 or 6

To test Gambit install:

```
gambit-lcp -h
```
