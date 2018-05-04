'''
Decode the output of Gambit's LCP solver for mixed-strategy Nash equilibria,
by printing out the corresponding strategy name for each weight in the mixed strategy.

Takes as input the LCP solution from Gambit, and a 2-player .nfg (outcome version) game file.
See also: http://www.gambit-project.org/gambit13/formats.html
'''

import sys

def get_mixture_weights(gambit_solution_string):
    '''
    Return the mixed strategy weights from the "LCP" solution method of Gambit.
    '''
    values = gambit_solution_string.strip().split(",")
    values.pop(0)
    values = [float(x) for x in values]
    return values

def print_outputs(attacker_strategies, defender_strategies, is_attacker_first, \
    mixture_weights):
    '''
    Print the corresponding strategy name with each mixture weight, for the given mixed
    strategy.
    '''
    if len(attacker_strategies) + len(defender_strategies) != len(mixture_weights):
        raise ValueError("Wrong numbre of mixture weights: " + str(len(mixture_weights)) + \
            ", expected " + str(len(attacker_strategies) + len(defender_strategies)))
    attacker_offset = 0 if is_attacker_first else len(defender_strategies)
    print("Attacker mixed strategy:")
    for i in range(len(attacker_strategies)):
        cur_weight = mixture_weights[i + attacker_offset]
        cur_attacker = attacker_strategies[i]
        if cur_weight > 0.0:
            print(cur_attacker + "\t" + str(cur_weight))
    print("\nDefender mixed strategy:")
    defender_offset = len(attacker_strategies) if is_attacker_first else 0
    for i in range(len(defender_strategies)):
        cur_weight = mixture_weights[i + defender_offset]
        cur_defender = defender_strategies[i]
        if cur_weight > 0.0:
            print(cur_defender + "\t" + str(cur_weight))

def get_profile_names(gambit_string):
    '''
    Given the text of the game file, return the lists of attacker and defender strategy names
    in order, and whether the attacker is listed as the first player.
    '''
    first_open_curly = gambit_string.find("{")
    second_open_curly = gambit_string.find("{", first_open_curly + 1)

    is_attacker_first = (gambit_string.find("attacker") < gambit_string.find("defender"))

    third_open_curly = gambit_string.find("{", second_open_curly + 1)
    next_close_curly = gambit_string.find("}", third_open_curly + 1)
    first_inner = gambit_string[third_open_curly + 1 : next_close_curly]
    first_inner = first_inner.strip()
    first_names = first_inner.split()
    first_names = [x.replace("\"", "") for x in first_names]

    fourth_open_curly = gambit_string.find("{", next_close_curly + 1)
    next_close_curly = gambit_string.find("}", fourth_open_curly + 1)
    second_inner = gambit_string[fourth_open_curly + 1 : next_close_curly]
    second_inner = second_inner.strip()
    second_names = second_inner.split()
    second_names = [x.replace("\"", "") for x in second_names]

    attacker_strings = first_names
    defender_strings = second_names
    if not is_attacker_first:
        defender_strings = first_names
        attacker_strings = second_names
    return (attacker_strings, defender_strings, is_attacker_first)

def get_file_text(file_name):
    '''
    Return the file's text as one string.
    '''
    with open(file_name, 'r') as my_file:
        data = my_file.read()
        return data

def main(game_file_name, solution_file_name):
    '''
    Load the lists of attacker and defender strategy names, and whether the attacker is
    listed first, from the game file.
    Load the list of mixed strategy weights from the solution file.
    Then print the corresponding strategy names for all non-zero weights in the solution.
    '''
    gambit_data = get_file_text(game_file_name)
    (attacker_strings, defender_strings, is_attacker_first) = get_profile_names(gambit_data)

    solution_data = get_file_text(solution_file_name)
    mixture_weights = get_mixture_weights(solution_data)
    print_outputs(attacker_strings, defender_strings, is_attacker_first, mixture_weights)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise ValueError("Needs 2 arguments: game_file_name solution_file_name")
    GAME_FILE_NAME = sys.argv[1]
    SOLUTION_FILE_NAME = sys.argv[2]
    main(GAME_FILE_NAME, SOLUTION_FILE_NAME)
