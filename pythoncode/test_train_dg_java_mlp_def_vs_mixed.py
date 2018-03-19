'''
Trains a multilayer perceptron to play the depgraph game against
an attacker that can mix over heuristic and network strategies.
'''
import time
import gym
from baselines import deepq

def main():
    '''
    Makes the depgraph environment, builds a multilayer perceptron model,
    trains the model, and saves the result.
    '''
    env_name = "DepgraphJavaEnvVsMixedAtt-v0"
    print("Environment: " + env_name)

    env = gym.make(env_name)
    model = deepq.models.mlp([256, 256])
    model_name = "test_dq_mlp_rand_epoch2.pkl"
    act = deepq.learn(
        env,
        q_func=model,
        lr=5e-5,
        max_timesteps=50,
        buffer_size=50,
        exploration_fraction=0.5,
        exploration_final_eps=0.03,
        checkpoint_freq=25,
        print_freq=100,
        param_noise=False,
        gamma=0.99,
        ep_mean_length=25,
        # save_name=model_name
    )
    print("Saving model to: " + model_name)
   
#    act.save(model_name)
#    print("Saved model")

    sleep_sec = 1
    time.sleep(sleep_sec)
    act_loaded = deepq.load_with_scope(model_name, "deepq_train")
    print("Loaded model")

if __name__ == '__main__':
    main()
