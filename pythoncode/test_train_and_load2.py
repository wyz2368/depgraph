import gym
import time
from baselines import deepq

def main():
    env = gym.make("VsNetTest-v0")
    model = deepq.models.mlp([256, 256])
    model_name = "test_dq_mlp_rand_epoch2.pkl"
    act = deepq.learn_and_save(
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
        save_name=model_name
    )
    print("Saving model to: " + model_name)
    time.sleep(3)
    act_loaded = deepq.load(model_name)
    print("Loaded model")

if __name__ == '__main__':
    main()
