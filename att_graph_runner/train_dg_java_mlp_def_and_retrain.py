'''
Trains a multilayer perceptron to play the depgraph game against
an attacker that can mix over heuristic and network strategies.
'''
import sys
import time
import os
import os.path
import gym

from baselines import deepq

PORT_DIR = "../gym/gym/gym/envs/board_game/"

def unlock_train_def(port_lock_name):
    lock_name = PORT_DIR + port_lock_name + "_train_def_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def main(env_name_vs_att, env_short_name, new_epoch, def_port, port_lock_name, \
    env_short_name_tsv, max_timesteps_def_init, max_timesteps_def_retrain, retrain_iters):
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + ".pkl"
    if os.path.isfile(model_name):
        raise ValueError("Skipping: " + model_name + " already exists.")

    print("Environment: " + env_name_vs_att)

    start = time.time()
    env = gym.make(env_name_vs_att)
    if env.get_port() != def_port:
        raise ValueError("Wrong port: " + str(env.get_port()) + " vs. " + str(def_port))
    unlock_train_def(port_lock_name)

    strat_file = env_short_name_tsv + "_epoch" + str(new_epoch) + "_att.tsv"
    env.setup_att_mixed_strat(strat_file)

    model = deepq.models.mlp([256, 256])

    my_scope = "deepq_train"
    if new_epoch > 1:
        my_scope = "deepq_train_e" + str(new_epoch)

    prefix_for_save = "dg_" + env_short_name + "_dq_mlp_retrain_epoch" + str(new_epoch)
    retrain_config_str = env_short_name_tsv + "_epoch" + str(new_epoch) + "_retrain_att.tsv"
    deepq.learn_retrain_and_save(
        env,
        q_func=model,
        lr=5e-5,
        max_timesteps_init=max_timesteps_def_init,
        buffer_size=30000,
        exploration_fraction=0.5,
        exploration_final_eps=0.03,
        checkpoint_freq=30000,
        print_freq=250,
        param_noise=False,
        gamma=0.99,
        ep_mean_length=250,
        scope=my_scope,
        path_for_save=model_name,
        retrain_exploration_initial_eps=0.3,
        retrain_exploration_final_eps=0.03,
        retrain_save_count=retrain_iters,
        max_timesteps_retrain=max_timesteps_def_retrain,
        retrain_config_str=retrain_config_str,
        prefix_for_save=prefix_for_save
    )
    print("Saving model to: " + model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name_vs_att)

    sys.stdout.flush()
    os._exit(os.EX_OK)

if __name__ == '__main__':
    if len(sys.argv) != 10:
        raise ValueError("Need 9 args: env_name_vs_att, env_short_name, new_epoch, " + \
            "def_port, port_lock_name, env_short_name_tsv, max_timesteps_def_init, " + \
            "max_timesteps_def_retrain, retrain_iters")
    ENV_NAME_VS_ATT = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    DEF_PORT = int(sys.argv[4])
    PORT_LOCK_NAME = sys.argv[5]
    ENV_SHORT_NAME_TSV = sys.argv[6]
    MAX_TIMESTEPS_DEF_INIT = int(sys.argv[7])
    MAX_TIMESTEPS_DEF_RETRAIN = int(sys.argv[8])
    RETRAIN_ITERS = int(sys.argv[9])
    main(ENV_NAME_VS_ATT, ENV_SHORT_NAME, NEW_EPOCH, DEF_PORT, PORT_LOCK_NAME, \
        ENV_SHORT_NAME_TSV, MAX_TIMESTEPS_DEF_INIT, MAX_TIMESTEPS_DEF_RETRAIN, RETRAIN_ITERS)
