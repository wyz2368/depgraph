'''
Play dependency graph game, using separate network for attacker and defender.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import sys
import subprocess
import numpy as np
import gym

from baselines import deepq

def get_payoffs_both(env_name_both, num_sims, def_model_name, att_model_name, graph_name):
    '''
    Get the mean payoff for defender and attacker, when the given network strategies play
    against each other in the given environment.
    '''
    # see also:
    # https://stackoverflow.com/questions/4789837/
    #     how-to-terminate-a-python-subprocess-launched-with-shell-true
    cmd = "exec java -jar ../depgraphpy4jboth/depgraphpy4jconfigboth.jar simspecs/ " \
        + graph_name
    my_process = subprocess.Popen(cmd, shell=True)

    env = gym.make(env_name_both)

    defender = deepq.load(def_model_name)
    attacker = deepq.load(att_model_name)

    def_rewards = []
    att_rewards = []
    for _ in range(num_sims):
        obs, done, is_def_turn = env.reset(), False, True
        while not done:
            cur_agent = defender
            if not is_def_turn:
                cur_agent = attacker
            obs, done, _, is_def_turn = env.step(cur_agent(obs)[0])
        def_rewards.append(env.get_defender_reward())
        att_rewards.append(env.get_attacker_reward())
    mean_def_reward = np.mean(def_rewards)
    mean_att_reward = np.mean(att_rewards)

    my_process.kill()
    return (mean_def_reward, mean_att_reward)

# DepgraphJavaEnvBoth-v0 100 dg_rand_30n_noAnd_B_eq_2.pkl dg_dqmlp_rand30NoAnd_B_att_fixed.pkl
# RandomGraph30N100E6T1_B.json
if __name__ == '__main__':
    if len(sys.argv) != 6:
        raise ValueError("Need 5 args: env_name_both, num_sims, def_model_name, " + \
            "att_model_name, graph_name")
    ENV_NAME_BOTH = sys.argv[1]
    NUM_SIMS = int(float(sys.argv[2]))
    DEF_MODEL_NAME = sys.argv[3]
    ATT_MODEL_NAME = sys.argv[4]
    GRAPH_NAME = sys.argv[5]
    get_payoffs_both(ENV_NAME_BOTH, NUM_SIMS, DEF_MODEL_NAME, ATT_MODEL_NAME, GRAPH_NAME)
