''' Play back the dependency graph network that was learned. '''
import sys
import time
import math
import numpy as np
import gym

from baselines import deepq

PORT_DIR = "~/gym/gym/gym/envs/board_game/"

def unlock_eval_def(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_eval_def_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def main(env_name, env_short_name, new_epoch, def_port, port_lock_name):
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        env_name = "DepgraphJava-v0"
        model_name = "depgraph_java_deepq_model2.pkl"
    '''

    model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + ".pkl"
    num_episodes = 1000

    start_time = time.time()

    env = gym.make(env_name)
    if env.get_port() != def_port:
        raise ValueError("Wrong port: " + str(env.get_port()) + " vs. " + str(def_port))
    unlock_eval_def(port_lock_name)
    print("Environment: " + env_name)

    strat_file = env_short_name + "_epoch" + str(new_epoch) + "_att.tsv"
    env.setup_att_mixed_strat(strat_file)

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

'''
python3 enjoy_depgraph_data_vs_mixed.py DepgraphJavaEnvVsMixedAtt29N-v0 sl29 15 25333 s29
'''
if __name__ == '__main__':
    if len(sys.argv) != 6:
        raise ValueError("Need 5 args: env_name_att_net, env_short_name, new_epoch, " + \
            "def_port, port_lock_name")
    ENV_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    DEF_PORT = int(sys.argv[4])
    PORT_LOCK_NAME = sys.argv[5]
    main(ENV_NAME, ENV_SHORT_NAME, NEW_EPOCH, DEF_PORT, PORT_LOCK_NAME)
