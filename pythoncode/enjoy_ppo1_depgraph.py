''' Play back the dependency graph network that was learned. '''
import time
import numpy as np
import gym
import tensorflow as tf

from baselines.ppo1 import pposgd_simple, cnn_policy

def main():
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        env_name = "DepgraphJavaConv-v0"
        model_name = "depgraph_java_ppo_conv_model_conf.ckpt"
    '''

    env_name = "DepgraphJavaConv-v0"
    model_name = "depgraph_java_ppo_conv_model_conf.ckpt"
    num_episodes = 1000

    start_time = time.time()

    env = gym.make(env_name)
    print("Environment: " + env_name)

    sess = tf.Session()    
    saver = tf.train.import_meta_graph(model_name + '.meta')
    saver.restore(sess,tf.train.latest_checkpoint('./'))
    print(saver)

    '''
    seg_gen = pposgd_simple.traj_segment_generator(saver, env, \
        horizon=256, stochastic=True)
    seg = seg_gen.__next__()
    '''

if __name__ == '__main__':
    main()
