'''
Play dependency graph game, using no network for defender or attacker.
Requirements:
    Py4J        https://www.py4j.org/download.html
'''
import sys
import subprocess
import time
import numpy as np
from depgraph_java_no_net import DepgraphJavaEnvAttNoNet

def get_payoffs_no_net_with_sd(num_sims, def_model_name, att_model_name, graph_name):
    '''
    Get the mean payoff for defender and attacker, when the given defender heuristic strategy
    plays against the given attacker heuristic, in the given environment.
    '''
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar dg4jnonetcli/dg4jnonetcli.jar " + def_model_name + " " + \
        att_model_name + " " + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)

    env = DepgraphJavaEnvAttNoNet()
    env.init()

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        env.reset_and_run_once()
        def_rewards.append(env.get_defender_reward())
        att_rewards.append(env.get_attacker_reward())

    mean_def_reward = np.mean(def_rewards)
    mean_att_reward = np.mean(att_rewards)
    stdev_def_reward = np.std(def_rewards)
    stdev_att_reward = np.std(att_rewards)
    result = (mean_def_reward, mean_att_reward, stdev_def_reward, stdev_att_reward)
    print(result)

    env.close_gateway()

    # wait before stopping Java server
    time.sleep(sleep_sec)

    my_process.kill()
    return result

def get_payoffs_no_net(num_sims, def_model_name, att_model_name, graph_name):
    '''
    Get the mean payoff for defender and attacker, when the given defender heuristic strategy
    plays against the given attacker heuristic, in the given environment.
    '''
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar dg4jnonetcli/dg4jnonetcli.jar " + def_model_name + " " + \
        att_model_name + " " + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)

    env = DepgraphJavaEnvAttNoNet()
    env.init()

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        env.reset_and_run_once()
        def_rewards.append(env.get_defender_reward())
        att_rewards.append(env.get_attacker_reward())

    mean_def_reward = np.mean(def_rewards)
    mean_att_reward = np.mean(att_rewards)
    result = (mean_def_reward, mean_att_reward)
    print(result)

    env.close_gateway()

    # wait before stopping Java server
    time.sleep(sleep_sec)

    my_process.kill()
    return result

'''
python3 cli_enjoy_dg_no_net.py 5 vsVALUE_PROPAGATION:maxNumRes_10.0_minNumRes_2.0_numResRatio_0.5_logisParam_3.0_maxNumAttCandidate_10.0_minNumAttCandidate_2.0_numAttCandidateRatio_0.5_qrParam_1.0_bThres_0.01_numAttCandStdev_0.0_isTopo_0.0 VALUE_PROPAGATION:maxNumSelectCandidate_10.0_minNumSelectCandidate_2.0_numSelectCandidateRatio_0.5_qrParam_3.0_stdev_0.0 RandomGraph30N100E2T1.json
'''
if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError( \
            "Need 4 args: num_sims, def_model_name," + \
            " att_model_name, graph_name")
    NUM_SIMS = int(float(sys.argv[1]))
    DEF_MODEL_NAME = sys.argv[2]
    ATT_MODEL_NAME = sys.argv[3]
    GRAPH_NAME = sys.argv[4]
    get_payoffs_no_net(NUM_SIMS, DEF_MODEL_NAME, \
        ATT_MODEL_NAME, GRAPH_NAME)
