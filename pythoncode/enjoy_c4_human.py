''' Play back the Connect Four network that was learned. '''
import gym

from baselines import deepq

def main():
    ''' Load the network from file, and play games of Connect Four against opponent. '''

    env_name = "Connect4HumanConvAug-v0"
    model_name = "c4_deepq_conv_model.pkl"

    env = gym.make(env_name)
    print("Environment: " + env_name)

    act = deepq.load(model_name)
    print("Model: " + model_name)

    obs, done = env.reset(), False
    while not done:
        obs, rew, done, _ = env.step(act(obs[None])[0])
        print("")
    env.render()

if __name__ == '__main__':
    main()
