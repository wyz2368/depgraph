'''
Trains a multilayer perceptron to play the depgraph game against an agent.
'''
import time
import gym

from baselines import deepq

def main():
    '''
    Makes the depgraph game environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    env_name = "DepgraphJava-v0"
    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.mlp([256])
    act = deepq.learn(
        env,
        q_func=model,
        lr=1e-4,
        max_timesteps=600000,
        buffer_size=30000,
        exploration_fraction=0.5,
        exploration_final_eps=0.05,
        print_freq=150,
        param_noise=False,
        gamma=0.99
    )
    model_name = "depgraph_java_deepq_model2.pkl"
    print("Saving model to: " + model_name)
    act.save(model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == '__main__':
    main()
