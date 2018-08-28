import sys
from get_payoff_from_game_pas import get_json_data
from generate_new_cols_pas import get_new_payoff_file_name, print_json
from utility import get_game_file_name

def get_defender_role(game_data):
    '''
    Get the object from game data -> "role" with "name" of "defender"
    '''
    roles_list = game_data["roles"]
    for role in roles_list:
        if role["name"] == "defender":
            return role
    raise ValueError("Defender role not found")

def add_defender_strategy(game_data, new_strategy):
    '''
    Add the new strategy name go game data -> "role" of "name" "defender" -> "strategies"
    '''
    try:
        def_role = get_defender_role(game_data)
    except ValueError:
        sys.exit(1)

    def_strategies = def_role["strategies"]
    def_strategies.append(new_strategy)


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
    profile["observations_count"] = 1

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

def augment_game_data(game_data, new_data):
    def_strat = list(game_data.keys())[0]

    next_profile_id = get_max_profile_id(game_data) + 1
    next_symmetry_group_id = get_max_symmetry_group_id(game_data) + 1
    def_strat_name = get_new_def_strat(new_data)
    add_defender_strategy(game_data, def_strat_name)
    for result_to_add in get_results_to_add(new_data, def_strat_name):
        (def_strat, att_strat, def_payoff, att_payoff) = result_to_add
        add_profile(game_data, def_strat, att_strat, def_payoff, att_payoff, \
            next_profile_id, next_symmetry_group_id)

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

def get_new_def_strat(new_data):
    keys = new_data.keys()
    for key in keys:
        if key != "num_sims":
            return key
    raise ValueError("Missing new defender key: " + str(new_data))

def get_results_to_add(new_data, def_strat_name):
    '''
    For each key in new_data, one will be the new defender strategy, the other the new
    attacker strategy.
    For each entry in that key's list, get the key, which is the opponent's strategy.
    The value will be a list of two items, the first being the mean defender payoff,
    the second the mean attacker payoff.
    Add the item to the result list, as a tuple
    (def_name, att_name, def_payoff, att_payoff).
    '''
    result = []

    def_results = new_data[def_strat_name]
    for att_strat, payoffs in def_results.items():
        cur_def = def_strat_name
        cur_att = att_strat
        def_payoff = payoffs[0]
        att_payoff = payoffs[1]
        result.append((cur_def, cur_att, def_payoff, att_payoff))
    return result

def add_data(run_name, test_round, cur_step):
    payoff_file_name = get_new_payoff_file_name(run_name, test_round, cur_step)
    new_payoffs = get_json_data(payoff_file_name)

    game_file_name = get_game_file_name(run_name, test_round, cur_step)
    game_json = get_json_data(game_file_name)

    augment_game_data(game_json, new_payoffs)
    result_file_name = get_game_file_name(run_name, test_round, cur_step + 1)
    print_json(result_file_name, game_json)

# example: python3 add_new_data_pas.py dg1 0 1
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: run_name, test_round, cur_step")
    RUN_NAME = sys.argv[1]
    TEST_ROUND = int(sys.argv[2])
    CUR_STEP = int(sys.argv[3])
    add_data(RUN_NAME, TEST_ROUND, CUR_STEP)
