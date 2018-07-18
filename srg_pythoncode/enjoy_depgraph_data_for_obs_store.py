''' Play back the dependency graph network that was learned. '''
import sys
import time
import math
import random
import numpy as np
import gym

from baselines import deepq

def main(env_name, env_short_name, new_epoch, num_episodes):
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        env_name = "DepgraphJava-v0"
        model_name = "depgraph_java_deepq_model2.pkl"
    '''

    # model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + ".pkl"
    model_name = "depgraph_dq_mlp_rand_epoch" + str(new_epoch) + ".pkl"

    start_time = time.time()

    env = gym.make(env_name)
    print("Environment: " + env_name)
    print("num_episodes: " + str(num_episodes))

    my_scope = "deepq_train"
    if new_epoch > 2:
        my_scope = "deepq_train_e" + str(new_epoch)
    elif new_epoch == 2:
        my_scope = "deepq_train"
    else:
        my_scope = None

    act = deepq.load_with_scope(model_name, my_scope)
    print("Model: " + model_name)

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
        choice_index = random.randint(0, len(cur_episode_observations) - 1)
        selected_observations.append(cur_episode_observations[choice_index])
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
python3 enjoy_depgraph_data_for_obs_store.py DepgraphJavaEnvVsMixedAtt29N-v0 sl29 15 400
'''
if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: env_name_att_net, env_short_name, new_epoch, " + \
            "num_episodes")
    ENV_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    NUM_EPISODES = int(sys.argv[4])
    main(ENV_NAME, ENV_SHORT_NAME, NEW_EPOCH, NUM_EPISODES)
