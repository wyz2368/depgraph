'''
Trains a convolutional network to play the depgraph game against a mixed strategy agent,
using PPO1 from OpenAI baselines.
https://github.com/openai/baselines/tree/master/baselines/ppo1
'''
import time
import gym

from baselines.ppo1 import pposgd_simple, cnn_policy
import baselines.common.tf_util as U

def main():
    '''
    Makes the depgraph environment, builds a multilayer convolutional network model,
    and trains the model with PPO.
    '''
    env_name = 'DepgraphJavaConv-v0'
    print("Environment: " + env_name)
    num_timesteps = 600000

    start = time.time()

    env = gym.make(env_name)
    sess = U.single_threaded_session()
    sess.__enter__()
    def policy_fn(name, ob_space, ac_space):
        '''
        Makes the convolutional network policy model.
        '''
        return cnn_policy.CnnPolicy(name=name, ob_space=ob_space, ac_space=ac_space, \
            kind='depgraph')

    pposgd_simple.learn(env, policy_fn,
                        max_timesteps=num_timesteps,
                        timesteps_per_actorbatch=256,
                        clip_param=0.2, entcoeff=0.01,
                        optim_epochs=4, optim_stepsize=1e-3, optim_batchsize=64,
                        gamma=0.99, lam=0.95,
                        schedule='linear'
                       )

    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == "__main__":
    main()
