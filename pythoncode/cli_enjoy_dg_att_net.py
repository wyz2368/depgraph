'''
Play dependency graph game, using network for attacker only.

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

def get_payoffs_att_net(env_name_att_net, num_sims, def_model_name, att_model_name, \
    graph_name):
    '''
    Get the mean payoff for defender and attacker, when the given attacker network strategy
    plays against the given defender heuristic, in the given environment.
    '''
    # TODO:
    # version of depgraphpy4jattconfig.jar that assumes 'simspecs/' is the sim spec file,
    # and takes the name of the opponent pure strategy instead of a mixed strategy file
    # name.
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar dg4jattcli.jar " + def_model_name + " " + graph_name
    my_process = subprocess.Popen(cmd, shell=True)
    sleep_sec = 5
    # wait for Java server to start
    time.sleep(sleep_sec)

    env = gym.make(env_name_att_net)

    attacker, _, att_sess = deepq.load_for_multiple_nets(att_model_name)

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        obs, done = env.reset(), False
        while not done:
            with att_sess.as_default():
                obs, _, done, _ = env.step(attacker(obs)[0])
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

if __name__ == '__main__':
    if len(sys.argv) != 6:
        raise ValueError( \
            "Need 5 args: env_name_att_net, num_sims, def_model_name," + \
            " att_model_name, graph_name")
    ENV_NAME_ATT_NET = sys.argv[1]
    NUM_SIMS = int(float(sys.argv[2]))
    DEF_MODEL_NAME = sys.argv[3]
    ATT_MODEL_NAME = sys.argv[4]
    GRAPH_NAME = sys.argv[5]
    get_payoffs_att_net(ENV_NAME_ATT_NET, NUM_SIMS, DEF_MODEL_NAME, \
        ATT_MODEL_NAME, GRAPH_NAME)
