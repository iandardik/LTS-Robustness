#!/bin/sh

# property 1
echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --largest-delta-size"
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_cfm.lts --env-prop envp_v.lts --largest-delta-size

# property 2
echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_select.lts --env-prop envp_v.lts --largest-delta-size"
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_select.lts --env-prop envp_v.lts --largest-delta-size

# property 3
# since this is the conjunction of the previous two, this property is stronger than both.
# thus we expect any controller to be less tolerant with respect to this property than the previous two.
echo "time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_both.lts --env-prop envp_v.lts --largest-delta-size"
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl sys.lts --prop p_both.lts --env-prop envp_v.lts --largest-delta-size
