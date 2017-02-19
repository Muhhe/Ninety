/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import data.CloseData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Math.abs;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class TestUtils {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private static double GetDiff(double v1, double v2) {
        return abs(1 - (v1 / v2)) * 100.0;
    }
    
    public static final BiFunction<String, String, Double> simpleComparator = (line1, line2) -> {
        if (line1.equals(line2)) {
            return 0.0;
        } else {
            return -1.0;
        }
    };
    
    public static final BiFunction<String, String, Double> histDataComparator = (line1, line2) -> {
        String[] tokens1 = line1.split(",");
        String[] tokens2 = line2.split(",");
        
        LocalDate parsedDate1 = LocalDate.parse(tokens1[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate parsedDate2 = LocalDate.parse(tokens2[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        if (parsedDate1.compareTo(parsedDate2) != 0) {
            return -1.0;
        }
        
        return GetDiff(Double.parseDouble(tokens1[1]), Double.parseDouble(tokens2[1]));
    };
    
    public static final BiFunction<String, String, Double> indicatorTxtComparator = (line1, line2) -> {
        String[] tokens1 = line1.split(":");
        String[] tokens2 = line2.split(":");
        
        return GetDiff(Double.parseDouble(tokens1[1]), Double.parseDouble(tokens2[1]));
    };

    public static final BiFunction<String, String, Double> indicatorCsvComparator = (line1, line2) -> {
        String[] tokens1 = line1.split(",");
        String[] tokens2 = line2.split(",");

        if (simpleComparator.apply(tokens1[0], tokens2[0]) != 0.0) {
            return -1.0;
        }

        double maxDiff = 0;
        for (int i = 1; i < 5; i++) {
            if (simpleComparator.apply(tokens1[i], tokens2[i]) != 0.0) {
                double diff = GetDiff(Double.parseDouble(tokens1[i]), Double.parseDouble(tokens2[i]));
                if (diff < 0) {
                    return -1.0;
                }
                
                if (diff > maxDiff) {
                    maxDiff = diff;
                }
            }
        }

        return maxDiff;
    };

    public static boolean CompareFiles(String testRef, String testedFile, BiFunction<String, String, Double> comparator) {
        try {
            BufferedReader br1 = new BufferedReader(new FileReader(testRef));
            BufferedReader br2 = new BufferedReader(new FileReader(testedFile));

            double warningLimitDiff = 0.1;
            double failLimitDiff = 1.0;
                    
            double maxDiff = 0;
            int maxDiffLineCount = 0;
            String maxDiffLine1 = "";
            String maxDiffLine2 = "";
            
            int lineCount = 1;
            while (true) {
                String line1 = br1.readLine();
                String line2 = br2.readLine();
                
                if (line1 == null && line2 == null) {
                    break;
                }

                if ((line1 == null) || (line2 == null)) {
                    logger.warning("Compared files " + testRef + " and " + testedFile + " does not have the same size.");
                    return false;
                }

                Double diff = comparator.apply(line1, line2);
                if (diff < 0) {
                    logger.warning("Compared files " + testRef + " and " + testedFile + " are different on line " + lineCount + ": '" + line1 + "' vs '" + line2 + "'");
                    return false;
                }
                if (diff > maxDiff) {
                    maxDiff = diff;
                    maxDiffLineCount = lineCount;
                    maxDiffLine1 = line1;
                    maxDiffLine2 = line2;
                }

                /*if (!line1.equals(line2)) {
                    logger.warning("Compared files " + testRef + " and " + testedFile + " have diff on line " + lineCount + ": '" + line1 + "' vs '" + line2 + "'");
                    return -1.0;
                }*/
                lineCount++;
            }

            if (maxDiff > warningLimitDiff) {
                logger.warning("Max diff " + maxDiff + "% on files " + testRef + " and " + testedFile + " on line " + maxDiffLineCount + ": '" + maxDiffLine1 + "' vs '" + maxDiffLine2 + "'");
            }
            
            return maxDiff <= failLimitDiff;

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load close data: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load close data: error reading file - " + ex.getMessage());
        }

        return false;
    }

    public static boolean CompareDirectories(String testRef, String testedDir, BiFunction<String, String, Double> comparator) {
        File folder1 = new File(testRef);
        File folder2 = new File(testedDir);

        boolean isOk = true;
        for (final File fileEntry : folder1.listFiles()) {
            if (fileEntry.isDirectory()) {
                logger.warning("Directory in tested directory.");
            } else {
                File testedFile = new File(folder2.getPath()+ "/" + fileEntry.getName());
                if (!testedFile.exists()) {
                    logger.warning("Tested file " + testedFile.getPath() + " doesn't exist.");
                    isOk = false;
                } else {
                    isOk &= CompareFiles(fileEntry.getPath(), testedFile.getPath(), comparator);
                }
            }
        }

        return isOk;
    }
}
