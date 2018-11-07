import csv
import subprocess
import time
import random
import matplotlib.pyplot as plt
from utility import get_n

def get_beneficial_prob(deviation):
    change_points = deviation[0]
    total = 0
    for i in range(len(change_points) - 1):
        if i % 2 == len(change_points) % 2:
            total += change_points[i + 1] - change_points[i]
    return total

def get_random_strats(strat_count, digit_count):
    return sorted([round(random.random(), digit_count) for _ in range(strat_count)])

def u1_expected(s1, s2):
    if s1 == 0:
        return 0
    if s2 == 0:
        return (1 - s1) / 2.
    if s1 <= s2:
        return ((1 - s1) * s1) / (3. * s2)
    return (1 - s1) * ((3 * (s1 ** 2) - (s2 ** 2)) / (6. * (s1 ** 2)))

def u1_expected_mixed_opp(s1, prob_2, s2_list):
    if len(prob_2) != len(s2_list):
        raise ValueError("Lists must have equal length")
    u1_total = 0
    for i in range(len(prob_2)):
        if prob_2[i] > 0:
            s2 = s2_list[i]
            p2 = prob_2[i]
            u1_total += p2 * u1_expected(s1, s2)
    return u1_total

def u1_expected_mixed_both(prob_1, prob_2, s_list):
    if len(prob_1) != len(s_list):
        raise ValueError("Lists must have equal length")
    u1_total = 0
    for i in range(len(prob_1)):
        if prob_1[i] > 0:
            s1 = s_list[i]
            p1 = prob_1[i]
            u1_total += p1 * u1_expected_mixed_opp(s1, prob_2, s_list)
    return u1_total

def get_msnes(file_name):
    result = []
    with open(file_name, "r") as csv_file:
        reader = csv.reader(csv_file, delimiter=",")
        for row in reader:
            result.append([float(i) for i in row])
    return result

def get_payoff_matrix(strats, digit_count):
    result = []
    for i in range(len(strats)):
        s1 = strats[i]
        cur_list = []
        for j in range(len(strats)):
            s2 = strats[j]
            u1 = u1_expected(s1, s2)
            cur_list.append(round(u1, digit_count))
        result.append(cur_list)
    return result

def record_payoff_matrix(payoff_matrix, out_file_name):
    with open(out_file_name, "w") as csv_file:
        writer = csv.writer(csv_file, delimiter=',')
        for row in payoff_matrix:
            writer.writerow(row)

def exp_devs_part_1(strat_count, digit_count):
    strats = get_random_strats(strat_count, digit_count)
    payoff_matrix = get_payoff_matrix(strats, digit_count)
    record_payoff_matrix(payoff_matrix, "test_payoff_matrix.csv")
    return strats, payoff_matrix

def find_deviations(msne, strat_list, grid_points):
    u1_old = u1_expected_mixed_both(msne, msne, strat_list)
    change_points = []
    is_beneficial = False
    for x in range(grid_points):
        deviating_strat = x * 1.0 / (grid_points - 1)
        u1 = u1_expected_mixed_opp(deviating_strat, msne, strat_list)
        if u1 > u1_old:
            if not is_beneficial:
                change_points.append(deviating_strat)
                is_beneficial = True
        else:
            if is_beneficial or not change_points:
                change_points.append(deviating_strat)
                is_beneficial = False
    return (change_points, is_beneficial)

def exp_devs_part_2(strats):
    msnes = get_msnes("test_msne.csv")
    devs = [find_deviations(msne, strats, 1001) for msne in msnes]
    return msnes, devs

