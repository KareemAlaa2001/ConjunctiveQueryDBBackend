# Minibase and Conjunctive Query Minimizer

This project is split into two main parts providing different functionality relevant for running and evaluating queries over a file-based conjuncrtive query database.

The first module is a conjunctive query minimiser which given an input query, finds the minimal equivalent form of that query, which is a useful query optimisation step.

The second module handles the evaluation of conjunctive queries over a given csv-based database. 

## Conjunctive Query Minimizer

This is an implementation of the conjunctive query minimization algorithm described in the INFR11199 Advanced Database Systems lectures. 

This is runnable using from the `CQMinimizer` class in `src/main/java/ed/inf/adbs/minibase/CQMinimizer.java`, which could be compiled using maven and run with the `input_file` and `output_file` relative paths as the command line arguments.


## Minibase

This is an implemenation of a mini-database which supports conjunctive queries over csv files.

This is runnable from the `Minibase` class in `src/main/java/ed/inf/adbs/minibase/Minibase.java` by passing in the database directory, input query txt and output query txt as command line arguments. 

It assumes that the db directory contains a file `schema.txt` defining the schema of the database and a `files` directory which contains a csv file for each relation outlined in the schema given the same name as that of the relation.

The logic for extracting the join conditions is outlined in the comments in the `QueryParser` class. 

