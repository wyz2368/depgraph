'''
Trains a multilayer perceptron to play the depgraph game as attacker.
'''
import time
import gym

from baselines import deepq

def main():
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    trains the model to play as attacker, and saves the result.
    '''
    env_name = "DepgraphJavaAtt-v0"
    print("Environment: " + env_name)

    start = time.time()
    env = gym.make(env_name)
    model = deepq.models.mlp([256, 256])
    act = deepq.learn(
        env,
        q_func=model,
        lr=5e-5,
        max_timesteps=1000000,
        buffer_size=30000,
        exploration_fraction=0.5,
        exploration_final_eps=0.03,
        checkpoint_freq=30000,
        print_freq=250,
        param_noise=False,
        gamma=0.99,
        ep_mean_length=250
    )
    model_name = "dg_dqmlp_rand30NoAnd_B_att_fixed.pkl"
    print("Saving model to: " + model_name)
    act.save(model_name)
    end = time.time()
    elapsed = end - start
    minutes = elapsed // 60
    print("Minutes taken: " + str(minutes))
    print("Opponent was: " + env_name)

if __name__ == '__main__':
    main()
