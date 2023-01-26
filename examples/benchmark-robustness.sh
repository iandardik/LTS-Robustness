#!/bin/bash

echo -e "run voting..."
cd voting
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env0.lts -p p.lts -d env1.lts

echo -e "\n\nrun voting-2..."
cd ../voting2
python generator2.py 2 1
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env.lts -p p.lts -d dev.lts

echo -e "\n\nrun voting-3..."
cd ../voting2
python generator2.py 3 1
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env.lts -p p.lts -d dev.lts

echo -e "\n\nrun voting-4..."
cd ../voting2
python generator2.py 4 1
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env.lts -p p.lts -d dev.lts


echo -e "\n\nrun therapy..."
cd ../therac25
timeout 10m java -jar ../../bin/robustifier.jar robustness -s sys.lts -e env0.lts -p p.lts -d env.lts


echo -e "\n\nrun infusion pump..."
cd ../pump
timeout 10m java -jar ../../bin/robustifier.jar robustness --jsons config-robustness.json


echo -e "\n\nrun infusion pump-2..."
cd ../pump2
timeout 10m java -jar ../../bin/robustifier.jar robustness --jsons config-robustness.json
