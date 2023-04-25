#!/bin/sh

echo 'Voting wrt. P_cfm'
echo '-----------------'
echo 'time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size --naive-bf'
time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size --naive-bf
echo

echo 'Voting wrt. P_all'
echo '-----------------'
echo 'time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size --naive-bf'
time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size --naive-bf
echo
