

    config_env_short_name # e.g., s29
    tsv_env_short_name # e.g., sl29_randNoAndB
    cur_epoch # at least 1
    old_strat_disc_fact # in (0, 1]
    save_count # at least 1
    graph_name # e.g., SepLayerGraph0_noAnd_B.json
    env_name_opp_mixed # e.g., DepgraphJavaEnvVsMixedAtt29N-v0
    is_defender_net # in {True, False}
    runs_per_pair # at least 2

if __name__ == '__main__':
    if len(sys.argv) != 5:
        raise ValueError("Need 4 args: config_env_short_name, tsv_env_short_name, " + \
            "cur_epoch, old_strat_disc_fact, save_count, graph_name, " + \
            "env_name_opp_mixed, is_defender_net, runs_per_pair"
            )
    GRAPH_NAME = sys.argv[1]
    ENV_SHORT_NAME = sys.argv[2]
    NEW_EPOCH = int(sys.argv[3])
    ENV_NAME_ATT_NET = sys.argv[4]
    main(GRAPH_NAME, ENV_SHORT_NAME, NEW_EPOCH, ENV_NAME_ATT_NET)