'''
Game of Connect Four
'''

from py4j.java_gateway import JavaGateway
import numpy as np
import gym
from gym import spaces

WIDTH = 6
HEIGHT = 7
JAVA_BOARD = None
GATEWAY = None

class C4JavaEnv(gym.Env):
    """
    Connect Four environment. Play against a fixed opponent.
    """
    metadata = {"render.modes": ["human"]}

    def __init__(self):
        # One action for each board position or resign
        # action space is {0, . . ., WIDTH}, indicating column or resign
        self.action_space = spaces.Discrete(WIDTH)

        observation = self.reset()
        self.observation_space = \
            spaces.Box(np.zeros(observation.shape), np.ones(observation.shape))

        # https://www.py4j.org/getting_started.html
        global GATEWAY
        GATEWAY = JavaGateway()
        global JAVA_BOARD
        JAVA_BOARD = GATEWAY.entry_point.getBoard()

    def _reset(self):
        return JAVA_BOARD.reset()

    def _step(self, action):
        # https://www.py4j.org/advanced_topics.html#collections-conversion
        action_for_java = GATEWAY.jvm.java.util.ArrayList()
        for item in action:
            action_for_java.add(item)
        return C4JavaEnv.step_result_from_flat_list(JAVA_BOARD.step(action_for_java))

    @staticmethod
    def step_result_from_flat_list(a_list):
        '''
        Convert a flat list input, a_list, to the observation, reward,
        is_done, and state dictionary.
        a_list will be a list of floats, of length (WIDTH * HEIGHT * 2 + 2).

        The first (WIDTH * HEIGHT * 2) elements of a_list represent the board state.

        The next element represents the reward, in {-1.0, 0.0, 1.0}.

        The last element represents whether the game is done, in {0.0, 1.0}.
        '''
        board_size = WIDTH * HEIGHT * 2
        obs = a_list[:board_size]
        reward = a_list[board_size]

        tolerance = 0.01
        is_done = abs(a_list[board_size + 1] - 1) < tolerance

        state_dict = {'state': obs[:]}
        return obs, reward, is_done, state_dict

    def _render(self, mode='human', close=False):
        if close:
            return
        print(JAVA_BOARD.render())
