import sys
import os.path
import copy
from get_both_payoffs_from_game import get_json_data
from generate_new_cols import print_json
from add_new_data import add_defender_strategy, add_attacker_strategy, get_max_profile_id, \
    get_max_symmetry_group_id, get_original_num_sims, add_profile
from union_games import get_defender_networks, get_attacker_networks, is_game_data_valid

def get_attacker(profile):
    for symmetry_group in profile["symmetry_groups"]:
        if symmetry_group["role"] == "attacker":
            return symmetry_group["strategy"]
    raise ValueError("No attacker: " + str(profile))

def get_defender(profile):
    for symmetry_group in profile["symmetry_groups"]:
        if symmetry_group["role"] == "defender":
            return symmetry_group["strategy"]
    raise ValueError("No defender: " + str(profile))

def get_att_payoff(profile):
    for symmetry_group in profile["symmetry_groups"]:
        if symmetry_group["role"] == "attacker":
            return symmetry_group["payoff"]
    raise ValueError("No attacker: " + str(profile))

def get_def_payoff(profile):
    for symmetry_group in profile["symmetry_groups"]:
        if symmetry_group["role"] == "defender":
            return symmetry_group["payoff"]
    raise ValueError("No defender: " + str(profile))

def has_profile(game_data, attacker, defender):
    for profile in game_data["profiles"]:
        if get_attacker(profile) == attacker and get_defender(profile) == defender:
            return True
    return False

def remove_duplicates_keep_order(my_list):
    result = []
    for item in my_list:
        if item not in result:
            result.append(item)
    return result

'''
union file format:
"profiles": [{"observations_count": 10, "id": 1234, "symmetry_groups": []}]
"roles" : [{"name": "attacker", "count": 1, "strategies": []}, . . .]
"network_source": {"game_3014_22_d30f1.json": [], "game_3014_20_d30cd1.json": []}
'''
def union_game_data_multiple(union_data_list):
    result = copy.deepcopy(union_data_list[0])

    def_nets_list = [get_defender_networks(union_data) for union_data in union_data_list]
    old_def_nets = def_nets_list[0]
    def_nets_list = [item for sublist in def_nets_list for item in sublist]
    def_nets_list = [item for item in def_nets_list if item not in old_def_nets]
    def_nets_list = remove_duplicates_keep_order(def_nets_list)

    att_nets_list = [get_attacker_networks(union_data) for union_data in union_data_list]
    old_att_nets = att_nets_list[0]
    att_nets_list = [item for sublist in att_nets_list for item in sublist]
    att_nets_list = [item for item in att_nets_list if item not in old_att_nets]
    att_nets_list = remove_duplicates_keep_order(att_nets_list)

    for def_net in def_nets_list:
        add_defender_strategy(result, def_net)
    for att_net in att_nets_list:
        add_attacker_strategy(result, att_net)

    original_num_sims = get_original_num_sims(result)
    next_profile_id = get_max_profile_id(result) + 1
    next_symmetry_group_id = get_max_symmetry_group_id(result) + 1
    for i in range(1, len(union_data_list)):
        union_data = union_data_list[i]
        profiles = union_data["profiles"]
        for profile in profiles:
            defender = get_defender(profile)
            attacker = get_attacker(profile)
            if not has_profile(result, attacker, defender):
                def_payoff = get_def_payoff(profile)
                att_payoff = get_att_payoff(profile)
                add_profile(result, defender, attacker, def_payoff, att_payoff, \
                    next_profile_id, next_symmetry_group_id, \
                    original_num_sims, original_num_sims)
                next_profile_id += 1
                next_symmetry_group_id += 2

    network_source = "network_source"
    for i in range(1, len(union_data_list)):
        for game_file_name in union_data_list[i][network_source]:
            if game_file_name not in result[network_source]:
                result[network_source][game_file_name] = \
                    list(union_data_list[i][network_source][game_file_name])
    return result

def main(result_game_file, union_game_file_list):
    print("union_game_file_list: " + str(union_game_file_list))
    print("result_game_file: " + result_game_file)

    if len(union_game_file_list) != len(set(union_game_file_list)):
        raise ValueError("Input list has duplicates: " + str(union_game_file_list))

    union_data_list = [get_json_data(x) for x in union_game_file_list]
    for union_data in union_data_list:
        if not is_game_data_valid(union_data):
            raise ValueError("Invalid game data")

    if os.path.isfile(result_game_file):
        raise ValueError("File already exists: " + result_game_file)

    combined_game_data = union_game_data_multiple(union_data_list)
    print_json(result_game_file, combined_game_data)

    new_game_dict = get_json_data(result_game_file)
    if not is_game_data_valid(new_game_dict):
        raise ValueError("Invalid result data")

'''
example: python3 union_games_multiple.py game_comb_d30cd1_d30n1_d30f1.json \
   game_comb_d30cd1_d30f1_100.json game_comb_d30cd1_d30n1_200.json \
   game_comb_d30n1_d30f1_100.json
'''
if __name__ == '__main__':
    if len(sys.argv) < 4:
        raise ValueError("Need 3+ args: " + \
                         "result_game_file, " + \
                         "union_game_file_a, union_game_file_b, . . .")
    RESULT_GAME_FILE = sys.argv[1]
    UNION_GAME_FILE_LIST = []
    for cur_index in range(2, len(sys.argv)):
        UNION_GAME_FILE_LIST.append(sys.argv[cur_index])
    main(RESULT_GAME_FILE, UNION_GAME_FILE_LIST)
