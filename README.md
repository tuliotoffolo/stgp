# STGP - Sport Teams Group Problem

## Solver for the Sport Teams Group Problem

Written by Túlio Toffolo and Jan Christiaens.

(C) Copyright 2015 by CODeS Research Group, KU Leuven. All rights reserved.  

Please address all contributions, suggestions, and inquiries to the current project administrator.

### Compiling the code

This project uses [gradle](http://gradle.org "Gradle").
It simplifies compiling the code with its dependencies. Just run:

- gradle build

The jar files, such as ``stgp-heuristic.jar``, will be generated.

### Usage examples:

Heuristic:
``java -jar stgp-heuristic.jar <problem_file> <solution_file>``

Validator:
``java -jar stgp-validator.jar <problem_file> <solution_file>``

### Requirements

Java 1.8 and Gson 2.5 are required. Additionaly, libraries for *Cplex* and *Gurobi* must be included within the 'lib' folder.

## Questions?

If you have any questions, please feel free to contact us.  
For additional information, see http://www.toffolo.com.br

Thanks!
