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

def get_defender_strategy(new_data):
    '''
    Of the keys in new_data, find the one WITHOUT the substring "_att_", indicating it's the
    defender strategy.
    '''
    for key in new_data.keys():
        if "_att_" not in key:
            return key
    raise ValueError("Defender strategy not found")

def get_defender_role(game_data):
    '''
    Get the object from game data -> "role" with "name" of "defender"
    '''
    roles_list = game_data["roles"]
    for role in roles_list:
        if role["name"] == "defender":
            return role
    raise ValueError("Defender role not found")

def get_attacker_strategy(new_data):
    '''
    Of the keys in new_data, find the one with the substring "_att_", indicating it's the
    attacker strategy.
    '''
    for key in new_data.keys():
        if "_att_" in key:
            return key
    raise ValueError("Attacker strategy not found")

def get_attacker_role(game_data):
    '''
    Get the object from game data -> "role" with "name" of "attacker"
    '''
    roles_list = game_data["roles"]
    for role in roles_list:
        if role["name"] == "attacker":
            return role
    raise ValueError("Attacker role not found")

def add_defender_strategy(game_data, new_strategy):
    '''
    Add the new strategy name go game data -> "role" of "name" "defender" -> "strategies"
    '''
    def_role = get_defender_role(game_data)
    def_strategies = def_role["strategies"]
    def_strategies.append(new_strategy)

def add_attacker_strategy(game_data, new_strategy):
    '''
    Add the new strategy name go game data -> "role" of "name" "attacker" -> "strategies"
    '''
    att_role = get_attacker_role(game_data)
    att_strategies = att_role["strategies"]
    att_strategies.append(new_strategy)

def add_profile(game_data, def_strat, att_strat, def_payoff, att_payoff, \
    next_profile_id, next_symmetry_group_id):
    '''
    Add the profile given by (def_strat, att_strat, def_payoff, att_payoff) to the list
    of profiles in game_data.
    Assume the new profile has 10 observations and standard deviation of payoffs of 1.0,
    with the count of agents being 1 per symmetry group.
    '''
    profile = {}
    profile["id"] = next_profile_id
    profile["observations_count"] = 10

    symmetry_groups = []

    attacker_group = {}
    attacker_group["role"] = "attacker"
    attacker_group["strategy"] = att_strat
    attacker_group["payoff_sd"] = 1.0
    attacker_group["count"] = 1
    attacker_group["id"] = next_symmetry_group_id
    attacker_group["payoff"] = att_payoff

    defender_group = {}
    defender_group["role"] = "defender"
    defender_group["strategy"] = def_strat
    defender_group["payoff_sd"] = 1.0
    defender_group["count"] = 1
    defender_group["id"] = next_symmetry_group_id + 1
    defender_group["payoff"] = def_payoff

    symmetry_groups.append(attacker_group)
    symmetry_groups.append(defender_group)
    profile["symmetry_groups"] = symmetry_groups
    game_data["profiles"].append(profile)

def get_max_profile_id(game_data):
    '''
    In the game data -> "profiles", get the highest "id".
    '''
    profiles = game_data["profiles"]
    ids = [x["id"] for x in profiles]
    return max(ids)

def get_max_symmetry_group_id(game_data):
    '''
    In the game data -> "profiles" -> "symmetry_groups", get the highest "id" for
    any such object.
    '''
    profiles = game_data["profiles"]
    max_id = 0
    for profile in profiles:
        for item in profile["symmetry_groups"]:
            cur_id = item["id"]
            if cur_id > max_id:
                max_id = cur_id
    return max_id

def get_results_to_add(new_data, def_strat_name, att_strat_name):
    '''
    For each key in new_data, one will be the new defender strategy, the other the new
    attacker strategy.
    For each entry in that key's list, get the key, which is the opponent's strategy.
    The value will be a list of two items, the first being the mean defender payoff,
    the second the mean attacker payoff.
    Add the item to the result list, as a tuple (def_name, att_name, def_payoff, att_payoff).
    '''
    result = []

    def_results = new_data[def_strat_name]
    for att_strat, payoffs in def_results.iteritems():
        cur_def = def_strat_name
        cur_att = att_strat
        def_payoff = payoffs[0]
        att_payoff = payoffs[1]
        result.append((cur_def, cur_att, def_payoff, att_payoff))

    att_results = new_data[att_strat_name]
    for def_strat, payoffs in att_results.iteritems():
        cur_def = def_strat
        cur_att = att_strat_name
        def_payoff = payoffs[0]
        att_payoff = payoffs[1]
        result.append((cur_def, cur_att, def_payoff, att_payoff))
    return result

def augment_game_data(game_data, new_data):
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
    def_strat_name = get_defender_strategy(new_data)
    add_defender_strategy(game_data, def_strat_name)

    att_strat_name = get_attacker_strategy(new_data)
    add_attacker_strategy(game_data, att_strat_name)

    next_profile_id = get_max_profile_id(game_data) + 1
    next_symmetry_group_id = get_max_symmetry_group_id(game_data) + 1
    for result_to_add in get_results_to_add(new_data, def_strat_name, att_strat_name):
        (def_strat, att_strat, def_payoff, att_payoff) = result_to_add
        if def_strat == "NOOP:" or att_strat == "NOOP:":
            pass
        add_profile(game_data, def_strat, att_strat, def_payoff, att_payoff, \
                    next_profile_id, next_symmetry_group_id)
        next_profile_id += 1
        next_symmetry_group_id += 2

# python add_new_data.py game_3014.json out_newPayoffData_1.json game_3014_1.json
def main(game_file, new_payoffs_file, result_file):
    '''
    Load the pre-existing game payoff data, then load the new payoff entries and
    strategy names.
    Augment the old game data object with the new strategies and payoffs.
    Print the extended object to file as Json.
    '''
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
