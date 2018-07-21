'''
Trains a multilayer perceptron to play the depgraph game against
a defender that can mix over heuristic and network strategies.
'''
import sys
import time
import os.path
import gym

from baselines import deepq

def unlock_train_att(env_short_name):
    lock_name = env_short_name + "_train_att_lock.txt"
    with open(lock_name, 'w') as file:
        file.write("0\n")

def main(env_name, env_short_name, new_epoch, att_port):
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + "_att.pkl"
    if os.path.isfile(model_name):
        raise ValueError("Skipping: " + model_name + " already exists.")

    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    if env.get_port() != att_port:
        raise ValueError("Wrong port: " + str(env.get_port()) + " vs. " + str(att_port))
    unlock_train_att(env_short_name)

    strat_file = env_short_name + "_epoch" + str(new_epoch) + "_def.tsv"
    env.setup_att_mixed_strat(strat_file)

    model = deepq.models.mlp([256, 256])

    my_scope = "deepq_train"
    if new_epoch > 1:
        my_scope = "deepq_train_e" + str(new_epoch)

    deepq.learn_and_save(
        env,
        q_func=model,
        lr=5e-5,
        max_timesteps=700000,
        buffer_size=30000,
        exploration_fraction=0.5,
        exploration_final_eps=0.03,
        checkpoint_freq=30000,
        print_freq=250,
        param_noise=False,
        gamma=0.99,
        ep_mean_length=250,
        scope=my_scope,
        path_for_save=model_name
    )
    print("Saving model to: " + model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

'''
example: python3 train_dg_java_mlp_att_vs_mixed.py DepgraphJavaEnvVsMixedDef29N-v0 sl29 15 \
    25335
'''
if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: env_name_mixed_def, env_short_name, new_epoch, " + \
            "att_port")
    ENV_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ATT_PORT = int(sys.argv[4])
    main(ENV_NAME, ENV_SHORT_NAME, NEW_EPOCH, ATT_PORT)
