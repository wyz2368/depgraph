'''
Play dependency graph game, using network for defender only.
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

def get_payoffs_def_net_with_sd(env_name_def_net, num_sims, def_model_name, att_model_name, \
    graph_name):
    '''
    Get the mean payoff for defender and attacker, when the given defender network strategy
    plays against the given attacker heuristic, in the given environment.
    '''
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar dg4jdefcli/dg4jdefcli.jar " + att_model_name + " " + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)

    env = gym.make(env_name_def_net)

    defender, _, def_sess = deepq.load_for_multiple_nets(def_model_name)

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        obs, done = env.reset(), False
        while not done:
            with def_sess.as_default():
                obs = obs.reshape(1, obs.size)
                obs, _, done, _ = env.step(defender(obs)[0])
        def_rewards.append(env.get_self_reward())
        att_rewards.append(env.get_opponent_reward())

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

def get_payoffs_def_net(env_name_def_net, num_sims, def_model_name, att_model_name, \
    graph_name):
    '''
    Get the mean payoff for defender and attacker, when the given defender network strategy
    plays against the given attacker heuristic, in the given environment.
    '''
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar dg4jdefcli/dg4jdefcli.jar " + att_model_name + " " + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)

    env = gym.make(env_name_def_net)

    defender, _, def_sess = deepq.load_for_multiple_nets(def_model_name)

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        obs, done = env.reset(), False
        while not done:
            with def_sess.as_default():
                obs = obs.reshape(1, obs.size)
                obs, _, done, _ = env.step(defender(obs)[0])
        def_rewards.append(env.get_self_reward())
        att_rewards.append(env.get_opponent_reward())

    mean_def_reward = np.mean(def_rewards)
    mean_att_reward = np.mean(att_rewards)
    result = (mean_def_reward, mean_att_reward)
    print(result)

    env.close_gateway()

    # wait before stopping Java server
    time.sleep(sleep_sec)

    my_process.kill()
    return result

if __name__ == '__main__':
    if len(sys.argv) != 6:
        raise ValueError( \
            "Need 5 args: env_name_def_net, num_sims, def_model_name," + \
            " att_model_name, graph_name")
    ENV_NAME_DEF_NET = sys.argv[1]
    NUM_SIMS = int(float(sys.argv[2]))
    DEF_MODEL_NAME = sys.argv[3]
    ATT_MODEL_NAME = sys.argv[4]
    GRAPH_NAME = sys.argv[5]
    get_payoffs_def_net(ENV_NAME_DEF_NET, NUM_SIMS, DEF_MODEL_NAME, \
        ATT_MODEL_NAME, GRAPH_NAME)
