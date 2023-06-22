package logic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManager {

    private static final Logger LOGGER = Logger.getLogger(FileManager.class.getName());
    private static final String DATASET_DIR = "dataset";
    private static final String OUTPUT_DIR = "output";
    private static final String COMM_NAME = "-wc";
    private static final String EVAL_NAME = "-final";
    private static final String[] WEKA_COLUMNS = new String[] {"Dataset","#TrainingRelease","%Training","%Defective training", "%Defective testing", "Classifier", "Balancing", "Feature Selection", "Sensitivity", "TP", "FP", "TN", "FN", "Precision", "Recall", "AUC", "Kappa"};
    private static final String FILE_EXT = ".csv";
    private static final String[] COLUMNS = new String[] {"ReleaseNumber","JavaFile", "Size", "LOCtouched", "NR", "NAuth", "LOCadded","MAX_LOCadded", "AVG_LOCadded", "Churn", "MAX_Churn", "AVG_Churn", "ChgSetSize", "MAX_ChgSet", "AVG_ChgSet", "Age","WeightedAge", "NFix" ,"Buggy"};
    private static final String[] COLUMNS_COMM = new String[] {"ReleaseNumber","JavaFile", "Size", "CommentsPercentage" , "LOCtouched", "NR", "NAuth", "LOCadded","MAX_LOCadded", "AVG_LOCadded", "Churn", "MAX_Churn", "AVG_Churn", "ChgSetSize", "MAX_ChgSet", "AVG_ChgSet", "Age","WeightedAge", "NFix" ,"Buggy"};

    private FileManager() {}

    public static void generateDatasetDir() {
        //check if output directory exists
        File dir = new File(DATASET_DIR);
        if(!dir.isDirectory()) {
            if(dir.mkdir()) {
                LOGGER.log(Level.INFO, "Generating dataset directory");
            }else{
                LOGGER.log(Level.INFO, "Error during dataset directory generation");
            }
        }else
            LOGGER.log(Level.INFO, "Dataset directory already exists");
    }

    public static void generateOutputDir() {
        //check if output directory exists
        File dir = new File(OUTPUT_DIR);
        if(!dir.isDirectory()) {
            if(dir.mkdir()) {
                LOGGER.log(Level.INFO, "Generating output directory");
            }else{
                LOGGER.log(Level.INFO, "Error during output directory generation");
            }
        }else
            LOGGER.log(Level.INFO, "Output directory already exists");
    }

    private static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    private static String convertToCSV(String[] data) {
        return Stream.of(data).map(FileManager::escapeSpecialCharacters).collect(Collectors.joining(","));
    }

    private static void toCsv(String fullPath, List<String[]> dataLines) throws IOException {
        String report;
        File csvOutputFile = new File(fullPath);
        if(!csvOutputFile.createNewFile()) {
            report = fullPath + " already exists";
            LOGGER.log(Level.WARNING, report);
        }
        else {
            try (PrintWriter pw = new PrintWriter(csvOutputFile)){
                dataLines.stream().map(FileManager::convertToCSV).forEach(pw::println);
            }
            report = fullPath + " created";
            LOGGER.log(Level.INFO, report);
        }
    }

    public static void generateDatasetCsv(String projectName, List<JavaFile> dataList, boolean addComments) {
        List<String[]> dataToConvert = new ArrayList<>();
        String datasetFileName;

        if (addComments) {
            dataToConvert.add(COLUMNS_COMM);
            datasetFileName = DATASET_DIR + File.separator + projectName + COMM_NAME + FILE_EXT;
        }
        else {
            dataToConvert.add(COLUMNS);
            datasetFileName = DATASET_DIR + File.separator + projectName + FILE_EXT;
        }

        //file metrics
        int releaseIndex;
        String fileName;
        int size;
        float commentsPercentage;
        long touchedLOC;

        int commitCount;
        int authorCount;

        int addedLOC;
        Integer maxAdded;
        double avgAdded;

        Integer churn;
        Integer maxChurn;
        double avgChurn;

        Integer chgSetSize;
        Integer maxChgSet;
        double avgChgSet;

        long age;
        long weightedAge;

        int nFix;

        String buggy;

        for (JavaFile file : dataList) {
            //release index
            releaseIndex = file.getReleaseIndex();

            //file name
            fileName = file.getName();

            //size
            size = file.getSize();

            //comments percentage
            commentsPercentage = file.getCommentsPercentage();

            //LOC touched
            touchedLOC = file.getTouchedLOC();

            //NR
            commitCount = file.getCommitCount();

            //NAuth
            authorCount = file.getAuthorCount();

            //LOC added
            addedLOC = file.getTotalAddedLOC();

            //MAX LOC added
            maxAdded = file.getMaxAddedLOC();

            //AVG LOC added
            avgAdded = file.getAvgAddedLOC();

            //churn
            churn = file.getTotalChurn();

            //MAX churn
            maxChurn = file.getMaxChurn();

            //AVG churn
            avgChurn = file.getAvgChurn();

            //ChgSetSize
            chgSetSize = file.getTotalChgSetSize();

            //MAX chgSet
            maxChgSet = file.getMaxChgSetSize();

            //AVG chgSet
            avgChgSet = file.getAvgChgSetSize();

            //age
            age = file.getAge();

            //weighted age
            weightedAge = file.getWeightedAge();

            //n fix
            nFix = file.getNFix();

            //buggy
            if (file.isBuggy()) {
                buggy = "Yes";
            } else {
                buggy = "No";
            }

            if(addComments)
                dataToConvert.add(new String [] {Integer.toString(releaseIndex), fileName, Integer.toString(size), Float.toString(commentsPercentage), Long.toString(touchedLOC), Integer.toString(commitCount), Integer.toString(authorCount), Integer.toString(addedLOC), maxAdded.toString(), Double.toString(avgAdded), churn.toString(), maxChurn.toString(), Double.toString(avgChurn), chgSetSize.toString(), maxChgSet.toString(), Double.toString(avgChgSet), Long.toString(age), Long.toString(weightedAge), Integer.toString(nFix), buggy});
            else
                dataToConvert.add(new String [] {Integer.toString(releaseIndex), fileName, Integer.toString(size), Long.toString(touchedLOC), Integer.toString(commitCount), Integer.toString(authorCount), Integer.toString(addedLOC), maxAdded.toString(), Double.toString(avgAdded), churn.toString(), maxChurn.toString(), Double.toString(avgChurn), chgSetSize.toString(), maxChgSet.toString(), Double.toString(avgChgSet), Long.toString(age), Long.toString(weightedAge), Integer.toString(nFix), buggy});
        }

        try {
            toCsv(datasetFileName, dataToConvert);

        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean datasetExists(String projectName, boolean addComments) {
        String fullPath;
        if(addComments){
            fullPath = DATASET_DIR + File.separator + projectName + COMM_NAME + FILE_EXT;
        }else{
            fullPath = DATASET_DIR + File.separator + projectName + FILE_EXT;
        }

        File filePath = new File(fullPath);
        return filePath.exists();
    }

    public static String getDatasetPath(String projectName, boolean addComments){
        String fullPath;
        if(addComments){
            fullPath = DATASET_DIR + File.separator + projectName + COMM_NAME + FILE_EXT;
        }else{
            fullPath = DATASET_DIR + File.separator + projectName + FILE_EXT;
        }

        return fullPath;
    }

    public static void generateFinalCsv(String projectName, List<List<WekaData>> resultData, boolean addComments) {
        List<String[]> dataToPrint = new ArrayList<>();
        dataToPrint.add(WEKA_COLUMNS);

        String evaluationPath;
        if(addComments)
            evaluationPath = OUTPUT_DIR + File.separator + projectName + COMM_NAME + EVAL_NAME + FILE_EXT;
        else
            evaluationPath = OUTPUT_DIR + File.separator + projectName + EVAL_NAME + FILE_EXT;


        for(List<WekaData> stepData:resultData){
            for(WekaData data:stepData) {
                dataToPrint.add(new String[] {data.getDatasetName(), String.valueOf(data.getTrainingRelease()), String.valueOf(data.getTrainingData()), String.valueOf(data.getDefectiveTraining()), String.valueOf(data.getDefectiveTesting()), data.getClassifier(), data.getBalancing(), data.getFeatureSelection(), data.getSensitivity(), String.valueOf(data.getTruePositive()), String.valueOf(data.getFalsePositive()), String.valueOf(data.getTrueNegative()), String.valueOf(data.getFalseNegative()), String.valueOf(data.getPrecision()), String.valueOf(data.getRecall()), String.valueOf(data.getAuc()), String.valueOf(data.getKappa())});
            }
        }
        try {
            toCsv(evaluationPath, dataToPrint);

        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean evaluationExists(String projectName, boolean addComments) {
        String fullPath;
        if(addComments){
            fullPath = OUTPUT_DIR + File.separator + projectName + COMM_NAME + EVAL_NAME + FILE_EXT;
        }else{
            fullPath = OUTPUT_DIR + File.separator + projectName +  EVAL_NAME + FILE_EXT;
        }

        File filePath = new File(fullPath);
        return filePath.exists();
    }

}
