'''
Play dependency graph game, using separate network for attacker and defender.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''
import time
import math
import numpy as np
import gym

from baselines import deepq

def play_game():
    '''
        Plays a round of the dependency graph game, with a network for each side.
    '''
    env_name = "DepgraphJavaEnvBoth-v0"
    def_model_name = "depgraph_java_deepq_mlp_long.pkl"
    att_model_name = "dg_dqmlp_rand30NoAnd_B_att.pkl"
    num_episodes = 1000

    start_time = time.time()

    env = gym.make(env_name)
    print("Environment: " + env_name)

    defender = deepq.load(def_model_name)
    attacker = deepq.load(att_model_name)
    print("Defender model: " + def_model_name)
    print("Attacker model: " + att_model_name)

    def_rewards = []
    att_rewards = []
    for _ in range(num_episodes):
        obs, done, is_def_turn = env.reset(), False, True
        while not done:
            cur_agent = defender
            if not is_def_turn:
                cur_agent = attacker
            obs, done, _, is_def_turn = env.step(cur_agent(obs)[0])
        def_rewards.append(env.get_defender_reward())
        att_rewards.append(env.get_attacker_reward())
    mean_def_reward = np.mean(def_rewards)
    stdev_def_reward = np.std(def_rewards)
    stderr_def_reward = stdev_def_reward * 1.0 / math.sqrt(num_episodes)

    mean_att_reward = np.mean(att_rewards)
    stdev_att_reward = np.std(att_rewards)
    stderr_att_reward = stdev_att_reward * 1.0 / math.sqrt(num_episodes)

    duration = time.time() - start_time
    fmt = "{0:.2f}"
    print("Mean def reward: " + fmt.format(mean_def_reward))
    print("Stdev def reward: " + fmt.format(stdev_def_reward))
    print("Stderr def reward: " + fmt.format(stderr_def_reward))
    print("Mean att reward: " + fmt.format(mean_att_reward))
    print("Stdev att reward: " + fmt.format(stdev_att_reward))
    print("Stderr att reward: " + fmt.format(stderr_att_reward))
    print("Minutes taken: " + str(duration // 60))

if __name__ == '__main__':
    play_game()
