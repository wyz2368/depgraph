'''
Add the new strategies and their payoffs to the given game data Json file.
'''
import sys
import json

def get_json_data(json_file):
    '''
    Loads the data from the file as Json into a new object.
    '''
    with open(json_file) as data_file:
        result = json.load(data_file)
        return result

def print_json(file_name, json_obj):
    '''
    Prints the given Json object to the given file name.
    '''
    with open(file_name, 'w') as my_file:
        json.dump(json_obj, my_file)

def check_game_data(game_data):
    '''
    -- check for duplicate profiles, and print any out with their values
    '''
    def_to_att_to_count = {}

    for profile in game_data["profiles"]:
        defender = None
        attacker = None
        for group in profile["symmetry_groups"]:
            if group["role"] == "defender":
                defender = group["strategy"]
            if group["role"] == "attacker":
                attacker = group["strategy"]
        if not defender or not attacker:
            raise ValueError("invalid profile: " + str(profile))
        if defender not in def_to_att_to_count:
            def_to_att_to_count[defender] = {}
        if attacker in def_to_att_to_count[defender]:
            def_to_att_to_count[defender][attacker] += 1
        else:
            def_to_att_to_count[defender][attacker] = 1

    any_problems = False
    for defender in def_to_att_to_count:
        for attacker in def_to_att_to_count[defender]:
            if def_to_att_to_count[defender][attacker] > 1:
                any_problems = True
                print(defender + "\t" + attacker + "\t" + \
                    str(def_to_att_to_count[defender][attacker]))
                for profile in game_data["profiles"]:
                    cur_defender = None
                    def_payoff = 0
                    cur_attacker = None
                    att_payoff = 0
                    for group in profile["symmetry_groups"]:
                        if group["role"] == "defender":
                            cur_defender = group["strategy"]
                            def_payoff = group["payoff"]
                        if group["role"] == "attacker":
                            cur_attacker = group["strategy"]
                            att_payoff = group["payoff"]
                    if cur_defender == defender and cur_attacker == attacker:
                        print(str(def_payoff) + "\t" + str(att_payoff))
                print("\n")
    if not any_problems:
        print("Checks out OK.")

# python check_game_data.py game_3014_5.json
def main(game_file):
    '''
    Load the pre-existing game payoff data,
    check for duplicate profiles,
    and print out their names and all payoff tuples
    '''
    game_data = get_json_data(game_file)
    check_game_data(game_data)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError( \
            "Need 3 args: game_file")
    GAME_FILE = sys.argv[1]
    main(GAME_FILE)
