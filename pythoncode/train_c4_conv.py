'''
Trains a convolutional network with 3 conv layers to play Connect Four against a random agent.
'''
import time
import gym

from baselines import deepq

def callback(lcl, glb):
    ''' Indicates training should stop if mean reward is at least 0.5 over 100 episodes. '''
    is_solved = lcl['t'] > 100 and sum(lcl['episode_rewards'][-101:-1]) / 100 >= 0.5
    return is_solved

def main():
    '''
    Makes the Connect Four environment, builds a convolutional network with 3 conv layers
    and one fully-connected layer,
    trains the model, and saves the result.
    '''
    env_name = "Connect4ConvAug-v0"
    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.cnn_to_mlp(
        convs=[(32, 4, 1), (64, 4, 1), (64, 3, 1)],
        hiddens=[256],
        dueling=True,
    )
    act = deepq.learn(
        env,
        q_func=model,
        lr=1e-2,
        max_timesteps=30000,
        buffer_size=10000,
        exploration_fraction=0.3,
        exploration_final_eps=0.02,
        print_freq=10,
        param_noise=False,
        gamma=0.99,
        callback=callback
    )
    model_name = "c4_deepq_conv_model.pkl"
    print("Saving model to: " + model_name)
    act.save(model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == '__main__':
    main()
