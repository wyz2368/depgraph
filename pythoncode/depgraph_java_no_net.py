'''
Dependency graph game.

Requirements:
    Py4J        https://www.py4j.org/download.html
'''
from py4j.java_gateway import JavaGateway

JAVA_GAME = None
GATEWAY = None

class DepgraphJavaEnvAttNoNet():
    '''
    Runs the Java game environment.
    '''
    def init(self):
        '''
        Set up the connection to the Java game.
        '''
        # https://www.py4j.org/getting_started.html
        global GATEWAY
        GATEWAY = JavaGateway()
        global JAVA_GAME
        JAVA_GAME = GATEWAY.entry_point.getGame()

    def reset_and_run_once(self):
        '''
        Reset the game and run until it ends.
        '''
        JAVA_GAME.resetAndRunOnce()

    def get_attacker_reward(self):
        '''
        Get the total discounted reward of the opponent (attacker) in the current game.
        '''
        return JAVA_GAME.getOpponentTotalPayoff()

    def get_defender_reward(self):
        '''
        Get the total discounted reward of self (defender) in the current game.
        '''
        return JAVA_GAME.getSelfTotalPayoff()

    def close_gateway(self):
        '''
        Close the connection to the Java program.
        '''
        GATEWAY.close()
        GATEWAY.close_callback_server()
        GATEWAY.shutdown()
    