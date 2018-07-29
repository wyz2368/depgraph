''' Play back the dependency graph network that was learned. '''
import sys
import time
import math
import os
import numpy as np
import gym

from baselines import deepq

PORT_DIR = "../gym/gym/gym/envs/board_game/"

def unlock_eval_att(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_eval_att_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def main(env_name, env_short_name, new_epoch, att_port, port_lock_name, env_short_name_tsv):
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        model_name = "depgraph_java_deepq_model2.pkl"
    '''
    model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + "_att.pkl"
    num_episodes = 1000

    start_time = time.time()

    env = gym.make(env_name)
    if env.get_port() != att_port:
        raise ValueError("Wrong port: " + str(env.get_port()) + " vs. " + str(att_port))
    unlock_eval_att(port_lock_name)
    print("Environment: " + env_name)

    strat_file = env_short_name_tsv + "_epoch" + str(new_epoch) + "_def.tsv"
    env.setup_def_mixed_strat(strat_file)

    my_scope = "deepq_train"
    if new_epoch > 1:
        my_scope = "deepq_train_e" + str(new_epoch)

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

    sys.stdout.flush()
    os._exit(os.EX_OK)

'''
python3 enjoy_dg_data_vs_mixed_def.py DepgraphJavaEnvVsMixedDef29N-v0 sl29 15 25335 s29 \
    sl29_randNoAndB
'''
if __name__ == '__main__':
    if len(sys.argv) != 7:
        raise ValueError("Need 6 args: env_name_def_net, env_short_name, new_epoch, " + \
            "att_port, env_short_name_tsv")
    ENV_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ATT_PORT = int(sys.argv[4])
    PORT_LOCK_NAME = sys.argv[5]
    ENV_SHORT_NAME_TSV = sys.argv[6]
    main(ENV_NAME, ENV_SHORT_NAME, NEW_EPOCH, ATT_PORT, PORT_LOCK_NAME, ENV_SHORT_NAME_TSV)
