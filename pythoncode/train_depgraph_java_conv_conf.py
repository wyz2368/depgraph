'''
Trains a convolutional network to play Connect Four against a mixed strategy agent,
using Deep Q Networks from OpenAI Baselines.
https://github.com/openai/baselines/tree/master/baselines/deepq
'''
import time
import gym

from baselines import deepq

def main():
    '''
    Makes the depgraph environment, builds a convolutional network model,
    trains the model, and saves the result.
    '''
    env_name = "DepgraphJavaConv-v0"
    print("Environment: " + env_name)
    hist_length = 3
    input_depth = 2 + 2 * hist_length

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.cnn_to_mlp(
        convs=[(32, (input_depth, 1), (input_depth, 1)), (32, 1, 1)],
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
