'''
Dependency graph game, from attacker and defender's view.

Requirements:
    Py4J        https://www.py4j.org/download.html
    OpenAI Gym  https://github.com/openai/gym#installation
'''

from py4j.java_gateway import JavaGateway
import numpy as np
import gym
from gym import spaces

NODE_COUNT = 30
AND_NODE_COUNT = 5
EDGE_TO_OR_NODE_COUNT = 100

DEF_OBS_LENGTH = 3
ATT_OBS_LENGTH = 1

DEF_ACTION_COUNT = NODE_COUNT + 1
ATT_ACTION_COUNT = AND_NODE_COUNT + EDGE_TO_OR_NODE_COUNT + 1

DEF_INPUT_DEPTH = 2 + DEF_OBS_LENGTH

DEF_OBS_SIZE = NODE_COUNT * DEF_INPUT_DEPTH
ATT_OBS_SIZE = (AND_NODE_COUNT + EDGE_TO_OR_NODE_COUNT) * 2 + NODE_COUNT * ATT_OBS_LENGTH + 1

JAVA_GAME = None
GATEWAY = None
IS_DEF_TURN = None

class DepgraphJavaEnvBoth(gym.Env):
    """
    Depgraph game environment.
    """
    metadata = {"render.modes": ["human"]}

    def __init__(self):
        # https://www.py4j.org/getting_started.html
        global GATEWAY
        GATEWAY = JavaGateway()
        global JAVA_GAME
        JAVA_GAME = GATEWAY.entry_point.getGame()
        global IS_DEF_TURN
        IS_DEF_TURN = True

        action_count = max(ATT_ACTION_COUNT, DEF_ACTION_COUNT)
        self.action_space = spaces.Discrete(action_count)

        observation = self.reset()
        # convert from JavaMember object to JavaList
        observation = observation[:]
        my_shape = (len(observation),)
        self.observation_space = \
            spaces.Box(np.zeros(my_shape), np.ones(my_shape))

    def _reset(self):
        result_values = JAVA_GAME.reset()
        global IS_DEF_TURN
        IS_DEF_TURN = True
        def_obs = result_values[:DEF_OBS_SIZE]
        def_obs = np.array([x for x in def_obs])
        return def_obs

    def _step(self, action):
        # action is a numpy.int64, need to convert to Python int before using with Py4J
        action_scalar = np.asscalar(action)
        action_id = action_scalar + 1
        both_obs, reward, is_done, state_dict, is_def_turn_local = \
            DepgraphJavaEnvBoth.step_result_from_flat_list(JAVA_GAME.step(action_id))
        global IS_DEF_TURN
        IS_DEF_TURN = is_def_turn_local

        def_obs = both_obs[:DEF_OBS_SIZE]
        att_obs = both_obs[DEF_OBS_SIZE:]

        cur_obs = def_obs
        if not IS_DEF_TURN:
            cur_obs = att_obs
        cur_obs = np.array([x for x in cur_obs])
        return cur_obs, reward, is_done, state_dict

    @staticmethod
    def step_result_from_flat_list(a_list):
        '''
        Convert a flat list input, a_list, to the observation (for defender, then
        attacker), reward, is_done, state dictionary, and is_def_turn_local.

        The first game_size elements of a_list represent the game state, first for the
        defender's view, then the attacker's.

        The next element represents the reward, in R.

        The next element represents whether the game is done, in {0.0, 1.0}.

        The last element represents whether it is the defender's turn, in {0.0, 1.0}.
        '''
        game_size = DEF_OBS_SIZE + ATT_OBS_SIZE

        obs_values = a_list[:game_size]
        # obs_values is a Py4J JavaList -> should convert to Python list
        both_obs = np.array([x for x in obs_values])

        reward = a_list[game_size]

        tolerance = 0.01
        is_done = abs(a_list[game_size + 1] - 1) < tolerance

        state_dict = {'state': both_obs[:]}

        is_def_turn_local = abs(a_list[game_size + 2] - 1) < tolerance
        return both_obs, reward, is_done, state_dict, is_def_turn_local

    def _render(self, mode='human', close=False):
        if close:
            return
        print(JAVA_GAME.render())

    def get_attacker_reward(self):
        '''
        Get the total discounted reward of the attacker in the current game.
        '''
        return JAVA_GAME.getAttackerTotalPayoff()

    def get_defender_reward(self):
        '''
        Get the total discounted reward of the defender in the current game.
        '''
        return JAVA_GAME.getDefenderTotalPayoff()
