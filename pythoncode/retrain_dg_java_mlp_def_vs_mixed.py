'''
Trains a multilayer perceptron to play the depgraph game against
an attacker that can mix over heuristic and network strategies.
'''
import sys
import time
import os.path
import gym

from baselines import deepq

def main(env_name, env_short_name, new_epoch):
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    to_load_model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + \
        ".pkl"
    to_save_model_name = "dg_" + env_short_name + "_dq_mlp_rand_epoch" + str(new_epoch) + \
        "_afterRetrain.pkl"
    if os.path.isfile(to_save_model_name):
        raise ValueError("Skipping: " + to_save_model_name + " already exists.")

    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.mlp([256, 256])

    my_scope_old = "deepq_train"
    if new_epoch > 1:
        my_scope_old = "deepq_train_e" + str(new_epoch)

    my_scope_new = my_scope_old + "_retrained"

    # must be loaded by TensorFlow so accessible from global variables
    deepq.load_with_scope(to_load_model_name, my_scope_old)

    deepq.retrain_and_save(
        env,
        q_func=model,
        lr=5e-5,
        max_timesteps=3000,
        buffer_size=30000,
        exploration_fraction=0.5,
        exploration_final_eps=0.03,
        checkpoint_freq=30000,
        print_freq=250,
        param_noise=False,
        gamma=0.99,
        ep_mean_length=250,
        scope_old=my_scope_old,
        scope_new=my_scope_new,
        path_for_save=to_save_model_name,
    )
    print("Saving model to: " + to_save_model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

'''
example: python3 retrain_dg_java_mlp_def_vs_mixed.py DepgraphJavaEnvVsMixedAtt29N-v0 sl29 18
'''
if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError("Need 3 args: env_name_mixed_att, env_short_name, new_epoch")
    ENV_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    main(ENV_NAME, ENV_SHORT_NAME, NEW_EPOCH)
