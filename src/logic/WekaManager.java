package logic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.CostMatrix;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.core.Instances;
import weka.core.WekaException;
import weka.core.converters.CSVLoader;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

public class WekaManager {

    private static final Logger LOGGER = Logger.getLogger(WekaManager.class.getName());
    private static final double CFP = 1.0;
    private static final double CFN = 10 * CFP;

    public enum ClassifierType {
        RANDOM_FOREST,
        NAIVE_BAYES,
        IBK
    }

    public enum FeatureSelection {
        NO_FEATURE_SELECTION,
        BEST_FIRST
    }

    public enum Sampling {
        NO_SAMPLING,
        OVERSAMPLING,
        UNDERSAMPLING,
        SMOTE
    }

    public enum CostSensitive {
        NO_COST_SENSITIVE,
        SENSITIVE_THRESHOLD,
        SENSITIVE_LEARNING
    }

    private static class StepInput {
        private String datasetName;
        private int trainingRelease;
        private List<ClassifierType> classifierType;
        private List<FeatureSelection> featureSelection;
        private List<Sampling> sampling;
        private List<CostSensitive> sensitivity;

        public StepInput(String datasetName, int trainingRelease, List<ClassifierType> classifierType, List<FeatureSelection> featureSelection, List<Sampling> sampling, List<CostSensitive> sensitivity ) {
            this.datasetName = datasetName;
            this.trainingRelease = trainingRelease;
            this.classifierType = classifierType;
            this.featureSelection = featureSelection;
            this.sampling = sampling;
            this.sensitivity = sensitivity;
        }

        public String getDatasetName() { return this.datasetName; }
        public int getTrainingRelease() { return this.trainingRelease; }
        public List<ClassifierType> getClassifierType() { return this.classifierType; }
        public List<FeatureSelection> getFeatureSelection() { return this.featureSelection; }
        public List<Sampling> getSampling() { return this.sampling; }
        public List<CostSensitive> getSensitivity() { return this.sensitivity; }
    }

    // return[0] --> Training Set: from release 1 to lastTrainingIndex
    // return[1] --> Testing Set: lastTrainingIndex+1
    private static  List<Instances> splitSets(Instances instances, int lastTrainingIndex) {
        List<Instances> splittedSets = new ArrayList<>();

        // init training set and testing set
        Instances trainingSet = new Instances(instances, 0);
        Instances testingSet = new Instances(instances,0);

        int index;

        //training set
        for(index=1; index<=lastTrainingIndex; index++) {
            int releaseIndex = index;
            instances.parallelStream().filter(instance -> instance.toString(0).equals(Integer.toString(releaseIndex))).forEachOrdered(trainingSet::add);
        }

        splittedSets.add(trainingSet);

        //testing set
        final int testIndex;
        testIndex = index;

        instances.parallelStream().filter(instance -> instance.toString(0).equals(Integer.toString(testIndex))).forEachOrdered(testingSet::add);
        splittedSets.add(testingSet);

        return splittedSets;
    }

    public static List<List<WekaData>> walkForward(String projectName, String datasetPath, List<ClassifierType> classifierList, List<FeatureSelection> featureSelectionList, List<Sampling> samplingList, List<CostSensitive> costList) throws WekaException {
        List<Instances> splittedSets;
        Instances trainingSet;
        Instances testingSet;
        StepInput input;
        String datasetName;
        String report;

        List<WekaData> stepData;

        int numReleases;

        // return data
        List<List<WekaData>> returnData = new ArrayList<>();

        try{
            CSVLoader csvLoader = new CSVLoader();
            csvLoader.setSource(new File(datasetPath));

            // Yes as value of interest for Buggy
            csvLoader.setNominalLabelSpecs(new Object[]{"Buggy:Yes,No"});

            // retrieve dataset
            Instances dataset = csvLoader.getDataSet();

            // remove class name
            dataset.deleteAttributeAt(1);

            // set attribute of interest
            dataset.setClassIndex(dataset.numAttributes()-1);

            numReleases = (int) dataset.lastInstance().value(0);

            // skip fir step with null training set
            for(int i=1; i<numReleases; i++) {
                // split training and testing
                splittedSets = splitSets(dataset, i);
                trainingSet = splittedSets.get(0);
                testingSet = splittedSets.get(1);


                // remove release index after split
                trainingSet.deleteAttributeAt(0);
                testingSet.deleteAttributeAt(0);

                 datasetName = projectName + "-step_" + i;

                input = new StepInput(datasetName, i, classifierList, featureSelectionList, samplingList, costList);

                trainingSet.setClassIndex(trainingSet.numAttributes()-1);
                testingSet.setClassIndex(testingSet.numAttributes()-1);

                // single step
                stepData = walkStep(input, trainingSet, testingSet);
                returnData.add(stepData);

                report = "Step " + i + "/" + (numReleases-1) + " completed";
                LOGGER.log(Level.INFO, report);
            }

        }catch(Exception e) {
            throw new WekaException(e);
        }
        return returnData;
    }

