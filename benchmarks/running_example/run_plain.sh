echo 'Running Example'
echo '---------------'
echo 'time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl ctrl.lts --prop p.lts --largest-delta-size --naive-bf'
time timeout 5m java -jar ~/Documents/CMU/tolerance-permissiveness/LTS-Robustness/bin/LTS-Robustness.jar --verbose --env env.lts --ctrl ctrl.lts --prop p.lts --largest-delta-size --naive-bf
echo
