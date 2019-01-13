import sys
from check_game_data import get_json_data, get_defender_strats, get_attacker_strats

def count_strats(game_file):
    game_data = get_json_data(game_file)
    def_strats = get_defender_strats(game_data)
    att_strats = get_attacker_strats(game_data)
    print("Def strat count: " + str(len(def_strats)))
    print("Att strat count: " + str(len(att_strats)))

if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError( \
            "Need 1 arg: game_file")
    GAME_FILE = sys.argv[1]
    count_strats(GAME_FILE)
