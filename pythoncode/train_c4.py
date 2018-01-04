''' Trains a multilayer perceptron to play Connect Four against a random agent. '''
import time
import gym

from baselines import deepq

def callback(lcl, glb):
    ''' Indicates training should stop if mean reward is at least 0.5 over 100 episodes. '''
    is_solved = lcl['t'] > 100 and sum(lcl['episode_rewards'][-101:-1]) / 100 >= 0.5
    return is_solved

def main():
    '''
    Makes the Connect Four environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    env_name = "Connect4-v0"
    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.mlp([64])
    act = deepq.learn(
        env,
        q_func=model,
        lr=1e-3,
        max_timesteps=10000,
        buffer_size=5000,
        exploration_fraction=0.1,
        exploration_final_eps=0.02,
        print_freq=10,
        param_noise=False,
        callback=callback
    )
    print("Saving model to c4_deepq_model.pkl")
    act.save("c4_deepq_model.pkl")
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == '__main__':
    main()
