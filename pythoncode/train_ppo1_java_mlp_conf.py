'''
Trains a multilayer perceptron to play the depgraph game against a mixed strategy agent,
using PPO1 from OpenAI baselines.
https://github.com/openai/baselines/tree/master/baselines/ppo1
'''
import time

import gym
from baselines.ppo1 import pposgd_simple, mlp_policy
import baselines.common.tf_util as U

# import cloudpickle

def main():
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    and trains the model with PPO.
    '''
    env_name = 'DepgraphJava-v0'
    print("Environment: " + env_name)
    num_timesteps = 600000

    start = time.time()

    env = gym.make(env_name)
    sess = U.single_threaded_session()
    sess.__enter__()
    def policy_fn(name, ob_space, ac_space):
        '''
        Makes the multilayer perceptron policy model.
        '''
        return mlp_policy.MlpPolicy(name=name, ob_space=ob_space, ac_space=ac_space,
                                    hid_size=256, num_hid_layers=2)

    pposgd_simple.learn(env, policy_fn,
                        max_timesteps=num_timesteps,
                        timesteps_per_actorbatch=2048,
                        clip_param=0.1, entcoeff=0.01,
                        optim_epochs=3, optim_stepsize=2.5e-4, optim_batchsize=1024,
                        gamma=0.99, lam=0.95,
                        schedule='constant'
                       )
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == "__main__":
    main()
