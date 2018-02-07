''' Play back the dependency graph network that was learned. '''
import time
import math
import numpy as np
import gym

from baselines import deepq

NODE_COUNT = 30

def main():
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        env_name = "DepgraphJavaConv-v0"
        model_name = "depgraph_java_deepq_conv_model3.pkl"
    '''

    env_name = "DepgraphJavaConv-v0"
    # model_name = "depgraph_java_deepq_conv_model3.pkl"
    # model_name = "depgraph_java_deepq_conv_model_conf2.pkl"
    # model_name = "depgraph_java_deepq_conv_model_conf2_d3.pkl"
    # model_name = "depgraph_java_deepq_conv_model_d3_2.pkl"
    # model_name = "depgraph_java_deepq_conv_model_conf2_d4.pkl"
    # model_name = "depgraph_java_deepq_conv_model_conf2_d3_3.pkl"
    model_name = "depgraph_java_deepq_conv_model_conf2_d3_big.pkl"
    # depgraph_java_deepq_conv_model_conf2
    num_episodes = 1000
    OBS_LENGTH = 3
    INPUT_DEPTH = 2 + 2 * OBS_LENGTH

    start_time = time.time()

    env = gym.make(env_name)
    print("Environment: " + env_name)

    act = deepq.load(model_name)
    print("Model: " + model_name)

    rewards = []
    for _ in range(num_episodes):
        obs, done = env.reset(), False
        obs = np.array(obs).reshape(1, 1, INPUT_DEPTH, NODE_COUNT)
        episode_rew = 0
        while not done:
            obs, rew, done, _ = env.step(act(obs)[0])
            obs = np.array(obs).reshape(1, 1, INPUT_DEPTH, NODE_COUNT)
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

if __name__ == '__main__':
    main()
