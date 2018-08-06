import sys
from select_best_curve import CUR_EQ_WEIGHT, get_cur_eq_payoff, get_eval_file_name, \
    get_net_payoff

def get_truth_value(str_input):
    if str_input == "True":
        return True
    if str_input == "False":
        return False
    raise ValueError("Must be True or False: " + str_input)

def main(env_short_name, new_epoch, is_defender, save_count):
    vs_eq_gains, vs_retrain_gains, winner = get_values(env_short_name, new_epoch, \
        is_defender, save_count)
    print(vs_eq_gains)
    print(vs_retrain_gains)
    print(winner)

def get_values(env_short_name, new_epoch, is_defender, save_count):
    '''
    Returns the index in {0, . . ., save_count} of the best trained or retrained agent,
    as defined below. The agent will be a defender if is_defender, in new_epoch round of
    training.
    The best agent is defined as an agent that gets strictly higher expected payoff than the
    current equilibrium agent against the current equilibrium opponent, and that achieves
    the highest weighted mean of this gain in expected payoff, and the gain in expected
    payoff against the retraining mixed strategy, relative to the initially trained (but not
    fine-tuned) agent from this training round.
    The weighting is as follows:
        J = CUR_EQ_WEIGHT * (myPayoffVsEq - eqPayoffVsEq) + \
            (1 - CUR_EQ_WEIGHT) * (myPayoffVsRetrainMix - firstTrainedNetPayoffVsRetrainMix)
    We set the constraint that
        (myPayoffVsEq - eqPayoffVsEq) > 0.
    If none of the (save_count + 1) trained nets meets this constraint return None.
    Otherwise, return the integer in {0, . . ., save_count} of the best.
    '''
    if CUR_EQ_WEIGHT <= 0.0 or CUR_EQ_WEIGHT >= 1.0:
        raise ValueError("Invalid CUR_EQ_WEIGHT: " + str(CUR_EQ_WEIGHT))
    cur_eq_payoff = get_cur_eq_payoff(env_short_name, is_defender, new_epoch)

    vs_eq_payoffs = []
    vs_retrain_payoffs = []
    for retrain_number in range(save_count):
        vs_eq_file = get_eval_file_name(env_short_name, is_defender, False, new_epoch, \
            retrain_number)
        vs_eq_payoff = get_net_payoff(vs_eq_file)
        vs_eq_payoffs.append(vs_eq_payoff)
        vs_retrain_file = get_eval_file_name(env_short_name, is_defender, True, new_epoch, \
            retrain_number)
        vs_retrain_payoff = get_net_payoff(vs_retrain_file)
        vs_retrain_payoffs.append(vs_retrain_payoff)

    vs_eq_gains = [x - cur_eq_payoff for x in vs_eq_payoffs]
    vs_retrain_gains = [x - vs_retrain_payoffs[0] for x in vs_retrain_payoffs]

    is_eligible = [x > 0 for x in vs_eq_gains]
    scores = []
    for i in range(len(vs_eq_payoffs)):
        if is_eligible[i]:
            cur_score = CUR_EQ_WEIGHT * vs_eq_gains[i] + \
                (1.0 - CUR_EQ_WEIGHT) * vs_retrain_gains[i]
            scores.append(cur_score)
        else:
            scores.append(float('-inf'))
    if True not in is_eligible:
        return None
    winner = scores.index(max(scores))
    if not is_eligible[winner]:
        raise ValueError("Illegal result: " + str(winner) + ", " + str(is_eligible))
    return vs_eq_gains, vs_retrain_gains, winner

# example: python3 plot_best_curve.py d30cd1_randNoAndB 2 True 4
if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise ValueError("Needs 3 arguments: env_short_name, new_epoch, is_defender, " + \
            "save_count")
    ENV_SHORT_NAME = sys.argv[1]
    NEW_EPOCH = int(sys.argv[2])
    IS_DEFENDER = get_truth_value(sys.argv[3])
    SAVE_COUNT = int(sys.argv[4])
    main(ENV_SHORT_NAME, NEW_EPOCH, IS_DEFENDER, SAVE_COUNT)
