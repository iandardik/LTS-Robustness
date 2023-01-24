#!/bin/sh

# property 1
echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p.lts --env-prop envp_v.lts"
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p.lts --env-prop envp_v.lts

# property 2
echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p2.lts --env-prop envp_v.lts"
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p2.lts --env-prop envp_v.lts

# property 3
echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p3.lts --env-prop envp_v.lts"
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p3.lts --env-prop envp_v.lts
