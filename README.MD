This is an implementation of our behavioral robustification tool FORT.

The implementation assumes the user should provide the system specification, the deviated environment w.r.t. some deviation, safety property, progress property, priorities for a set of preferred behavior and controllable/observable events. The tool automatically synthesizes new designs that satisfy the properties and maximize an objective function w.r.t. the common behavior between the original design and the new design and the cost of changes.

We assume all the specifications of the models should be specified in FSP, the modeling language used by [the LTSA tool](https://www.doc.ic.ac.uk/ltsa/).

## System Requirements
This program requires Java version >= 11 and Python 3.8. The program has been tested under
```
openjdk version "11.0.14" 2022-01-18
OpenJDK Runtime Environment (build 11.0.14+9-post-Debian-1deb11u1)
OpenJDK 64-Bit Server VM (build 11.0.14+9-post-Debian-1deb11u1, mixed mode, sharing)

Python 3.8.12
```

## Package Structure
- bin: includes all the executable jars of our tool.
- examples: includes the case study models and scripts to run the benchmark.

## Reproduce the results in the paper
We will use the Voting Machine to show an example of our tool.
### Voting Machine
```
cd examples/voting
java -jar ../../bin/robustifier.jar robustify config-pareto.json
```
This will run the Voting machine example with the SmartPareto search method. You should get output like:
```
2022-08-31 16:26:06.735 [INFO] SupervisoryRobustifier - Number of states of the system: 5
2022-08-31 16:26:06.737 [INFO] SupervisoryRobustifier - Number of states of the environment: 4
2022-08-31 16:26:06.737 [INFO] SupervisoryRobustifier - Number of states of the plant (S || E): 12
2022-08-31 16:26:06.738 [INFO] SupervisoryRobustifier - Number of transitions of the plant: 27
2022-08-31 16:26:06.739 [INFO] SolutionIterator - ==============================>
2022-08-31 16:26:06.739 [INFO] SolutionIterator - Initializing search by using Pareto search...
2022-08-31 16:26:06.739 [INFO] SolutionIterator - Number of preferred behaviors: 1
2022-08-31 16:26:06.740 [INFO] SolutionIterator - Number of controllable events with cost: 9
2022-08-31 16:26:06.740 [INFO] SolutionIterator - Number of observable events with cost: 4
2022-08-31 16:26:06.947 [INFO] SolutionIterator - Maximum fulfilled preferred behavior:
2022-08-31 16:26:06.947 [INFO] SolutionIterator -       select,back,select,vote,confirm
2022-08-31 16:26:06.956 [INFO] SolutionIterator - Initialization completes, time: 00:00:00:216
2022-08-31 16:26:06.957 [INFO] SolutionIterator - Start search from events:
2022-08-31 16:26:06.957 [INFO] SolutionIterator - Controllable: [confirm]
2022-08-31 16:26:06.957 [INFO] SolutionIterator - Observable: [back, confirm, eo.enter, eo.exit, password, select, v.enter, v.exit, vote]
2022-08-31 16:26:06.958 [INFO] SolutionIterator - ==============================>
2022-08-31 16:26:06.958 [INFO] SolutionIterator - Start iteration 1...
2022-08-31 16:26:06.958 [INFO] SolutionIterator - Try to weaken the preferred behavior by one of the 0 behavior sets:
2022-08-31 16:26:07.495 [INFO] SolutionIterator - This iteration completes, time: 00:00:00:537
2022-08-31 16:26:07.496 [INFO] SolutionIterator - Number of controller synthesis process invoked: 14
2022-08-31 16:26:07.497 [INFO] SupervisoryRobustifier - Build new design from controller...
2022-08-31 16:26:07.498 [INFO] SupervisoryRobustifier - Size of the controller: 46 states and 316 transitions
2022-08-31 16:26:07.499 [INFO] SolutionIterator - New pareto-optimal found:
2022-08-31 16:26:07.500 [INFO] SolutionIterator -       Size of the new design: 64 states and 222 transitions
2022-08-31 16:26:07.500 [INFO] SolutionIterator -       Number of controllable events: 1
2022-08-31 16:26:07.500 [INFO] SolutionIterator -       Controllable: [confirm]
2022-08-31 16:26:07.501 [INFO] SolutionIterator -       Number of observable events: 7
2022-08-31 16:26:07.501 [INFO] SolutionIterator -       Observable: [back, confirm, password, select, v.enter, v.exit, vote]
2022-08-31 16:26:07.501 [INFO] SolutionIterator -       Number of preferred behavior: 1
2022-08-31 16:26:07.502 [INFO] SolutionIterator -       Preferred Behavior:
2022-08-31 16:26:07.502 [INFO] SolutionIterator -               select,back,select,vote,confirm
2022-08-31 16:26:07.502 [INFO] SolutionIterator - Utility Preferred Behavior: 30
2022-08-31 16:26:07.502 [INFO] SolutionIterator - Utility Cost: -13
2022-08-31 16:26:07.504 [INFO] SupervisoryRobustifier - Build new design from controller...
2022-08-31 16:26:07.505 [INFO] SupervisoryRobustifier - Size of the controller: 61 states and 419 transitions

...

2022-08-31 16:26:07.585 [INFO] Robustify - Total number of controller synthesis invoked: 15
2022-08-31 16:26:07.585 [INFO] Robustify - Total number of solutions: 6
2022-08-31 16:26:07.620 [INFO] Robustify - Robustification completes, total time: 00:00:01:066
```

The suffix of the JSON config files indicates the different searching methods. For example, to run the Voting example with LocalSearch, run
```
java -jar ../../bin/robustifier.jar robustify config-fast.json
```

### Run the benchmark
We also provide the script to run the benchmark.
```
cd examples
sh benchmark.sh
```
This will run all the problems as listed in the paper.

## How to use in general
