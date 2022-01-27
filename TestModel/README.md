# Testler:

This tool aims at creating a test suite model that can be used to eliminate fine-grained redundancies in Java tests.

In order to create the test model first you need to instrument the project.

**Instruction to Build the Test Model**

1. To instrument the project specify the project path in `Settings.PROJECT_PATH`
2. run `java ca.ubc.salt.model.instrumenter.Instrumenter`
3. You need to run the instrumented project by calling `mvn test` in the project path 
4. Now you can get the model by calling `TestMerger.createModelForTestCases(testCases)` and as input provide a list of testcase names that you want to build the model for

**Merging Usecase**

To merge test cases you can call `java ca.ubc.salt.model.merger.TestMerger`, it builds a test suite model and uses the test model to identify partly redundant test cases, it then tries to reorganize these test cases in a way that removes the redundancy and repeated production method calls. It writes the reorganized test suite back to the disk. Path for the reorganized test suite can be set in `Settings`.

### Publication

The tool and its empirical evaluation have been published in the following conference paper.

- Arash Vahabzadeh, Andrea Stocco, Ali Mesbah. **Fine-Grained Test Minimization.**
    _Proceedings of 40th International Conference on Software Engineering (ICSE 2018) - Research Track_, May 27-June 3 2018, Gothenburg, Sweden, ACM, New York, NY, USA, 2018. [DOI](https://doi.org/10.1145/3180155.3180203)

