''' Play back the dependency graph network that was learned. '''
import time
import numpy as np
import gym
from baselines import deepq

def main():
    '''
        Load the network from file, and play games of the depdency
        graph game against opponent.

        env_name = "DepgraphJava-v0"
        model_name = "depgraph_java_deepq_model2.pkl"
    '''

    env_name = "DepgraphJava-v0"
    model_name = "depgraph_java_deepq_model2.pkl"
    num_episodes = 100

    env = gym.make(env_name)
    print("Environment: " + env_name)

    act = deepq.load(model_name)
    print("Model: " + model_name)

    move_sleep = 0.1
    game_sleep = 1.0
    total_reward = 0.0
    episode_count = 0
    while episode_count < num_episodes:
        obs, done = env.reset(), False
        obs_len = len(obs)
        obs = np.array(obs).reshape(1, obs_len)
        episode_rew = 0
        while not done:
            env.render()
            obs, rew, done, _ = env.step(act(obs)[0])
            obs = np.array(obs).reshape(1, obs_len)
            episode_rew += rew
            time.sleep(move_sleep)
            print("")
        env.render()
        print("Episode reward", episode_rew)
        total_reward += episode_rew
        episode_count += 1
        print("Mean reward: " + str(total_reward * 1.0 / episode_count))
        time.sleep(game_sleep)

if __name__ == '__main__':
    main()
