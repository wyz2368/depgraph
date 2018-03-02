''' Play back the dependency graph network that was learned. '''
import sys
import numpy as np
import gym

from baselines import deepq

def get_mean_payoffs(num_episodes, model_name):
    '''
        Load the network from file, and play games of the dependency
        graph game against opponent.
    '''

    env_name = "DepgraphJava29N-v0"
    env = gym.make(env_name)
    act = deepq.load(model_name)

    rewards = []
    att_rewards = []
    for _ in range(num_episodes):
        obs, done = env.reset(), False
        obs_len = len(obs)
        obs = np.array(obs).reshape(1, obs_len)
        episode_rew = 0
        while not done:
            obs, rew, done, _ = env.step(act(obs)[0])
            obs = np.array(obs).reshape(1, obs_len)
            episode_rew += rew
        rewards.append(episode_rew)
        att_rewards.append(env.get_opponent_reward())
    mean_reward = np.mean(rewards)
    mean_att_reward = np.mean(att_rewards)
    return (mean_att_reward, mean_reward)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: num_episodes, model_name")
    NUM_EPISODES_INPUT = int(float(sys.argv[1]))
    if NUM_EPISODES_INPUT < 1:
        raise ValueError("num_episodes must be >= 1")
    MODEL_NAME_INPUT = sys.argv[2]
    print(get_mean_payoffs(NUM_EPISODES_INPUT, MODEL_NAME_INPUT))
