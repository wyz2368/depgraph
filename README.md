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

* Deep RL code has been tested only on Linux servers, running CentOS or Ubuntu. Analysis code has been mostly used on Mac OS, with some Ubuntu.
* A screen manager, for running locally
    * [tmux](https://github.com/tmux/tmux) or [screen](https://en.wikipedia.org/wiki/GNU_Screen)
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

The above call works for game r30, but for game s29, several coupled changes to the method call must be made, as below.

```
cd ~/depgraph/depgraph/att_graph_runner
tmux new -s mySession1b
stdbuf -i0 -o0 -e0 python3 -u master_dq_runner.py 3013 0 s29m1_randNoAndB \
    s29m1 DepgraphJava29N-v0 DepgraphJavaEnvAtt29N-v0 DepgraphJavaEnvBoth29N-v0 \
    SepLayerGraph0_noAnd_B.json DepgraphJavaEnvVsMixedDef29N-v0 \
    DepgraphJavaEnvVsMixedAtt29N-v0 400 dg_s29m1_dq_mlp_rand_epoch \
    dg_s29m1_dq_mlp_rand_epoch s29 700000 700000 None 500 True True > \
    master_s29m1_agr_out1.txt
Ctl-b d
```

The key points are that game s29 uses a different graph file and different
OpenAI Gym environments.

## Example run of HADO-EGTA experiment

```
cd ~/depgraph/depgraph/att_graph_runner
tmux new -s mySession2
stdbuf -i0 -o0 -e0 python3 -u master_dq_runner_curve.py 3014 \
    d30cm1_randNoAndB d30cm1 DepgraphJava-v0 DepgraphJavaAtt-v0 \
    DepgraphJavaEnvBoth-v0 RandomGraph30N100E6T1_B.json \
    DepgraphJavaEnvVsMixedDef-v0 DepgraphJavaEnvVsMixedAtt-v0 400 0.7 4 d30 \
    1000000 400000 700000 400000 0 None 500 > master_d30cm1_agr_out1.txt
Ctl-b d
```

### Notes
* `0.7` is the discount factor for old opponent strategies
* `4` is the number of interim strategies to record during fine-tuning and pre-training combined
* `1000000` is the number of pre-training steps for defender
* `400000` is the number of fine-tuning steps for defender
* `700000` is the number of pre-training steps for attacker
* `400000` is the number of fine-tuning steps for attacker

## Example run on [Flux](https://arc-ts.umich.edu/flux-user-guide/)

```
ssh myUniqName@flux-login.arc-ts.umich.edu
```

You should have on Flux: `~/att_graph_runner/` from the `depgraph` repo, 
and `~/pbs` from the `depgraph` repo, plus all dependencies installed, 
including `~/gambit-15.1.1`, `~/baselines`, and `~/gym`.

```
cd pbs
module load python-dev/3.5.2
ga -h
qsub agr_test_d30f1_cur.pbs
qstat | grep myUniqName
```

### Notes
* `module load python-dev/3.5.2` loads a full version of Python3 into your environment, with extra packages like [NumPy](http://www.numpy.org/)
* `ga -h` loads GameAnalysis into your environment
* `qsub agr_test_d30f1_cur.pbs` submits the PBS script
* `qstat | grep myUniqName` checks on your jobs
* You can use `qdel 12345` to kill a job you submitted by number

## Things to watch out for

Sometimes a run of `gambit-lcp` takes many hours to terminate, because it is 
trying to search exhaustively for Nash equilibria even though it has already found some.
You can use `top` to watch out for this happening.
If it does, you should use `kill -9 12345` to kill the `gambit-lcp` process by its `pid` number.

Sometimes a server crashes due to a power outage, being physically moved, or other reasons.
In these cases, any partially generated training results for the current round must be deleted
before the run is restrated, including all `.pkl`, `attVMixed*.txt`, and `defVMixed*.txt` files from this round.
Then the run can be restarted, with the current round number as the argument.

So that multiple Java attack-graph game servers can run on the same server, a primitive locking
system is used, which is mainly located in Mason's branch of OpenAI Gym, at `gym/gym/envs/board_game`.
Sometimes you may get an error message "lock is being held". To fix this, do the following,
substituting your environment name (`d30` or `s29`):
```
cd ~/att_graph_runner
python3 unlock_all.py d30
```

## How to recompile the [JAR](https://en.wikipedia.org/wiki/JAR_(file_format)) files

If you must recompile the JAR files instead of using the ones provided in `/att_graph_runner/` subfolders, here is how.

First, you must build the Eclipse project, as described above.

To recompile `dg4jattcli.jar`, first run the file `src/rldepgraph/DepgraphPy4JAttGreedyConfigCLI.java` in Eclipse.
Then right-click it this file > Export > Runnable Jar file.
Set Export destination to `dg4jattcli.jar`, and select "Copy required libraries . . ." > OK.
You can now replace the old JAR file in `att_graph_runner/dg4jattcli/` with your new one.

The process is similar for the other JAR files included.

To recompile `dg4jdefcli.jar`, follow the steps for `src/rldepgraph/DepgraphPy4JGreedyConfigCLI.java`.

To recompile `dg4jnonetcli.jar`, follow the steps for `src/rldepgraph/DepgraphPy4JGreedyConfigNeitherNetworkCLI.java`.

To recompile `depgraphpy4jdefvsnetorheuristic.jar`, follow the steps for `src/rldepgraph/DepgraphPy4JDefVsNetOrHeuristic.java`.

To recompile `depgraphpy4jattvsnetorheuristic.jar`, follow the steps for `src/rldepgraph/DepgraphPy4JAttVsNetOrHeuristic.java`.

To recompile `depgraphpy4jconfigboth.jar`, follow the steps for `src/rldepgraph/DepgraphPy4JGreedyConfigBoth.java`.

## How to visualize results

First, you should gather the output data files into the expected folders, for data visualization code in `deeprlanalyze/`.

* All `*.pkl` files should be copied to `deeprlanalyze/pkl_files/`
* All `*.tsv` files should be copied to `deeprlanalyze/eqs2/`
* All `*_vsEq.txt` and `_vsRetrain.txt` files should be copied to `deeprlanalyze/for_plot_curve/` (HADO-EGTA runs only)
* All `*.json` and `*_lcp_decode.txt` files FROM COMBINING GAME FILES ONLY should be copied to `deeprlanalyze/combined_outputs/`
* All `attNetStrings_*` and `defNetStrings_*` files should be copied to `deeprlanalyze/net_strings2/`
* All `attVMixed*` and `defVMixed*` files should be copied to `deeprlanalyze/learning_curves2/`

### Example visualization calls

```
python3 plot_mean_gains_stderror.py 3014 d30d1 d30m1

python3 plot_mean_regret_stderror.py game_3014_23.json game_3014_22_d30f1.json \
        d30n1 d30f1

python3 plot_payoffs_auto.py 3014 d30cd1 d30cd1_randNoAndB True

python3 plot_mean_payoffs.py 3014 d30cd1 d30cm1

python3 plot_gains_auto.py 3014 d30cd1 d30cd1_randNoAndB

python3 plot_mean_gains.py 3014 d30cd1 d30cm1

python3 plot_curves.py d30cd1 4

python3 heatmap.py d30cd1 d30cd1_randNoAndB

python3 plot_learning_curves.py 3014 d30cd1 d30cd1_randNoAndB

python3 merged_learning_curves.py 3014 d30cd1 d30cd1_randNoAndB

python3 analyze_learning.py s29m1

python3 analyze_retrain.py s29cs1 4

python3 runs_analyze.py game_comb_d30_cd1_cm35_n1_f1_2f25.json

python3 regret_analyze.py game_comb_d30_cd1_cm35_n1_f1_2f25.json > \
    out_regret_analyze_d30.txt
```

Note: `regret_analyze.py` must be called after `runs_analyze.py`, because it depends on
the Gambit result being generated already.

## How to merge two games into one JSON file

TODO

## How to analyze the regret of each game's strategies in a merged game file

TODO

## How to generate JSON output of a run and visualize game play

The data visualization code depends on [Graphviz](http://www.graphviz.org/). It can be
installed via `pip3 install graphviz`.

You can compile `src/DepgraphPy4JGreedyConfigNeitherNetworkCLIJson.java` or just 
run the JAR file in `dg4jnonetclijson/dg4jnonetclijson.jar`, to generate the JSON file
with a representation of the game's results.

```
java -jar dg4jnonetclijson.jar \
    vsRANDOM_WALK:logisParam_3.0_bThres_0.01_qrParam_3.0_numRWSample_30_isRandomized_1.0 \
    VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0 \
    RandomGraph30N100E6T1_B.json test_game_result.json 3
```

To visualize the results, copy your JSON output `test_game_result.json` (our your name) to `deeprlanalyze/`.

Now edit `vis_net_plus.py` to set the input file name to `test_game_result.json` (TODO: take as argument instead).

Call `python3 vis_net_plus.py`. The output will be a file like `out_*.gv.pdf` for each time step of one game result.

These outputs can be edited together into a GIF.
The GIF generation code depends on [Wand](http://docs.wand-py.org/en/0.4.5/). To install, you can use:

```
apt-get install libmagickwand-dev
sudo -H pip3 install --user Wand
```