def get_dev_p_vs_acc_rt_multi(strat_count, digit_count, max_p, alpha_list,
                              test_count, max_rounds):
    results = []
    start_time = time.time()
    for i in range(test_count):
        strats, payoff_matrix = exp_devs_part_1(strat_count, digit_count)

        cur_round = 0
        was_confirmed = False
        msne = None
        while cur_round < max_rounds:
            # call Python2 to run Gambit, because can't run it from Python3.
            # this script must be in Python3, because Jupyter notebook
            # only supports Python3.
            python3_command = "python gambit_test.py test_payoff_matrix.csv"
            process = subprocess.Popen(python3_command.split(), stdout=subprocess.PIPE)
            _, _ = process.communicate()

            msnes, _ = exp_devs_part_2(strats)
            rand_index = random.randrange(len(msnes))
            msne = msnes[rand_index]

            error_prob_one_side = alpha_list[cur_round]
            n = get_n(max_p, error_prob_one_side)

            u1_old = u1_expected_mixed_both(msne, msne, strats)
            found_dev = False
            for _ in range(n):
                deviating_strat = random.random()
                u1 = u1_expected_mixed_opp(deviating_strat, msne, strats)
                if u1 > u1_old:
                    found_dev = True
                    if cur_round + 1 < max_rounds:
                        strats.append(deviating_strat)
                        payoff_matrix = get_payoff_matrix(strats, digit_count)
                        record_payoff_matrix(payoff_matrix, "test_payoff_matrix.csv")
                    break
            if not found_dev:
                was_confirmed = True
                break
            cur_round += 1
        cur_result = (was_confirmed, strats, msne, cur_round)
        if i % 10 == 0:
            print(str(i) + "\t" + str(cur_result), flush=True)
        results.append(cur_result)
    seconds_taken = time.time() - start_time
    print("Seconds taken for games: " + str(seconds_taken), flush=True)
    return results

def analyze_results_multi(result_tuples, grid_points, max_p, error_tol):
    false_positives = 0
    false_negatives = 0
    true_positives = 0
    true_negatives = 0
    round_total = 0

    true_ps = []
    accept_results = []
    start_time = time.time()
    for result_tuple in result_tuples:
        was_confirmed, strats, msne, cur_round = result_tuple
        true_beneficial_prob = \
            get_beneficial_prob(find_deviations(msne, strats, grid_points))
        true_ps.append(true_beneficial_prob)
        accept_results.append(1 if was_confirmed else 0)
        should_confirm = (true_beneficial_prob <= max_p)
        round_total += cur_round
        if should_confirm:
            if was_confirmed:
                true_positives += 1
            else:
                false_negatives += 1
        else:
            if was_confirmed:
                false_positives += 1
            else:
                true_negatives += 1
    false_pos_rate = false_positives * 1. / len(result_tuples)
    false_neg_rate = false_negatives * 1. / len(result_tuples)
    true_pos_rate = true_positives * 1. / len(result_tuples)
    true_neg_rate = true_negatives * 1. / len(result_tuples)
    mean_round_count = round_total * 1. / len(result_tuples)

    seconds_taken = time.time() - start_time
    print("Seconds taken for analysis: " + str(seconds_taken), flush=True)

    print("Error tolerance: " + str(error_tol))
    print("False positive rate: " + str(false_pos_rate))
    print("False negative rate: " + str(false_neg_rate))
    print("True positive rate: " + str(true_pos_rate))
    print("True negative rate: " + str(true_neg_rate))
    print("Mean round count: " + str(mean_round_count), flush=True)

    base_strat_count = min([len(x[1]) for x in result_tuples])
    max_p_string = str(int(10000 * max_p))
    error_tol_string = str(int(100 * error_tol))
    out_file_name = "mult_round_output_" + str(base_strat_count) + \
        "_" + max_p_string + "_" + error_tol_string + ".csv"
    with open(out_file_name, "w") as csv_file:
        writer = csv.writer(csv_file, delimiter=',')
        writer.writerow(true_ps)
        writer.writerow(accept_results)
    print("Wrote to file: " + out_file_name)
    return (true_ps, accept_results)

