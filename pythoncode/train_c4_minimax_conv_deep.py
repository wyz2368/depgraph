import gym

from baselines import deepq

def callback(lcl, glb):
    # stop training if reward exceeds 0.5
    is_solved = lcl['t'] > 100 and sum(lcl['episode_rewards'][-101:-1]) / 100 >= 0.5
    return is_solved

def main():
    env_name = "Connect4MaxConvAug-v0"
    # env_name = "Connect4MaxConvAugD2-v0"
    print("Environment: " + env_name)
    model = deepq.models.cnn_to_mlp(
        convs=[(64, 5, 1), (64, 5, 1), (64, 3, 1), (64, 3, 1), (64, 3, 1)],
        hiddens=[128],
        dueling=True,
    )
    act = deepq.learn(
        env,
        q_func=model,
        lr=1e-3,
        max_timesteps=60000,
        buffer_size=10000,
        exploration_fraction=0.3,
        exploration_final_eps=0.02,
        print_freq=10,
        param_noise=False,
        gamma=0.99,
        callback=callback
    )
    print("Saving model to c4_deepq_model_deep.pkl")
    act.save("c4_deepq_model_deep.pkl")

if __name__ == '__main__':
    main()