    private static List<WekaData> walkStep(StepInput stepInput, Instances trainingSet, Instances testingSet) throws WekaException {

        List<WekaData> stepData = new ArrayList<>();

        // feature selection
        for(FeatureSelection featureSelection: stepInput.getFeatureSelection()) {
            // sampling
            for(Sampling sampling: stepInput.getSampling()) {
                // classifier
                for(ClassifierType classifierType: stepInput.getClassifierType()) {
                    // cost sensitive
                    for(CostSensitive costSensitive: stepInput.getSensitivity()) {
                        List<Instances> featureSelectionDatasets = applyFeatureSelection(trainingSet, testingSet, featureSelection);
                        Instances fsTraining = featureSelectionDatasets.get(0);
                        Instances fsTesting = featureSelectionDatasets.get(1);
                        Instances sampledTraining = applySampling(fsTraining, sampling);
                        Classifier classifier = generateClassifier(classifierType, sampledTraining);
                        CostSensitiveClassifier costClassifier = applyCostSensitive(classifier, costSensitive, sampledTraining);
                        WekaData data = new WekaData(stepInput.getDatasetName(), stepInput.getTrainingRelease(), featureSelection, sampling, classifierType, costSensitive);
                        evaluateData(data, sampledTraining, fsTesting, classifier, costClassifier);
                        if(data.isValid())
                            stepData.add(data);
                    }
                }
            }
        }

        return stepData;
    }

    /**********************************************************
     * @return  [0] trainingSet filtered with feature selection
     *          [1] testingSet filtered with feature selection
     **********************************************************/
    private static List<Instances> applyFeatureSelection(Instances trainingSet, Instances testingSet, FeatureSelection featureSelection) throws WekaException {
        List<Instances> filteredList ;
        switch(featureSelection){
            case NO_FEATURE_SELECTION:
                filteredList = new ArrayList<>();
                filteredList.add(0, trainingSet);
                filteredList.add(1, testingSet);
                break;
            case BEST_FIRST:
                filteredList = applyBestFirst(trainingSet, testingSet);
                break;
            default:
                throw new IllegalArgumentException("Invalid feature selection");
        }

        return filteredList;
    }

    /**********************************************************
     * @return  [0] trainingSet filtered with feature selection
     *          [1] testingSet filtered with feature selection
     **********************************************************/
    private static List<Instances> applyBestFirst(Instances trainingSet, Instances testingSet) throws WekaException {
        List<Instances> filteredList = new ArrayList<>();
        Instances filteredTraining;
        Instances filteredTesting;

        // generate AttributeSelection, SubsetEvaluator and SearchAlgorithm
        AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval evaluator = new CfsSubsetEval();
        BestFirst search = new BestFirst();

        try{
            // setup the filter
            filter.setEvaluator(evaluator);
            filter.setSearch(search);
            filter.setInputFormat(trainingSet);

            // use filter
            filteredTraining = Filter.useFilter(trainingSet, filter);
            filteredTesting = Filter.useFilter(testingSet, filter);

            filteredList.add(0, filteredTraining);
            filteredList.add(1, filteredTesting);

        }catch(Exception e){
            throw new WekaException(e);
        }

        return filteredList;
    }

    private static Instances applySampling(Instances trainingSet, Sampling sampling) throws WekaException {
        Instances filteredTraining;
        switch(sampling) {
            case NO_SAMPLING:
                filteredTraining = trainingSet;
                break;
            case OVERSAMPLING:
                filteredTraining = oversampling(trainingSet);
                break;
            case UNDERSAMPLING:
                filteredTraining = undersampling(trainingSet);
                break;
            case SMOTE:
                filteredTraining = smote(trainingSet);
                break;
            default:
                throw new IllegalArgumentException("Invalid sampling");
        }

        return filteredTraining;
    }

    private static Instances oversampling(Instances trainingSet) throws WekaException {
            Resample resample = new Resample();
            Instances filtered;

            resample.setNoReplacement(false);
            resample.setBiasToUniformClass(1.0);
            resample.setSampleSizePercent(getOversamplingPercentage(trainingSet));
            try {
                resample.setInputFormat(trainingSet);
                filtered = Filter.useFilter(trainingSet, resample);
            }catch(Exception e) {
                throw new WekaException(e);
            }
            return filtered;
    }

    private static double getOversamplingPercentage(Instances instances) {
        int buggy = countBuggyInstances(instances);
        int size = instances.size();
        int notBuggy = size - buggy;
        double majority;
        double minority;


        if(buggy > notBuggy) {
            majority = buggy;
            minority = notBuggy;
        }else {
            minority = buggy;
            majority = notBuggy;
        }

        //check 0 on minority
        if(minority == 0) {
            return 0;
        }
        else
            return 200*majority/instances.size();
    }

    private static int countBuggyInstances(Instances data) {
        int counter;
        counter = 0;
        for(Instance instance: data){
            //get last attribute (Buggy)
            counter += (int)instance.value(data.numAttributes()-1) == 0 ? 1 : 0;
        }
        return counter;
    }

