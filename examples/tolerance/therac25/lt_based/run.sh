#!/bin/sh

# therac-25 w/bug
echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env_safe.lts --ctrl sys.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env_safe.lts --ctrl sys.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size

# therac-25 w/fix
echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env_safe.lts --ctrl sys_fixed.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env_safe.lts --ctrl sys_fixed.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size

# therac-20
echo 'time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env_safe.lts --ctrl sys_interlock.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size'
time java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env_safe.lts --ctrl sys_interlock.lts --prop p.lts --env-prop envp_term.lts --largest-delta-size
