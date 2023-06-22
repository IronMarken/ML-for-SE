package logic;

import org.json.JSONException;
import weka.core.WekaException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static void datasetGenerationPhase(String gitUrl, String projName) throws JSONException, IOException, InterruptedException {

        boolean commentDatasetExists;
        boolean datasetExists;

        GitBoundary gb;
        ReleaseManager rm;
        ReleaseNameAdapter rna;
        IssueManager im;
        List<JavaFile> dataList;


        commentDatasetExists = FileManager.datasetExists(projName, true);
        datasetExists = FileManager.datasetExists(projName, false);

        if(commentDatasetExists && datasetExists){
            LOGGER.log(Level.INFO, "Datasets already exist skipping generation phase" );
        }else {
            gb = new GitBoundary(gitUrl);
            rna = new ReleaseNameAdapter(0, "release-");
            rm = new ReleaseManager(projName, gb, rna);
            im = new IssueManager(projName, rm, gb);

            rm.setupReleaseManager();

            im.setupIssues();

            dataList = rm.getDataList();

            if(!commentDatasetExists) {
                LOGGER.log(Level.INFO, "Generating dataset with comment percentage");
                FileManager.generateDatasetCsv(projName, dataList, true);

            }

            if(!datasetExists) {
                LOGGER.log(Level.INFO, "Generating dataset without comment percentage");
                FileManager.generateDatasetCsv(projName, dataList, false);
            }
        }

    }

    private static void evaluationPhase(String projectName) throws WekaException {

        boolean commentEvaluationExists;
        boolean evaluationExists;

        commentEvaluationExists = FileManager.evaluationExists(projectName, true);
        evaluationExists = FileManager.evaluationExists(projectName, false);

        if(commentEvaluationExists && evaluationExists){
            LOGGER.log(Level.INFO, "Evaluation results already exist skipping generation phase" );
        }else {
            String datasetPath;
            List<List<WekaData>> returnData;
            List<WekaManager.FeatureSelection> featureSelectionList;

            List<WekaManager.ClassifierType> classifierTypeList = Arrays.asList(WekaManager.ClassifierType.RANDOM_FOREST, WekaManager.ClassifierType.NAIVE_BAYES, WekaManager.ClassifierType.IBK);
            List<WekaManager.Sampling> samplingList = Arrays.asList(WekaManager.Sampling.NO_SAMPLING, WekaManager.Sampling.OVERSAMPLING, WekaManager.Sampling.UNDERSAMPLING, WekaManager.Sampling.SMOTE);
            List<WekaManager.CostSensitive> costSensitiveList = Arrays.asList(WekaManager.CostSensitive.NO_COST_SENSITIVE, WekaManager.CostSensitive.SENSITIVE_THRESHOLD, WekaManager.CostSensitive.SENSITIVE_LEARNING);

            if(!commentEvaluationExists) {
                // with comments
                LOGGER.log(Level.INFO, "Generating evaluation results with comment percentage");

                featureSelectionList = Collections.singletonList(WekaManager.FeatureSelection.NO_FEATURE_SELECTION);
                datasetPath = FileManager.getDatasetPath(projectName, true);

                returnData = WekaManager.walkForward(projectName, datasetPath,  classifierTypeList, featureSelectionList, samplingList, costSensitiveList);

                FileManager.generateFinalCsv(projectName, returnData, true);

            }

            if(!evaluationExists) {
                // without comments
                LOGGER.log(Level.INFO, "Generating evaluation results without comment percentage");

                featureSelectionList = Arrays.asList(WekaManager.FeatureSelection.BEST_FIRST, WekaManager.FeatureSelection.NO_FEATURE_SELECTION);
                datasetPath = FileManager.getDatasetPath(projectName, false);

                returnData = WekaManager.walkForward(projectName, datasetPath,  classifierTypeList, featureSelectionList, samplingList, costSensitiveList);

                FileManager.generateFinalCsv(projectName, returnData, false);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // projects analyzed
        List<String> urlList = new ArrayList<>(Arrays.asList("https://github.com/apache/avro", "https://github.com/apache/bookkeeper"));

        // generate output directory
        FileManager.generateDatasetDir();
        FileManager.generateOutputDir();

        // parse all projects
        for (String gitUrl:urlList) {

            String[] splitted;
            String projectName;
            splitted = gitUrl.split("/");
            projectName = splitted[splitted.length -1];

            // dataset generation phase
            datasetGenerationPhase(gitUrl, projectName);

            // evaluation phase
            evaluationPhase(projectName);
        }
    }
}