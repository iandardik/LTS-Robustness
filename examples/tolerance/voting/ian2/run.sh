#!/bin/sh

echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size

echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq_back.lts --largest-delta-size






## property P_cfm
#echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq.lts --largest-delta-size"
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --env-prop envp_correct_seq.lts --largest-delta-size
#
## property P_all
#echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq.lts --largest-delta-size"
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq.lts --largest-delta-size
#
## fig 10(b) from the paper
##time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --env-prop envp_correct_seq.lts --print-fsp
#
#
## w/o envp_correct_seq
## property P_cfm
#echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --largest-delta-size"
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --largest-delta-size
#
## property P_all
#echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --largest-delta-size"
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_all.lts --env-prop envp_v.lts --largest-delta-size




## property 1
#echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --largest-delta-size"
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --largest-delta-size
#
## property 2
#echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_select.lts --env-prop envp_v.lts --largest-delta-size"
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_select.lts --env-prop envp_v.lts --largest-delta-size
#
## property 3
## since this is the conjunction of the previous two, this property is stronger than both.
## thus we expect any controller to be less tolerant with respect to this property than the previous two.
#echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_both.lts --env-prop envp_v.lts --largest-delta-size"
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_both.lts --env-prop envp_v.lts --largest-delta-size
#
## property 3 with envp_correct_seq
#echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_both.lts --env-prop envp_v.lts --env-prop envp_correct_seq.lts --largest-delta-size'
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_both.lts --env-prop envp_v.lts --env-prop envp_correct_seq.lts --largest-delta-size
