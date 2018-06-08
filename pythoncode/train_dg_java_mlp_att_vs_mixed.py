'''
Trains a multilayer perceptron to play the depgraph game against
a defender that can mix over heuristic and network strategies.
'''
import sys
import time
import gym

from baselines import deepq

def main(env_name):
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.mlp([256, 256])
    model_name = "dg_sl29_dq_mlp_rand_epoch14_att.pkl"
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
        scope="deepq_train_e14",
        path_for_save=model_name
    )
    print("Saving model to: " + model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

# example: python3 train_dg_java_mlp_att_vs_mixed.py DepgraphJavaEnvVsMixedDef29N-v0
if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError("Need 1 arg: env_name_def_net")
    ENV_NAME = sys.argv[1]
    main(ENV_NAME)