    private static Instances undersampling(Instances trainingSet) throws WekaException {
        SpreadSubsample spreadSubsample = new SpreadSubsample();
        Instances filtered ;
        String[] opts = new String[]{ "-M", "1.0"};

        try {
            spreadSubsample.setOptions(opts);
            spreadSubsample.setInputFormat(trainingSet);
            filtered = Filter.useFilter(trainingSet, spreadSubsample);
        }catch(Exception e ) {
            throw new WekaException(e);
        }
        return filtered;
    }

    private static Instances smote(Instances trainingSet) throws WekaException {
        SMOTE smote = new SMOTE();
        Instances filtered;

        smote.setPercentage(getSmotePercentage(trainingSet));
        try {
            smote.setInputFormat(trainingSet);
            filtered = Filter.useFilter(trainingSet, smote);
        }catch(Exception e) {
            throw new WekaException(e);
        }
        return filtered;
    }

    private static double getSmotePercentage(Instances instances) {
        int buggy;
        int size;
        int notBuggy;
        double majority;
        double minority;

        size = instances.size();
        buggy = countBuggyInstances(instances);
        notBuggy = size - buggy;

        if(buggy > notBuggy) {
            majority = buggy;
            minority = notBuggy;
        }else {
            minority = buggy;
            majority = notBuggy;
        }

        //check 0 on minority
        if(minority == 0) {
            return 0;
        }
        else
            return 100*(majority-minority)/minority;
    }

    private static Classifier generateClassifier(ClassifierType classifierType, Instances trainingSet) throws WekaException {
        Classifier classifier;

        switch(classifierType){
            case RANDOM_FOREST:
                classifier = new RandomForest();
                break;
            case NAIVE_BAYES:
                classifier = new NaiveBayes();
                break;
            case IBK:
                classifier = new IBk();
                break;
            default:
                throw new IllegalArgumentException("Invalid classifier");
        }

        try {
            classifier.buildClassifier(trainingSet);
        }catch(Exception e) {
            throw new WekaException(e);
        }

        return classifier;
    }

    private static CostSensitiveClassifier applyCostSensitive(Classifier classifier, CostSensitive costSensitive, Instances trainingSet) throws WekaException {
        CostSensitiveClassifier costClassifier;

        switch(costSensitive){
            case NO_COST_SENSITIVE:
                costClassifier = null;
                break;
            case SENSITIVE_THRESHOLD:
                costClassifier = generateCostSensitiveClassifier(classifier, trainingSet,true);
                break;
            case SENSITIVE_LEARNING:
                costClassifier = generateCostSensitiveClassifier(classifier, trainingSet,false);
                break;
            default:
                throw new IllegalArgumentException("invalid cost sensitive");
        }

        return costClassifier;
    }

    private static CostSensitiveClassifier generateCostSensitiveClassifier(Classifier classifier, Instances trainingSet, boolean threshold) throws WekaException {
        CostSensitiveClassifier costClassifier = new CostSensitiveClassifier();

        costClassifier.setClassifier(classifier);
        costClassifier.setCostMatrix(generateCostMatrix(CFP, CFN));
        costClassifier.setMinimizeExpectedCost(threshold);

        try {
            costClassifier.buildClassifier(trainingSet);
        }catch(Exception e) {
            throw new WekaException(e);
        }


        return costClassifier;
    }

    public static CostMatrix generateCostMatrix(double costFalsePositive, double costFalseNegative) {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, costFalsePositive);
        costMatrix.setCell(0, 1, costFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }

    private static void evaluateData(WekaData data, Instances trainingSet, Instances testingSet, Classifier classifier, CostSensitiveClassifier costClassifier) throws WekaException {
        Evaluation evaluation;

        double trainingData;
        double defectiveTraining;
        double defectiveTesting;

        try {
            // check if cost sensitive is applied
            if(costClassifier == null) {
                // no cost sensitive
                evaluation = new Evaluation(trainingSet);
                evaluation.evaluateModel(classifier, testingSet);
            }else {
                // cost sensitive
                evaluation = new Evaluation(testingSet, costClassifier.getCostMatrix());
                evaluation.evaluateModel(costClassifier, testingSet);
            }

        }
        catch(Exception e) {
            throw new WekaException(e);
        }

        // set training and testing data
        trainingData = (double) 100 * trainingSet.size() / (trainingSet.size() + testingSet.size());
        defectiveTraining = 100 * countBuggyInstances(trainingSet)/(double)trainingSet.size();
        defectiveTesting = 100 * countBuggyInstances(testingSet)/(double)testingSet.size();

        data.setTrainingData(trainingData);
        data.setDefectiveTraining(defectiveTraining);
        data.setDefectiveTesting(defectiveTesting);

        //set evaluation data
        data.setTruePositive((int) evaluation.numTruePositives(0));
        data.setFalsePositive((int) evaluation.numFalsePositives(0));
        data.setTrueNegative((int) evaluation.numTrueNegatives(0));
        data.setFalseNegative((int) evaluation.numFalseNegatives(0));

        data.setPrecision(evaluation.precision(0));
        data.setRecall(evaluation.recall(0));
        data.setAuc(evaluation.areaUnderROC(0));
        data.setKappa(evaluation.kappa());


    }

}
