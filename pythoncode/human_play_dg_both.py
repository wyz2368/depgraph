import numpy as np
import gym

def main():
    env_name = "DepgraphJavaEnvBoth-v0"
    env = gym.make(env_name)
    print("Environment: " + env_name)

    obs, done is_def_turn = env.reset(), False, False
    while not done:
        act_index = get_action(obs, is_def_turn)
        obs, done, _, is_def_turn = env.step(np.int32(act_index))
    def_reward = env.get_defender_reward()
    att_reward = env.get_attacker_reward()
    print("Defender reward: " + str(def_reward))
    print("Attacker reward: " + str(att_reward))

def get_action(obs, is_def_turn)
    prompt_string = "Enter defender action index: "
    if not is_def_turn
        prompt_string = "Enter attacker action index: "
    print(obs)
    action = input(prompt_string)
    return 0

if __name__ == '__main__':
    main()