def get_bucket_y_means(x_vals, y_vals, bucket_count):
    y_sorted_by_x = [y for (x, y) in sorted(zip(x_vals, y_vals))]
    x_sorted = sorted(x_vals)
    bucket_width = 1. / bucket_count
    x_index = 0
    bucket_centers = []
    bucket_y_means = []
    for bucket_index in range(bucket_count):
        bucket_min = bucket_index * bucket_width
        bucket_max = bucket_min + bucket_width
        cur_bucket_indexes = []
        while x_index < len(x_sorted) and x_sorted[x_index] <= bucket_max:
            cur_bucket_indexes.append(x_index)
            x_index += 1
        if cur_bucket_indexes:
            bucket_center = bucket_min + 0.5 * bucket_width
            bucket_y_mean = sum([y_sorted_by_x[j] for j in \
                cur_bucket_indexes]) * 1. / len(cur_bucket_indexes)
            bucket_centers.append(bucket_center)
            bucket_y_means.append(bucket_y_mean)
    return bucket_centers, bucket_y_means

def plot_acccept_rate_vs_p(true_ps, accept_rates, bucket_count, max_p, error_tol):
    bucket_centers, bucket_y_means = get_bucket_y_means(true_ps, accept_rates, bucket_count)
    fig = plt.figure()
    axis = plt.subplot()
    plt.plot(bucket_centers, bucket_y_means, lw=2)
    plt.axvline(x=max_p, c='red', lw=2, ls='--')
    plt.axhline(y=error_tol, c='green', lw=2, ls='-.')
    axis.set_xlabel("$p$", fontsize=16)
    axis.set_ylabel("Clopper-Pearson\naccept rate", fontsize=16)
    axis.spines['right'].set_visible(False)
    axis.spines['top'].set_visible(False)
    axis.xaxis.set_ticks_position('bottom')
    axis.yaxis.set_ticks_position('left')
    max_p_str = str(int(100 * max_p))
    error_tol_str = str(int(100 * error_tol))
    file_name = "accept_rate_vs_true_p_" + max_p_str + "_" + \
        error_tol_str + ".pdf"
    print("Plotting to file: " + file_name)
    fig.savefig(file_name, bbox_inches='tight')

def record_dev_prob_vs_accept_rate(true_ps, accept_rates, max_p, error_tol):
    max_p_str = str(int(max_p * 10000))
    error_tol_str = str(int(error_tol * 100))
    out_file_name = "dev_prob_vs_accept_rate_" + str(len(true_ps)) + "_" + \
        max_p_str + "_" + error_tol_str +  ".csv"
    with open(out_file_name, "w") as csv_file:
        writer = csv.writer(csv_file, delimiter=',')
        writer.writerow(true_ps)
        writer.writerow(accept_rates)
    print("Wrote to file: " + out_file_name)

def get_data_from_file(file_name):
    results = get_msnes(file_name)
    true_ps = results[0]
    accept_rates = results[1]
    return true_ps, accept_rates

def main(strat_count, digit_count, max_p, error_tolerance, test_count, max_rounds):
    alpha_list = [error_tolerance * 1.0 / max_rounds] * max_rounds
    print("alpha_list: " + str(alpha_list))
    result_tuples = get_dev_p_vs_acc_rt_multi(strat_count, digit_count, max_p, alpha_list, \
        test_count, max_rounds)

    grid_points = 50000
    true_ps, accept_results = analyze_results_multi(result_tuples, grid_points, max_p, \
        error_tolerance)

    record_dev_prob_vs_accept_rate(true_ps, accept_results, max_p, error_tolerance)

    '''
    max_p_str = str(int(max_p * 10000))
    error_tol_str = str(int(error_tol * 100))
    out_file_name = "dev_prob_vs_accept_rate_" + str(len(true_ps)) + "_" + \
        max_p_str + "_" + error_tol_str +  ".csv"
    (true_ps, accept_results) = get_data_from_file(out_file_name)
    '''

    bucket_count = 50
    plot_acccept_rate_vs_p(true_ps, accept_results, bucket_count, max_p, error_tolerance)

if __name__ == "__main__":
    STRAT_COUNT = 10
    DIGIT_COUNT = 4
    MAX_P = 0.005
    ERROR_TOLERANCE = 0.1
    TEST_COUNT = 100
    MAX_ROUNDS = 5
    main(STRAT_COUNT, DIGIT_COUNT, MAX_P, ERROR_TOLERANCE, TEST_COUNT, MAX_ROUNDS)
