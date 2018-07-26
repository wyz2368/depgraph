'''
Play dependency graph game, using separate network for attacker and defender.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import sys
import subprocess
import time
import numpy as np
import gym
from baselines import deepq

def get_payoffs_both_with_sd(env_name_both, num_sims, def_model_name, att_model_name, \
    graph_name, def_scope, att_scope):
    '''
    Get the mean payoff for defender and attacker, when the given network strategies play
    against each other in the given environment.
    '''
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar ../depgraphpy4jboth/depgraphpy4jconfigboth.jar simspecs/ "\
        + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)

    env = gym.make(env_name_both)

    if def_scope is None:
        defender, _, def_sess = deepq.load_for_multiple_nets(def_model_name)
    else:
        defender, _, def_sess = deepq.load_for_multiple_nets_with_scope( \
            def_model_name, def_scope)
    if att_scope is None:
        attacker, _, att_sess = deepq.load_for_multiple_nets(att_model_name)
    else:
        attacker, _, att_sess = deepq.load_for_multiple_nets_with_scope( \
            att_model_name, att_scope)

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        obs, done, is_def_turn = env.reset(), False, True
        while not done:
            cur_agent = defender
            cur_sess = def_sess
            if not is_def_turn:
                cur_agent = attacker
                cur_sess = att_sess
            with cur_sess.as_default():
                obs, done, _, is_def_turn = env.step(cur_agent(obs)[0])
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

def get_payoffs_both(env_name_both, num_sims, def_model_name, att_model_name, graph_name, \
    def_scope, att_scope):
    '''
    Get the mean payoff for defender and attacker, when the given network strategies play
    against each other in the given environment.
    '''
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar ../depgraphpy4jboth/depgraphpy4jconfigboth.jar simspecs/ "\
        + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)

    env = gym.make(env_name_both)

    if def_scope is None:
        defender, _, def_sess = deepq.load_for_multiple_nets(def_model_name)
    else:
        defender, _, def_sess = deepq.load_for_multiple_nets_with_scope( \
            def_model_name, def_scope)
    if att_scope is None:
        attacker, _, att_sess = deepq.load_for_multiple_nets(att_model_name)
    else:
        attacker, _, att_sess = deepq.load_for_multiple_nets_with_scope( \
            att_model_name, att_scope)

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        obs, done, is_def_turn = env.reset(), False, True
        while not done:
            cur_agent = defender
            cur_sess = def_sess
            if not is_def_turn:
                cur_agent = attacker
                cur_sess = att_sess
            with cur_sess.as_default():
                obs, done, _, is_def_turn = env.step(cur_agent(obs)[0])
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

# DepgraphJavaEnvBoth-v0 100 dg_rand_30n_noAnd_B_eq_2.pkl dg_dqmlp_rand30NoAnd_B_att_fixed.pkl
# RandomGraph30N100E6T1_B.json
if __name__ == '__main__':
    if len(sys.argv) != 8:
        raise ValueError("Need 7 args: env_name_both, num_sims, def_model_name, " + \
            "att_model_name, graph_name, def_scope, att_scope")
    ENV_NAME_BOTH = sys.argv[1]
    NUM_SIMS = int(float(sys.argv[2]))
    DEF_MODEL_NAME = sys.argv[3]
    ATT_MODEL_NAME = sys.argv[4]
    GRAPH_NAME = sys.argv[5]
    DEF_SCOPE = sys.argv[6]
    ATT_SCOPE = sys.argv[7]
    if DEF_SCOPE == "None":
        DEF_SCOPE = None
    if ATT_SCOPE == "None":
        ATT_SCOPE = None
    get_payoffs_both(ENV_NAME_BOTH, NUM_SIMS, DEF_MODEL_NAME, ATT_MODEL_NAME, GRAPH_NAME, \
        DEF_SCOPE, ATT_SCOPE)
