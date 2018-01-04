''' Play back the Connect Four network that was learned. '''
import time
import gym

from baselines import deepq

def main():
    ''' Load the network from file, and play games of Connect Four against opponent. '''

    '''
    env_name = "Connect4-v0"
    model_name = "c4_deepq_model.pkl"
    '''

    env_name = "Connect4Max-v0"
    model_name = "c4_deepq_d1_model.pkl"

    env = gym.make(env_name)
    print("Environment: " + env_name)

    act = deepq.load(model_name)
    print("Model: " + model_name)

    move_sleep = 0.1
    game_sleep = 1.0
    total_reward = 0.0
    episode_count = 0
    while True:
        obs, done = env.reset(), False
        episode_rew = 0
        while not done:
            env.render()
            obs, rew, done, _ = env.step(act(obs[None])[0])
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
