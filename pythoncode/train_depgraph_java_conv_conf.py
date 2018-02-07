'''
Trains a multilayer perceptron to play Connect Four against a minimax agent.
'''
import time
import gym

from baselines import deepq

def main():
    '''
    Makes the Connect Four environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    env_name = "DepgraphJavaConv-v0"
    print("Environment: " + env_name)
    HIST_LENGTH = 3
    INPUT_DEPTH = 2 + 2 * HIST_LENGTH

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.cnn_to_mlp(
        convs=[(32, (INPUT_DEPTH, 1), (INPUT_DEPTH, 1)), (32, 1, 1)],
        hiddens=[128],
        dueling=True,
    )
    act = deepq.learn(
        env,
        q_func=model,
        lr=3e-6,
        max_timesteps=600000,
        buffer_size=30000,
        exploration_fraction=0.5,
        exploration_final_eps=0.03,
        print_freq=150,
        param_noise=False,
        gamma=0.99
    )
    model_name = "depgraph_java_deepq_conv_model_conf2_d3_3.pkl"
    print("Saving model to: " + model_name)
    act.save(model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == '__main__':
    main()
