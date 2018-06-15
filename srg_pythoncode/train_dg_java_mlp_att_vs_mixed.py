'''
Trains a multilayer perceptron to play the depgraph game against
a defender that can mix over heuristic and network strategies.
'''
import sys
import time
import os.path
import gym

from baselines import deepq

def main(env_name, new_epoch):
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    model_name = "dg_dqmlp_rand30NoAnd_B_epoch" + str(new_epoch) + "_att.pkl"
    if os.path.isfile(model_name):
        raise ValueError("Skipping: " + model_name + " already exists.")

    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.mlp([256, 256])

    my_scope = "deepq_train"
    if new_epoch > 2:
        my_scope = "deepq_train_e" + str(new_epoch)
    elif new_epoch == 0:
        my_scope = None

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

if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: env_name_mixed_def, new_epoch")
    ENV_NAME = sys.argv[1]
    NEW_EPOCH = int(sys.argv[3])
    main(ENV_NAME, NEW_EPOCH)
