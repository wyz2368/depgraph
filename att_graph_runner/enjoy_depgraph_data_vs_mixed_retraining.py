''' Play back the dependency graph network that was learned. '''
import sys
import time
import math
import numpy as np
import gym

from baselines import deepq

PORT_DIR = "../gym/gym/gym/envs/board_game/"

def unlock_eval_def(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_eval_def_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def main(env_name_vs_att, env_short_name, new_epoch, retrain_number, def_port, \
    port_lock_name, env_short_name_tsv, is_vs_retrain, old_strat_disc_fact, num_episodes):
    '''
    Load the network from file, and play games of the depdency
    graph game against opponent.
    '''
    model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + ".pkl"
    if retrain_number > 0:
        model_name = "dg_" + env_short_name + "_dq_mlp_retrain_epoch" + str(new_epoch) + \
            "_r" + str(retrain_number) + ".pkl"

    start_time = time.time()

    env = gym.make(env_name_vs_att)
    if env.get_port() != def_port:
        raise ValueError("Wrong port: " + str(env.get_port()) + " vs. " + str(def_port))
    unlock_eval_def(port_lock_name)
    print("Environment: " + env_name_vs_att)

    strat_file = env_short_name_tsv + "_epoch" + str(new_epoch) + "_att.tsv"
    if is_vs_retrain:
        fmt = "{0:.2f}"
        strat_file = env_short_name_tsv + "_epoch" + str(new_epoch) + "_mixed" + \
            fmt.format(old_strat_disc_fact).replace('.', '_') + "_att.tsv"
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

def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

'''
example: python3 enjoy_depgraph_data_vs_mixed_retraining.py \
    DepgraphJavaEnvVsMixedAtt29N-v0 s29m1 7 0 25333 s29 sl29_randNoAndB True 0.7 1000
'''
if __name__ == '__main__':
    if len(sys.argv) != 10:
        raise ValueError("Need 9 args: env_name_vs_att, env_short_name, new_epoch, " + \
            "retrain_number, def_port, port_lock_name, env_short_name_tsv, " + \
            "is_vs_retrain, old_strat_disc_fact, new_eval_count")
    ENV_NAME_VS_ATT = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    RETRAIN_NUMBER = int(sys.argv[4])
    DEF_PORT = int(sys.argv[5])
    PORT_LOCK_NAME = sys.argv[6]
    ENV_SHORT_NAME_TSV = sys.argv[7]
    IS_VS_RETRAIN = get_truth_value(sys.argv[8])
    OLD_STRAT_DISC_FACT = float(sys.argv[9])
    NEW_EVAL_COUNT = int(sys.argv[10])
    main(ENV_NAME_VS_ATT, ENV_SHORT_NAME, NEW_EPOCH, RETRAIN_NUMBER, DEF_PORT, \
        PORT_LOCK_NAME, ENV_SHORT_NAME_TSV, IS_VS_RETRAIN, OLD_STRAT_DISC_FACT, \
        NEW_EVAL_COUNT)
