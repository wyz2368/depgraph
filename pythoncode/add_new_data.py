
import sys
import json

def get_json_data(json_file):
    with open(json_file) as data_file:    
        result = json.load(data_file)
        return result

def print_json(file_name, json_obj):
    '''
    Prints the given Json object to the given file name.
    '''
    with open(file_name, 'w') as my_file:
        json.dump(json_obj, my_file)

def get_defender_strategy(new_data):
    for key in new_data.keys():
        if "_att_" not in key:
            return key
    raise ValueError("Defender strategy not found")

def get_defender_role(game_data):
    roles_list = game_data["roles"]
    for role in roles_list:
        if role["name"] == "defender"
            return role
    raise ValueError("Defender role not found")

def get_attacker_strategy(new_data):
    for key in new_data.keys():
        if "_att_" in key:
            return key
    raise ValueError("Attacker strategy not found")

def get_attacker_role(game_data):
    roles_list = game_data["roles"]
    for role in roles_list:
        if role["name"] == "attacker"
            return role
    raise ValueError("Attacker role not found")

def add_defender_strategy(game_data, new_strategy):
    def_role = get_defender_role(game_data)
    def_strategies = def_role["strategies"]
    def_strategies.append(new_strategy)

def add_attacker_strategy(game_data, new_strategy):
    att_role = get_attacker_role(game_data)
    att_strategies = att_role["strategies"]
    att_strategies.append(new_strategy)

'''
-- add the new defender strategy to "roles" -> "defender" -> "strategies"
-- add the new attacker strategy to "roles" -> "attacker" -> "strategies"
-- for each (defender, attacker) payoff result:
    -- "profiles" -> new entry:
        -- "id": 1 more than highest previous "profiles" id
        -- "observations_count": same as others (e.g., 10)
        -- "symmetry_groups":
            -- "role": "attacker" or "defender"
            -- "strategy": the attacker or defender strategy string in ""
            -- "payoff_sd": doesn't matter, can use 1.0 for all
            -- "count": 1
            -- "id": 1 more than highest previous "symmetry_groups" id
            -- "payoff": whatever the attacker or defender payoff was in that pairing
'''
def augment_game_data(game_data, new_data):
    def_strat_name = get_defender_strategy(new_data)
    add_defender_strategy(def_strat_name)

    att_strat_name = get_attacker_strategy(new_data)
    add_attacker_strategy(att_strat_name)

    pass

def main(game_file, new_payoffs_file, result_file):
    game_data = get_json_data(game_file)
    new_data = get_json_data(new_payoffs_file)
    augment_game_data(game_data, new_data)
    print_json(result_file, game_data)

if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise ValueError( \
            "Need 3 args: game_file, new_payoffs_file, result_file")
    GAME_FILE = sys.argv[1]
    NEW_PAYOFFS_FILE = sys.argv[2]
    RESULT_FILE = sys.argv[3]
    main(GAME_FILE, NEW_PAYOFFS_FILE, RESULT_FILE)
