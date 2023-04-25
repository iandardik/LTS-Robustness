#!/bin/sh

echo 'Voting wrt. P_cfm'
echo '-----------------'
echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size
echo

echo 'Voting wrt. P_all'
echo '-----------------'
echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size
echo
