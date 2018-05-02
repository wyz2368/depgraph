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

def get_original_num_sims(game_data):
    '''
    Return the num_sims that were used per observation originally in the game, on
    EGTA Online.
    '''
    config = game_data["configuration"]
    for pair in config:
        if pair[0] == "numSim":
            return int(pair[1])
    raise ValueError("numSim not given in game file configuration")

def get_profile_entry(game_data, def_strat, att_strat):
    '''
    From the game_data object, return the entry whose strategies match
    def_strat and att_strat.
    '''
    for profile in game_data["profiles"]:
        matches_def = False
        matches_att = False
        for item in profile["symmetry_groups"]:
            if item["strategy"] == def_strat:
                matches_def = True
            if item["strategy"] == att_strat:
                matches_att = True
        if matches_att and matches_def:
            return profile
    raise ValueError("profile not found: " + def_strat + ", " + att_strat)

def get_def_payoff(profile):
    '''
    Given a profile entry from a game data file, return the payoff of the defender.
    '''
    for item in profile["symmetry_groups"]:
        if item["role"] == "defender":
            return item["payoff"]
    raise ValueError("def payoff not found: " + str(profile))

def set_def_payoff(game_data, def_strat, att_strat, new_payoff):
    '''
    Given a profile entry from a game data file, set the defender payoff to the given value.
    '''
    for profile in game_data["profiles"]:
        matches_def = False
        matches_att = False
        for item in profile["symmetry_groups"]:
            if item["strategy"] == def_strat:
                matches_def = True
            if item["strategy"] == att_strat:
                matches_att = True
        if matches_att and matches_def:
            for item in profile["symmetry_groups"]:
                if item["role"] == "defender":
                    item["payoff"] = new_payoff
                    return
    raise ValueError("def payoff not found: " + str(def_strat) + " " + str(att_strat))

def get_att_payoff(profile):
    '''
    Given a profile entry from a game data file, return the payoff of the attacker.
    '''
    for item in profile["symmetry_groups"]:
        if item["role"] == "attacker":
            return item["payoff"]
    raise ValueError("att payoff not found: " + str(profile))

def set_att_payoff(game_data, def_strat, att_strat, new_payoff):
    '''
    Given a profile entry from a game data file, set the attacker payoff to the given value.
    '''
    for profile in game_data["profiles"]:
        matches_def = False
        matches_att = False
        for item in profile["symmetry_groups"]:
            if item["strategy"] == def_strat:
                matches_def = True
            if item["strategy"] == att_strat:
                matches_att = True
        if matches_att and matches_def:
            for item in profile["symmetry_groups"]:
                if item["role"] == "attacker":
                    item["payoff"] = new_payoff
                    return
    raise ValueError("att payoff not found: " + str(def_strat) + " " + str(att_strat))

def set_observations(game_data, def_strat, att_strat, observations):
    '''
    Given a profile entry from a game data file, set the observations_count to the given
    value.
    '''
    for profile in game_data["profiles"]:
        matches_def = False
        matches_att = False
        for item in profile["symmetry_groups"]:
            if item["strategy"] == def_strat:
                matches_def = True
            if item["strategy"] == att_strat:
                matches_att = True
        if matches_att and matches_def:
            profile["observations_count"] = observations
            return
    raise ValueError("profile not found: " + str(def_strat) + " " + str(att_strat))

def augment_game_data(game_data, new_data):
    '''
    Update game_data based on the new payoffs for existing profiles in new_data.
    For each attacker-defender pair in new_data:
    -- compute the equivalent number of observations represented by num_sims.
    -- take the weighted mean of the old and new payoffs for the profile, based on
        the old observation count and num_sims.
    -- update observation_count and payoffs in game_data with the new values, taking
        weighted mean over equivalent observations for payoffs, and sum for equivalent
        observations (scaled with the original num_sims as one observation).
    '''

    new_num_sims = new_data["num_sims"]
    old_num_sims = get_original_num_sims(game_data)
    new_obs_equivalents = new_num_sims * 1.0 / old_num_sims
    if new_num_sims < 1 or old_num_sims < 1:
        raise ValueError("num_sims must be positive: " + str(old_num_sims) + ", " + \
            str(new_num_sims))

    skip_names = ["num_sims", "graph_name", "env_name_att_net", "env_name_def_net", \
        "env_name_both"]
    for defender in new_data:
        if defender in skip_names:
            continue # not a defender entry
        for attacker in new_data[defender]:
            (new_def_payoff, new_att_payoff) = new_data[defender][attacker]
            old_profile = get_profile_entry(game_data, defender, attacker)
            old_observations = old_profile["observations_count"]
            # print(attacker + " " + defender + " " + \
            #     str(new_def_payoff) + " " + str(old_observations))
            old_def_payoff = get_def_payoff(old_profile)
            old_att_payoff = get_att_payoff(old_profile)
            total_def_payoff = old_def_payoff * old_observations + \
                new_def_payoff * new_obs_equivalents
            total_att_payoff = old_att_payoff * old_observations + \
                new_att_payoff * new_obs_equivalents

            updated_observations = old_observations + new_obs_equivalents
            mean_def_payoff = total_def_payoff * 1.0 / updated_observations
            mean_att_payoff = total_att_payoff * 1.0 / updated_observations
            set_observations(game_data, defender, attacker, updated_observations)
            set_def_payoff(game_data, defender, attacker, mean_def_payoff)
            set_att_payoff(game_data, defender, attacker, mean_att_payoff)

# python augment_game_data.py game_3014_5_fixed.json out_morePayoffData_epoch6.json game_3014_5_fixed_aug.json
def main(game_file, new_payoffs_file, result_file):
    '''
    Load the pre-existing game payoff data, then load the new payoff entries.
    Augment the old game data object with the new payoffs, by taking the weighted mean
    of new and pre-existing payoffs.
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
