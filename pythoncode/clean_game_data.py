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

def clean_game_data(game_data):
    '''
    -- for each profile in "profiles":
            -- check if either group in "symmetry_groups" has "pkl" in "strategy"
            -- if so:
                -- set "observations_count" to 1
    '''
    for profile in game_data["profiles"]:
        should_update = False
        for group in profile["symmetry_groups"]:
            strat = group["strategy"]
            if "pkl" in strat:
                should_update = True
        if should_update:
            profile["observations_count"] = 1

# python clean_game_data.py game_3014_5.json game_3014_5_fixed.json
def main(game_file, result_file):
    '''
    Load the pre-existing game payoff data, change all symmetry_groups with a "pkl" strategy
    name ot have observations_count 1, and output to Json.
    Print the extended object to file as Json.
    '''
    game_data = get_json_data(game_file)
    clean_game_data(game_data)
    print_json(result_file, game_data)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError( \
            "Need 3 args: game_file, result_file")
    GAME_FILE = sys.argv[1]
    RESULT_FILE = sys.argv[2]
    main(GAME_FILE, RESULT_FILE)
