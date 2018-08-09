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

def get_defender_strats(game_data):
    '''
    Returns the list of strategies for the defender role.
    '''
    for role in game_data["roles"]:
        if role["name"] == "defender":
            return role["strategies"]
    raise ValueError("Defender not found: " + str(game_data["roles"]))

def get_attacker_strats(game_data):
    '''
    Returns the list of strategies for the attacker role.
    '''
    for role in game_data["roles"]:
        if role["name"] == "attacker":
            return role["strategies"]
    raise ValueError("Attacker not found: " + str(game_data["roles"]))

def are_payoffs_found(game_data, defender, attacker):
    '''
    Returns True iff there is a payoff entry for the defender-attacker pair.
    '''
    for profile in game_data["profiles"]:
        matches_def = False
        matches_att = False
        for group in profile["symmetry_groups"]:
            if group["strategy"] == defender and group["role"] == "defender":
                matches_def = True
            if group["strategy"] == attacker and group["role"] == "attacker":
                matches_att = True
        if matches_def and matches_att:
            return True
    return False

def check_for_missing_payoffs(game_data):
    '''
    Check for combinations of attacker strategy and defender strategy from the role strategy
    lists, that do not have corresponding payoffs.
    '''
    defender_strats = get_defender_strats(game_data)
    attacker_strats = get_attacker_strats(game_data)
    found_missing = False
    for defender in defender_strats:
        for attacker in attacker_strats:
            if not are_payoffs_found(game_data, defender, attacker):
                print("Missing payoffs: " + defender + " " + attacker)
                found_missing = True
    if not found_missing:
        print("No missing payoffs.")
    return not found_missing

def check_for_extra_payoffs(game_data):
    '''
    Check for payoff entries where either the defender strategy or attacker strategy
    is not present in the role strategy list.
    '''
    defender_strats = get_defender_strats(game_data)
    attacker_strats = get_attacker_strats(game_data)

    missing_defs = set()
    missing_atts = set()
    for profile in game_data["profiles"]:
        defender = None
        attacker = None
        for group in profile["symmetry_groups"]:
            if group["role"] == "defender":
                defender = group["strategy"]
            if group["role"] == "attacker":
                attacker = group["strategy"]
        if defender is None or attacker is None:
            raise ValueError("Invalid profile: " + str(profile))
        if defender not in defender_strats or attacker not in attacker_strats:
            print("Extra payoffs: " + defender + " " + attacker)
        if defender not in defender_strats:
            missing_defs.add(defender)
        if attacker not in attacker_strats:
            missing_atts.add(attacker)
    if missing_defs:
        print("Missing defenders: " + str(missing_defs))
    if missing_atts:
        print("Missing attackers: " + str(missing_atts))
    if not missing_atts and not missing_defs:
        print("No extra payoffs found.")
    return (not missing_atts) and (not missing_defs)

def check_for_duplicates(game_data):
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
        print("No duplicate payoffs found.")
    return not any_problems

# python check_game_data.py game_3014_5.json
def check_game(game_file):
    '''
    Load the pre-existing game payoff data,
    check for duplicate profiles,
    and print out their names and all payoff tuples
    '''
    game_data = get_json_data(game_file)
    no_dups = check_for_duplicates(game_data)
    no_missing = check_for_missing_payoffs(game_data)
    no_extra = check_for_extra_payoffs(game_data)
    return no_dups and no_missing and no_extra

if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError( \
            "Need 3 args: game_file")
    GAME_FILE = sys.argv[1]
    check_game(GAME_FILE)
