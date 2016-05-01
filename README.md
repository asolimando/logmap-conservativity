# logmap-conservativity
LogMapC, a LogMap ontology mathcing tool extension for conservativity principle

---- MODIFICATIONS TO LOGMAP 2.4 ----
LogMapC is based on LogMap version 2.4, as available at https://code.google.com/archive/p/logmap-matcher/. 
This is a complete list of the modified/added files to the base version:

uk.ac.ox.krr.logmap2/LogMap2_RepairFacility.java
uk.ac.ox.krr.logmap2/LogMap2Core.java

uk.ac.ox.krr.logmap2.indexing/IndexManager.java
uk.ac.ox.krr.logmap2.indexing/JointIndexManager.java
uk.ac.ox.krr.logmap2.indexing/LightTarjan.java
uk.ac.ox.krr.logmap2.indexing/OntologyProcessing.java

uk.ac.ox.krr.logmap2.indexing.entities/ClassIndex.java
uk.ac.ox.krr.logmap2.indexing.entities/DataPropertyIndex.java
uk.ac.ox.krr.logmap2.indexing.entities/EntityIndex.java
uk.ac.ox.krr.logmap2.indexing.entities/IndividualIndex.java
uk.ac.ox.krr.logmap2.indexing.entities/ObjectPropertyIndex.java
uk.ac.ox.krr.logmap2.indexing.entities/PropertyIndex.java

uk.ac.ox.krr.logmap2.indexing.labelling_schema/Interval.java
uk.ac.ox.krr.logmap2.indexing.labelling_schema/IntervalForNode.java
uk.ac.ox.krr.logmap2.indexing.labelling_schema/IntervalLabelledHierarchy.java
uk.ac.ox.krr.logmap2.indexing.labelling_schema/IntervalNode.java
uk.ac.ox.krr.logmap2.indexing.labelling_schema/IntervalTree.java
uk.ac.ox.krr.logmap2.indexing.labelling_schema/Node.java
uk.ac.ox.krr.logmap2.indexing.labelling_schema/PostNode.java
uk.ac.ox.krr.logmap2.indexing.labelling_schema/PreNode.java

uk.ac.ox.krr.logmap2.mappings.objects/MappingObjectStr.java

uk.ac.ox.krr.logmap2.repair/AnchorAssessment.java
uk.ac.ox.krr.logmap2.repair/DandGCounterThread.java (added)
uk.ac.ox.krr.logmap2.repair/MappingNumViolationsComparator.java

---- EXPERIMENTS REPEATABILITY ----
The classes implementing the experiments reported in our publications are available under "test" and "scc.test" packages

---- PARAMETERS ---- 
LogMap: LogMap's parameters can be specified using "parameters.txt" file distributed with LogMap 2.4 (suggested parameters are kept)
LogMapC: LogMapC's parameters can be specified in "util/Params.java", unfortunately there is no support for external configuration files. All the parameters available in that file are those used for running our experiments

---- HOW TO USE LogMapC ----
Under "repair" package, we provide out-of-the-shelf repair facilities, namely "AlignmentAPIRepairFacility" (implementing the repair interface of the AlignmentAPI) and "ConservativityRepairFacility", providing complete support for all the main functionalities of LogMapC. An example of usage of this class is available in "oaei/LogMap2_OAEI.java", the class implementing the behaviour needed for LogMapC to participate to OAEI (that is, calling the basic LogMap 2.4, and then applying ).

---- Graphical User Interfaces ----
Two GUIs are available for LogMapC, one for subsumption and one for equivalence violations. 
An example of launcher of the GUIs is available at "main/MainGUI.java".

---- Folder structure ----
In order to run correctly, LogMapC requires that the folder structure is preserved, if you experience misbehaviour please 
check that each executable have execution permission and that the folder structure and file namings have not been altered.

---- Supported Operating Systems and Environments ----
The tools have been runned succesfully under Ubuntu 12.04 and Ubuntu 14.04, but also under MacOSX 10.7 Lion (by third parties). 
All the code is compatible with Java7, what makes LogMapC system dependent is the usage of an external ASP solver (Clingo).

--- Importing in Eclipse ---
The projects are known to be running in Eclipse 3.8 under Ubuntu 14.04, import first the project inside LOGMAP folder, and then the one in the main git folder, as it depends on the other one.
