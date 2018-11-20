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

* A screen manager, for running locally
    * [tmux](https://github.com/tmux/tmux)
    * [screen](https://en.wikipedia.org/wiki/GNU_Screen)
* A package manager
    * For [pip3](https://pip.pypa.io/en/stable/): `sudo apt install python3-pip`
        * `pip3 -h` # to check install 
    * For [Homebrew](https://brew.sh/) on Mac: https://docs.brew.sh/Installation
        * `brew -h` # to check install 
* [Python3](https://www.python.org/download/releases/3.0/), tested version 3.5.2
    * `python3 -V` # to check version
* [Matplotlib](https://matplotlib.org/), tested version 2.2.2
    * `pip3 install matplotlib` # to install 
    * `python3; import matplotlib` # to check installation
* [TensorFlow](https://www.tensorflow.org/), tested versions 1.4.1, 1.5.0, 1.7.0
    * `python3 -c 'import tensorflow as tf; print(tf.__version__)'` # to check version
    * `pip3 install tensorflow==1.5` # to install
* [Java 8](https://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html), tested version 1.8.0_162
    * `java -version` # to check version
    * `sudo apt-get install default-jdk` # to install Java on Unix
* [cloudpickle](https://github.com/cloudpipe/cloudpickle)
    * `pip3 install cloudpickle` # to install
* [OpenAI Baselines](https://github.com/masonwright14/baselines), downloaded from my own fork on GitHub and built from source
* [OpenAI Gym](https://github.com/masonwright14/gym), downloaded from my own fork on GitHub and built from source
* [Py4J](https://www.py4j.org/)
    * `pip3 install py4j` # to install
* [Gambit](http://www.gambit-project.org/), tested version 15.1.1; built from source
* [GameAnalysis](https://github.com/egtaonline/gameanalysis)
    * `pip3 install gameanalysis --user` # to install
        * must be installed under Python3, not Python2
        * may need to add to `PATH` in `.bash_profile`: `export PATH="/home/masondw/.local/bin:$PATH"`
    * `ga -h` # to check installation 

## Installing Mason's fork of OpenAI Baselines

```
cd ~/
mkdir baselines
cd baselines
git clone https://github.com/masonwright14/baselines.git
cd baselines
pip3 install -e .
```

* this will fail if [mpi](https://www.mpich.org/) is not installed. in that case, do:
    * `brew install mpich` # to install if on Mac with [Homebrew](https://brew.sh/)
    * `pip3 install mpi4py` # to install if don't have Homebrew

To check installation of my fork of OpenAI Baselines:

```
cd ~/baselines/baselines/baselines/deepq/experiments
python3 train_cartpole.py
python3 enjoy_cartpole.py # won't work if don't have a display on this server
```

## Installing Mason's fork of OpenAI Gym

```
cd ~/
mkdir gym
cd gym
git clone https://github.com/masonwright14/gym.git
cd gym
pip3 install -e .[all]
```

* this will fail if [SWIG](http://www.swig.org/) is not installed. in that case, do:
    * `sudo apt-get install swig` # to install
* this will fail if [CMake](https://cmake.org/) is not installed. in that case, do:
    * `brew install cmake` # to install if on Mac, with Homebrew
    * `sudo apt-get install cmake` # to install on Unix
* this may fail if [zlib](https://zlib.net/) is not installed. in that case, do:
    * `sudo apt-get install zlib1g-dev`
* this may fail of [OpenMPI](https://www.open-mpi.org/) is not installed. in that case, do:
    * `sudo apt install libopenmpi-dev` 

To check installation of my fork of OpenAI Gym:

```
python3
import gym
env = gym.make('Hex9x9-v0')
env.reset()
env.render()
```

* check may fail of OpenAI Baselines is not installed yet.
* check may fail if there is a duplicate copy of OpenAI Gym in `~/.local/lib/python3.5/site-packages/`
    * In case of duplicate copy in `~/.local`: `cd ~/.local/lib/python3.5/site-packages/`, then `mv gym gymtemp`

## Installing Gambit 15.1.1

Download: https://sourceforge.net/projects/gambit/files/gambit15/15.1.1

Unzip the package as `~/gambit-15.1.1`

```
cd ~/gambit-15.1.1
./configure
make
sudo make install
```

* Compilation may fail if gcc is version 7.
   * `gcc -v` # should return version 5 or 6
   * `g++ -v` # should return version 5 or 6

To test Gambit install:

```
gambit-lcp -h
```

## Example run of DO-EGTA experiment

```
cd ~/depgraph/depgraph/att_graph_runner
tmux new -s mySession1
stdbuf -i0 -o0 -e0 python3 -u master_dq_runner.py 3014 0 d30d1_randNoAndB \
    d30d1 DepgraphJava-v0 DepgraphJavaAtt-v0 DepgraphJavaEnvBoth-v0 \
    RandomGraph30N100E6T1_B.json DepgraphJavaEnvVsMixedDef-v0 \
    DepgraphJavaEnvVsMixedAtt-v0 400 dg_d30d1_dq_mlp_rand_epoch \
    dg_d30d1_dq_mlp_rand_epoch d30 1000000 700000 None 500 True True > \
    master_d30d1_agr_out1.txt
Ctl-b d
```

### Notes
* `stdbuf -i0 o0 e0` turns off output string buffering
* `3014` is the game number (taken from EGTA-Online output)
* `0` is the initial round number (always starts at 0)
* `d30d1_randNoAndB` is an arbitrary name to give to this run
* `d30d1` is a shorter, arbitrary version of this run's name
* `DepgraphJava-v0` is the OpenAI Gym environment name when defender network plays vs. pure strategy
* `DepgraphJavaAtt-v0` is the OpenAI Gym environment name when attacker network plays vs. pure strategy
* `DepgraphJavaEnvBoth-v0` is the OpenAI Gym environment name when both players are networks
* `RandomGraph30N100E6T1_B.json` is the attack-graph file name
* `DepgraphJavaEnvVsMixedDef-v0` is the OpenAI Gym environment name when attacker network plays vs. mixed strategy
* `DepgraphJavaEnvVsMixedAtt-v0` is the OpenAI Gym environment name when defender network plays vs. mixed strategy
* `400` is number of payoff samples to use to compute expected payoff, when checking if new network is a beneficial deviation
* `dg_d30d1_dq_mlp_rand_epoch` is base name to use for saving network strategies for one agent
* `dg_d30d1_dq_mlp_rand_epoch` is base name to use for saving network strategies for other agent
* `d30` is a short name to use corresponding to strategy file names to record, like `defStratStrings_d30.txt`
* `1000000` is how many steps to train for defender
* `700000` is how many steps to train for attacker
* `None` is the maximum number of rounds to train for before pausing, or `None`
* `500` is number of payoff samples to use to compute expected payoff, to extend payoff table of empirical game with new strategy
* `True` is whether to continue after the current round's training is complete
* `True` is whether to continue after the current round's payoff table extension is complete
