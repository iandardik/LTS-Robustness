#!/bin/bash

echo -e "run voting..."
cd voting
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env0.lts -p p.lts -d env1.lts

echo -e "\n\nrun voting-2..."
cd ../voting2
python generator2.py 2 2
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env.lts -p p.lts -d dev.lts

echo -e "\n\nrun voting-4..."
cd ../voting2
python generator2.py 4 4
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env.lts -p p.lts -d dev.lts

echo -e "\n\nrun voting-8..."
cd ../voting2
python generator2.py 8 8
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env.lts -p p.lts -d dev.lts

echo -e "\n\nrun perfect protocol..."
cd ../abp
timeout 10m java -jar ../../bin/robustifier.jar robustness -s perfect.lts -e abp_env.lts -p p.lts -d abp_env_lossy.lts
echo -e "\n\nrun ABP protocol..."
timeout 10m java -jar ../../bin/robustifier.jar robustness -s abp.lts -e abp_env.lts -p p.lts -d abp_env_lossy.lts

echo -e "\n\nrun therapy..."
cd ../therac25
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env0.lts -p p.lts -d env.lts

echo -e "\n\nrun infusion pump..."
cd ../pump
timeout 10m java -jar ../../bin/robustifier.jar robustness --jsons config-robustness.json

echo -e "\n\nrun infusion pump-2..."
cd ../pump2
timeout 10m java -jar ../../bin/robustifier.jar robustness --jsons config-robustness.json

echo -e "\n\nrun infusion pump-4..."
cd ../pump4
timeout 10m java -Xmx16g -jar ../../bin/robustifier.jar robustness --jsons config-robustness.json