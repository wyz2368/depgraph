import sys
from train_test_def import unlock_train_def, unlock_eval_def
from train_test_att import unlock_train_att, unlock_eval_att

def main(port_lock_name):
    unlock_train_def(port_lock_name)
    unlock_eval_def(port_lock_name)
    unlock_train_att(port_lock_name)
    unlock_eval_att(port_lock_name)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise ValueError("Need 1 arg: port_lock_name")
    PORT_LOCK_NAME = sys.argv[2]
    main(PORT_LOCK_NAME)
