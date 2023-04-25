#!/bin/sh

echo 'PCA Pump'
echo '--------'
echo 'time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_req_order.lts --env-prop envp_line.lts --largest-delta-size --naive-bf'
time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env0.lts --ctrl sys.lts --prop p.lts --env-prop envp_req_order.lts --env-prop envp_line.lts --largest-delta-size --naive-bf
echo
