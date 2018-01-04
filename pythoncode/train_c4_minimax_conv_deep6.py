'''
Trains a convolutional net with 6 conv layers to play Connect Four against a minimax agent.
'''
import time
import gym

from baselines import deepq

def callback(lcl, glb):
    ''' Indicates training should stop if mean reward is at least 0.5 over 200 episodes. '''
    is_solved = lcl['t'] > 200 and sum(lcl['episode_rewards'][-201:-1]) / 200 >= 0.5
    return is_solved

def main():
    '''
    Makes the Connect Four environment, builds a convolutional neural net with 6 conv layers
    and one fully-connected layer,
    trains the model, and saves the result.
    '''
    # env_name = "Connect4MaxConvAugD2-v0"
    env_name = "Connect4MaxConvAugD3-v0"
    # env_name = "Connect4MaxConvAugD4-v0"
    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.cnn_to_mlp(
        convs=[(64, 5, 1), (64, 5, 1), (64, 5, 1), (64, 3, 1), (64, 3, 1), (64, 3, 1)],
        hiddens=[128],
        dueling=True,
    )
    act = deepq.learn(
        env,
        q_func=model,
        lr=5e-4,
        max_timesteps=180000,
        buffer_size=10000,
        exploration_fraction=0.3,
        exploration_final_eps=0.01,
        print_freq=10,
        param_noise=False,
        gamma=0.99,
        callback=callback
    )
    print("Saving model to c4_deepq_model_deep6.pkl")
    act.save("c4_deepq_model_deep6.pkl")
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == '__main__':
    main()