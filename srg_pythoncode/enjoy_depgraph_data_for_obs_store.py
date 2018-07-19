''' Play back the dependency graph network that was learned. '''
import sys
import time
import math
import random
import numpy as np
import gym

from baselines import deepq

def get_net_scope(net_name):
    # name is like:
    # *epochNUM_* or *epochNUM[a-z]* or *epochNUM.pkl, where NUM is an integer > 1,
    # unless "epoch" is absent, in which case return None.
    #
    # if "epoch" is absent: return None.
    # else if NUM is 2: return "deepq_train".
    # else: return "deepq_train_eNUM", inserting the integer for NUM
    if net_name == "dg_rand_30n_noAnd_B_eq_2.pkl":
        return "deepq"

    epoch_index = net_name.find('epoch')
    num_start_index = epoch_index + len("epoch")

    underbar_index = net_name.find('_', num_start_index + 1)
    dot_index = net_name.find('.', num_start_index + 1)
    e_index = net_name.find('e', num_start_index + 1)
    candidates = [x for x in [underbar_index, dot_index, e_index] if x > -1]
    num_end_index = min(candidates)
    net_num = net_name[num_start_index : num_end_index]
    if net_num == "2":
        return "deepq_train"
    return "deepq_train_e" + str(net_num)

def generate_obs(env_name_att_net, def_strat, num_episodes, obs_per_episode):
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        env_name = "DepgraphJava-v0"
        model_name = "depgraph_java_deepq_model2.pkl"
    '''
    start_time = time.time()

    env = gym.make(env_name_att_net)
    print("Environment: " + env_name_att_net)
    print("num_episodes: " + str(num_episodes))

    my_scope = get_net_scope(def_strat)
    print("scope: " + str(my_scope))

    act = deepq.load_with_scope(def_strat, my_scope)
    print("Model: " + def_strat)

    rewards = []
    selected_observations = []
    for _ in range(num_episodes):
        cur_episode_observations = []
        obs, done = env.reset(), False
        obs_len = len(obs)
        cur_episode_observations.append(obs.copy())
        obs = np.array(obs).reshape(1, obs_len)
        episode_rew = 0
        while not done:
            obs, rew, done, _ = env.step(act(obs)[0])
            cur_episode_observations.append(obs.copy())
            obs = np.array(obs).reshape(1, obs_len)
            episode_rew += rew
        rewards.append(episode_rew)
        for _ in range(obs_per_episode):
            choice_index = random.randint(0, len(cur_episode_observations) - 1)
            cur_obs = cur_episode_observations[choice_index]
            cur_obs = [int(x) for x in cur_obs]
            selected_observations.append(cur_obs)
    mean_reward = np.mean(rewards)
    stdev_reward = np.std(rewards)
    stderr_reward = stdev_reward * 1.0 / math.sqrt(num_episodes)

    duration = time.time() - start_time
    fmt = "{0:.2f}"
    print("Mean reward: " + fmt.format(mean_reward))
    print("Stdev reward: " + fmt.format(stdev_reward))
    print("Stderr reward: " + fmt.format(stderr_reward))
    print("Minutes taken: " + str(duration // 60))
    return selected_observations

'''
python3 enjoy_depgraph_data_for_obs_store.py DepgraphJavaEnvVsMixedAtt-v0 \
    depgraph_dq_mlp_rand_epoch14.pkl 100 5
'''
if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: env_name_att_net, def_strat, num_episodes, " + \
            "obs_per_episode")
    ENV_NAME_ATT_NET = sys.argv[1]
    DEF_STRAT = sys.argv[2]
    NUM_EPISODES = int(sys.argv[3])
    OBS_PER_EPISODE = int(sys.argv[4])
    generate_obs(ENV_NAME_ATT_NET, DEF_STRAT, NUM_EPISODES, OBS_PER_EPISODE)
