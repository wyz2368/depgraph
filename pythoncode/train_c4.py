import gym

from baselines import deepq

def callback(lcl, glb):
    # stop training if reward exceeds 0.5
    is_solved = lcl['t'] > 100 and sum(lcl['episode_rewards'][-101:-1]) / 100 >= 0.5
    return is_solved

def main():
    env = gym.make("Connect4-v0")
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

if __name__ == '__main__':
    main()
