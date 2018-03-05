'''
Lets human play dependency graph game, from both attacker and defender sides at once.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''

import numpy as np
import gym

def play_game():
    '''
        Plays a round of the dependency graph game, with human taking both roles.
    '''
    env_name = "DepgraphJavaEnvBoth-v0"
    env = gym.make(env_name)
    print("Environment: " + env_name)
    def_action_count = env.get_defender_action_count()
    att_action_count = env.get_attacker_action_count()

    obs, done, is_def_turn = env.reset(), False, False
    while not done:
        act_index = get_action(obs, is_def_turn, def_action_count, att_action_count)
        obs, done, _, is_def_turn = env.step(np.int32(act_index))
    def_reward = env.get_defender_reward()
    att_reward = env.get_attacker_reward()
    print("\nDefender reward: " + str(def_reward))
    print("Attacker reward: " + str(att_reward))

def get_action(obs, is_def_turn, def_action_count, att_action_count):
    '''
        Asks human for input, on the attacker's or defender's next item to add to the set
        to act on, or whether to pass.
        The input must be an integer in {0, . . ., maxIndex}.
    '''
    max_input = def_action_count - 1
    prompt_string = "Enter defender action index in {0, . . ., " + str(max_input) + "}: "
    if not is_def_turn:
        max_input = att_action_count - 1
        prompt_string = "Enter attacker action index in {0, . . ., " + str(max_input) + "}: "
    print(obs)
    action = input(prompt_string).trim()
    if not action.isdigit():
        print("Invalid input: must be non-negative integer.")
        return get_action(obs, is_def_turn, def_action_count, att_action_count)
    action = int(float(action))
    if action < 0 or action > max_input:
        print("Invalid input: must be in {0, . . ., " + str(max_input + "}."))
        return get_action(obs, is_def_turn, def_action_count, att_action_count)
    return action

if __name__ == '__main__':
    play_game()
