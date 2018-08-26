import csv
import matplotlib.pyplot as plt

def analyze_results_from_file(true_ps, accept_rates, max_p, error_tol):
    false_positives = 0
    false_negatives = 0
    true_positives = 0
    true_negatives = 0

    for i in range(len(true_ps)):
        true_p = true_ps[i]
        accept_rate = accept_rates[i]
        was_confirmed = (accept_rate > 0.5)
        should_confirm = (true_p <= max_p)
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
    false_pos_rate = false_positives * 1. / len(true_ps)
    false_neg_rate = false_negatives * 1. / len(true_ps)
    true_pos_rate = true_positives * 1. / len(true_ps)
    true_neg_rate = true_negatives * 1. / len(true_ps)

    print("Error tolerance: " + str(error_tol))
    print("Examples: " + str(len(true_ps)))
    print("False positive rate: " + str(false_pos_rate))
    print("False negative rate: " + str(false_neg_rate))
    print("True positive rate: " + str(true_pos_rate))
    print("True negative rate: " + str(true_neg_rate), flush=True)

def get_msnes(file_name):
    result = []
    with open(file_name, "r") as csv_file:
        reader = csv.reader(csv_file, delimiter=",")
        for row in reader:
            result.append([float(i) for i in row])
    return result

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
    axis.set_xscale("log")
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

def get_data_from_file(file_name):
    results = get_msnes(file_name)
    true_ps = results[0]
    accept_rates = results[1]
    return true_ps, accept_rates

def main(max_p, error_tolerance):
    out_file_name = "dpvar_100_5_10_all.csv"
    (true_ps, accept_results) = get_data_from_file(out_file_name)

    analyze_results_from_file(true_ps, accept_results, max_p, error_tolerance)

    bucket_count = 7500
    plot_acccept_rate_vs_p(true_ps, accept_results, bucket_count, max_p, error_tolerance)

if __name__ == "__main__":
    MAX_P = 0.0005
    ERROR_TOLERANCE = 0.1
    main(MAX_P, ERROR_TOLERANCE)
