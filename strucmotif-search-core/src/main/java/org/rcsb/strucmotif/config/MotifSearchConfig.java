package org.rcsb.strucmotif.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties("app")
public class MotifSearchConfig {
    private double distanceCutoff = 20;
    private String rootPath = "/opt/data/";
    private String dataSource = "/opt/pdb/";
    private int numberThreads = Runtime.getRuntime().availableProcessors();
    private String dbConnectionUri;
    private int maxResults = 10000;
    private int decimalPlacesRMSD = 2;
    private int decimalPlacesMatrix = 3;
    private int chunkSize = 400;

    public double getDistanceCutoff() {
        return distanceCutoff;
    }

    public void setDistanceCutoff(double distanceCutoff) {
        this.distanceCutoff = distanceCutoff;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public int getNumberThreads() {
        return numberThreads;
    }

    public void setNumberThreads(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    public String getDbConnectionUri() {
        return dbConnectionUri;
    }

    public void setDbConnectionUri(String dbConnectionUri) {
        this.dbConnectionUri = dbConnectionUri;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getDecimalPlacesRMSD() {
        return decimalPlacesRMSD;
    }

    public void setDecimalPlacesRMSD(int decimalPlacesRMSD) {
        this.decimalPlacesRMSD = decimalPlacesRMSD;
    }

    public int getDecimalPlacesMatrix() {
        return decimalPlacesMatrix;
    }

    public void setDecimalPlacesMatrix(int decimalPlacesMatrix) {
        this.decimalPlacesMatrix = decimalPlacesMatrix;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Path getArchivePath() {
        return Paths.get(rootPath).resolve("archive");
    }

    public double getSquaredDistanceCutoff() {
        return distanceCutoff * distanceCutoff;
    }
}
