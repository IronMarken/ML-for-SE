package logic;

public class WekaData {

    private String datasetName;
    private int trainingRelease;
    private WekaManager.ClassifierType classifier;
    private WekaManager.FeatureSelection featureSelection;
    private WekaManager.Sampling balancing;
    private WekaManager.CostSensitive sensitivity;
    //% data on training / total data
    private double trainingData;
    //% defective in training
    private double defectiveTraining;
    //% defective in testing
    private double defectiveTesting;
    private int truePositive;
    private int falsePositive;
    private int trueNegative;
    private int falseNegative;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;

    public WekaData(String datasetName, int trainingRelease, WekaManager.FeatureSelection featureSelection, WekaManager.Sampling balancing, WekaManager.ClassifierType classifier, WekaManager.CostSensitive sensitivity) {
        this.datasetName = datasetName;
        this.trainingRelease = trainingRelease;
        this.featureSelection = featureSelection;
        this.balancing = balancing;
        this.classifier = classifier;
        this.sensitivity = sensitivity;

        // init other data
        this.trainingData = 0;
        this.defectiveTraining = 0;
        this.defectiveTesting = 0;
        this.truePositive = 0;
        this.falsePositive = 0;
        this.trueNegative = 0;
        this.falseNegative = 0;
        this.precision = 0;
        this.recall = 0;
        this.auc = 0;
        this.kappa= 0;
    }

    public String getDatasetName() {
        return this.datasetName;
    }

    public int getTrainingRelease() {
        return this.trainingRelease;
    }

    public void setAuc(double auc) {
        this.auc = auc;
    }

    public double getAuc() {
        return this.auc;
    }

    public void setKappa(double kappa){
        this.kappa= kappa;
    }

    public double getKappa() {
        return this.kappa;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public int getFalseNegative() {
        return falseNegative;
    }

    public void setFalseNegative(int falseNegative) {
        this.falseNegative = falseNegative;
    }

    public int getTrueNegative() {
        return trueNegative;
    }

    public void setTrueNegative(int trueNegative) {
        this.trueNegative = trueNegative;
    }

    public int getFalsePositive() {
        return falsePositive;
    }

    public void setFalsePositive(int falsePositive) {
        this.falsePositive = falsePositive;
    }

    public int getTruePositive() {
        return truePositive;
    }

    public void setTruePositive(int truePositive) {
        this.truePositive = truePositive;
    }

    public double getDefectiveTesting() {
        return defectiveTesting;
    }

    public void setDefectiveTesting(double defectiveTesting) {
        this.defectiveTesting = defectiveTesting;
    }

    public double getDefectiveTraining() {
        return defectiveTraining;
    }

    public void setDefectiveTraining(double defectiveTraining) {
        this.defectiveTraining = defectiveTraining;
    }

    public double getTrainingData() {
        return trainingData;
    }

    public void setTrainingData(double trainingData) {
        this.trainingData = trainingData;
    }

    public String getSensitivity() {
        return sensitivity.toString();
    }

    public boolean isValid() { return this.precision != 1 || this.recall != 1 || this.auc != 1;}

    public String getBalancing() {
        return balancing.toString();
    }

    public String getFeatureSelection() {
        return featureSelection.toString();
    }

    public String getClassifier() {
        return classifier.toString();
    }
}
