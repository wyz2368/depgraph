''' Play back the dependency graph network that was learned. '''
import sys
import time
import math
import numpy as np
import gym

from baselines import deepq

def main(env_name, model_name, my_scope, num_episodes):
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        model_name = "depgraph_java_deepq_model2.pkl"
    '''
    start_time = time.time()

    env = gym.make(env_name)
    print("Environment: " + env_name)

    act = deepq.load_with_scope(model_name, my_scope)
    print("Model: " + model_name)

    rewards = []
    for _ in range(num_episodes):
        obs, done = env.reset(), False
        obs_len = len(obs)
        obs = np.array(obs).reshape(1, obs_len)
        episode_rew = 0
        while not done:
            obs, rew, done, _ = env.step(act(obs)[0])
            obs = np.array(obs).reshape(1, obs_len)
            episode_rew += rew
        rewards.append(episode_rew)
    mean_reward = np.mean(rewards)
    stdev_reward = np.std(rewards)
    stderr_reward = stdev_reward * 1.0 / math.sqrt(num_episodes)

    duration = time.time() - start_time
    fmt = "{0:.2f}"
    print("Mean reward: " + fmt.format(mean_reward))
    print("Stdev reward: " + fmt.format(stdev_reward))
    print("Stderr reward: " + fmt.format(stderr_reward))
    print("Minutes taken: " + str(duration // 60))

'''
example: python3 enjoy_dg_data_vs_mixed_def_mod_net.py DepgraphJavaEnvVsMixedDef29N-v0 \
    dg_s29_dq_mlp_rand_epoch7_afterRetrain_att_r1.pkl deepq_train_e7_retrained 400
'''
if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: env_name_def_net, model_name, scope, num_episodes")
    ENV_NAME_DEF_NET = sys.argv[1]
    MODEL_NAME = sys.argv[2]
    SCOPE = sys.argv[3]
    NUM_EPISODES = int(sys.argv[4])
    main(ENV_NAME_DEF_NET, MODEL_NAME, SCOPE, NUM_EPISODES)
