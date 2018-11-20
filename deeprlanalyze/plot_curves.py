import sys
import os
import os.path
from plot_curve_remote import get_values, get_values_even_if_ineligible
from select_best_curve import get_eval_file_name
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt
from pylab import savefig

plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype'] = 42

def has_values(env_short_name, new_epoch, save_count):
    for i in range(save_count):
        vs_eq_file_def = get_eval_file_name(env_short_name, True, False, new_epoch, i)
        vs_retrain_file_def = get_eval_file_name(env_short_name, True, True, new_epoch, i)
        vs_eq_file_att = get_eval_file_name(env_short_name, False, False, new_epoch, i)
        vs_retrain_file_att = get_eval_file_name(env_short_name, False, True, new_epoch, i)
        if not os.path.isfile(vs_eq_file_def) or not os.path.isfile(vs_retrain_file_def) \
            or not os.path.isfile(vs_eq_file_att) or not \
            os.path.isfile(vs_retrain_file_att):
            return False
    return True

def plot_curves(curves, is_defender, env_short_name_payoffs):
    num_curves = len(curves)
    my_size = 1
    while my_size ** 2 < num_curves:
        my_size += 1
    rows = my_size
    cols = my_size
    _, axes = plt.subplots(nrows=rows, ncols=cols, sharex=True, sharey='row', \
                             figsize=(10, 10))

    vs_eq_lists = [x[0] for x in curves]
    vs_retrain_lists = [x[1] for x in curves]
    winner_indexes = [x[2] for x in curves]
    eligible_lists = [x[3] for x in curves]

    y_max = max([max(y) for y in vs_eq_lists])
    y_max = max(y_max, max([max(y) for y in vs_retrain_lists]))

    y_min = min([min(y) for y in vs_eq_lists])
    y_min = min(y_min, min([min(y) for y in vs_retrain_lists]))

    for i in range(num_curves):
        vs_eq_gains = vs_eq_lists[i]
        vs_retrain_gains = vs_retrain_lists[i]
        winner_index = winner_indexes[i]
        is_eligible = eligible_lists[i]

        cur_x = i % cols
        cur_y = i // cols
        ax = axes[cur_y, cur_x]

        for j in range(len(vs_eq_gains)):
            if j == winner_index and is_eligible:
                ax.scatter(vs_retrain_gains[j], vs_eq_gains[j], color='g', marker='o', \
                    s=100)
            else:
                ax.scatter(vs_retrain_gains[j], vs_eq_gains[j], color='r', marker='x', \
                    s=100)
            ax.annotate(str(j), (vs_retrain_gains[j], vs_eq_gains[j]), fontsize=16)

        ax.axhline(y=0, color='gray', lw=2.0, linestyle='--')
        ax.axvline(x=0, color='gray', lw=2.0, linestyle='--')
        ax.set_title("Round " + str(i + 2), fontsize=16)
        ax.spines['right'].set_visible(False)
        ax.spines['top'].set_visible(False)

        # Only show ticks on the left and bottom spines
        ax.yaxis.set_ticks_position('left')
        ax.xaxis.set_ticks_position('bottom')

        ax.set_ylim(y_min - 10, y_max + 10)

    for i in range(num_curves, rows * cols):
        cur_x = i % cols
        cur_y = i // cols
        ax = axes[cur_y, cur_x]
        ax.remove()

    save_name = "curves_" + env_short_name_payoffs
    if is_defender:
        save_name += "_def.pdf"
    else:
        save_name += "_att.pdf"
    savefig(save_name)

def main(env_short_name_payoffs, save_count):
    os.chdir("for_plot_curve")
    new_epoch = 2
    def_list = []
    att_list = []
    while has_values(env_short_name_payoffs, new_epoch, save_count):
        vs_eq_gains_def, vs_retrain_gains_def, winner_index_def, is_eligible = \
            get_values_even_if_ineligible( \
            env_short_name_payoffs, new_epoch, True, save_count)
        def_list.append([vs_eq_gains_def, vs_retrain_gains_def, winner_index_def, \
            is_eligible])
        vs_eq_gains_att, vs_retrain_gains_att, winner_index_att, is_eligible = \
            get_values_even_if_ineligible( \
                env_short_name_payoffs, new_epoch, False, save_count)
        att_list.append([vs_eq_gains_att, vs_retrain_gains_att, winner_index_att, \
            is_eligible])
        new_epoch += 1
    os.chdir("..")

    plot_curves(def_list, True, env_short_name_payoffs)
    plot_curves(att_list, False, env_short_name_payoffs)

'''
example: python3 plot_curves.py s29cs1 4
'''
if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise ValueError("Need 2 args: env_short_name_payoffs, save_count")
    ENV_SHORT_NAME_PAYOFFS = sys.argv[1]
    SAVE_COUNT = int(sys.argv[2])
    main(ENV_SHORT_NAME_PAYOFFS, SAVE_COUNT)
