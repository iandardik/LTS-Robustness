#!/bin/sh

#echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_before.lts --largest-delta-size'
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_before.lts --largest-delta-size

#echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_after.lts --largest-delta-size'
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_after.lts --largest-delta-size

#echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_unplug.lts --largest-delta-size'
#time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_unplug.lts --largest-delta-size


echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_req_order.lts --env-prop envp_line.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_req_order.lts --env-prop envp_line.lts --largest-delta-size
