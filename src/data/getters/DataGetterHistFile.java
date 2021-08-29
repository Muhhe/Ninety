/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import data.CloseData;
import data.OHLCData;
import data.Utils;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterHistFile implements IDataGetterHist {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final String path;
    private String pattern = "yyyy-MM-dd";
    private int closeIndex = 1;
    private boolean ascendingDates = false;

    public DataGetterHistFile(String path) {
        this.path = path;
    }

    public DataGetterHistFile(String path, String datePattern, int closeIndex, boolean ascendingDates) {
        this.path = path;
        this.pattern = datePattern;
        this.closeIndex = closeIndex;
        this.ascendingDates = ascendingDates;
    }

    @Override
    public String getName() {
        return "File loader";
    }

    @Override
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, boolean skipFirstIndex) {
        return readAdjCloseData(startDate, endDate, tickerSymbol, -1, skipFirstIndex);
    }

    @Override
    public CloseData readAdjCloseData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = (int) ((daysToRead * (7.0 / 5.0)) + 20);
        return readAdjCloseData(lastDate.minusDays(daysBackNecessary), lastDate, tickerSymbol, daysToRead, skipFirstIndex);
    }

    @Override
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = daysToRead;

        if (ascendingDates && daysToRead > 0) {
            daysBackNecessary = (int) ((daysToRead * (7.0 / 5.0)) + 20);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path + tickerSymbol + ".csv"))) {

            ArrayList<Double> arrCloseVals = new ArrayList<>();
            ArrayList<LocalDate> arrDates = new ArrayList<>();

            int totalCount = skipFirstIndex ? -1 : 0;
            String line;
            while ((line = br.readLine()) != null) {

                if (totalCount == -1) {
                    /*arrDates.add(LocalDate.MIN);
                    arrCloseVals.add(0.0);*/
                    totalCount++;
                    continue;
                }

                String[] tokens = line.split(",");

                LocalDate parsedDate;
                double adjClose;
                try {
                    adjClose = Double.parseDouble(tokens[closeIndex]);
                    parsedDate = LocalDate.parse(tokens[0], DateTimeFormatter.ofPattern(pattern));
                } catch (NumberFormatException e) {
                    continue;
                }

                if (parsedDate.compareTo(endDate) > 0) {
                    if (ascendingDates) {
                        break;
                    } else {
                        continue;
                    }
                }
                if (parsedDate.compareTo(startDate) < 0) {
                    if (!ascendingDates) {
                        break;
                    } else {
                        continue;
                    }
                }

                arrDates.add(parsedDate);
                arrCloseVals.add(adjClose);

                totalCount++;
                if (totalCount == daysBackNecessary) {
                    break;
                }
            }

            CloseData retData = new CloseData(0);

            retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();

            retData.dates = new LocalDate[arrDates.size()];
            retData.dates = arrDates.toArray(retData.dates);

            if (ascendingDates) {
                retData.adjCloses = Utils.reverseDouble(retData.adjCloses, daysToRead);
                retData.dates = Utils.reverseLocalDate(retData.dates, daysToRead);
            }

            return retData;

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load close data: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load close data: error reading file - " + ex.getMessage());
        }

        return null;
    }

    @Override
    public OHLCData readAdjOHLCData(LocalDate startDate, LocalDate endDate, String tickerSymbol, boolean skipFirstIndex) {
        return readAdjOHLCData(startDate, endDate, tickerSymbol, -1, skipFirstIndex);
    }

    @Override
    public OHLCData readAdjOHLCData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = (int) ((daysToRead * (7.0 / 5.0)) + 20);
        return readAdjOHLCData(lastDate.minusDays(daysBackNecessary), lastDate, tickerSymbol, daysToRead, skipFirstIndex);
    }

    @Override
    public OHLCData readAdjOHLCData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex) {
        int daysBackNecessary = daysToRead;

        if (ascendingDates) {
            daysBackNecessary = (int) ((daysToRead * (7.0 / 5.0)) + 20);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path + tickerSymbol + ".csv"))) {

            ArrayList<Double> arrOpenVals = new ArrayList<>();
            ArrayList<Double> arrHighVals = new ArrayList<>();
            ArrayList<Double> arrLowVals = new ArrayList<>();
            ArrayList<Double> arrCloseVals = new ArrayList<>();
            ArrayList<LocalDate> arrDates = new ArrayList<>();

            int totalCount = skipFirstIndex ? -1 : 0;
            String line;
            while ((line = br.readLine()) != null) {

                if (totalCount == -1/* && skipFirstIndex*/) {
                    /*arrDates.add(LocalDate.MIN);
                    arrOpenVals.add(0.0);
                    arrHighVals.add(0.0);
                    arrLowVals.add(0.0);
                    arrCloseVals.add(0.0);*/
                    totalCount++;
                    continue;
                }

                String[] tokens = line.split(",");

                double adjOpen;
                double adjHigh;
                double adjLow;
                double adjClose;
                LocalDate parsedDate;
                try {
                    adjOpen = Double.parseDouble(tokens[closeIndex - 3]);
                    adjHigh = Double.parseDouble(tokens[closeIndex - 2]);
                    adjLow = Double.parseDouble(tokens[closeIndex - 1]);
                    adjClose = Double.parseDouble(tokens[closeIndex]);
                    parsedDate = LocalDate.parse(tokens[0], DateTimeFormatter.ofPattern(pattern));
                } catch (NumberFormatException e) {
                    continue;
                }

                if (parsedDate.compareTo(endDate) > 0) {
                    if (ascendingDates) {
                        break;
                    } else {
                        continue;
                    }
                }
                if (parsedDate.compareTo(startDate) < 0) {
                    if (!ascendingDates) {
                        break;
                    } else {
                        continue;
                    }
                }

                arrDates.add(parsedDate);
                arrOpenVals.add(adjOpen);
                arrHighVals.add(adjHigh);
                arrLowVals.add(adjLow);
                arrCloseVals.add(adjClose);

                totalCount++;
                if (totalCount == daysBackNecessary) {
                    break;
                }
            }

            OHLCData retData = new OHLCData(0);

            retData.opens = arrOpenVals.stream().mapToDouble(Double::doubleValue).toArray();
            retData.highs = arrHighVals.stream().mapToDouble(Double::doubleValue).toArray();
            retData.lows = arrLowVals.stream().mapToDouble(Double::doubleValue).toArray();
            retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();

            retData.dates = new LocalDate[arrDates.size()];
            retData.dates = arrDates.toArray(retData.dates);

            if (ascendingDates) {
                retData.opens = Utils.reverseDouble(retData.opens, daysToRead);
                retData.highs = Utils.reverseDouble(retData.highs, daysToRead);
                retData.lows = Utils.reverseDouble(retData.lows, daysToRead);
                retData.adjCloses = Utils.reverseDouble(retData.adjCloses, daysToRead);
                retData.dates = Utils.reverseLocalDate(retData.dates, daysToRead);
            }

            return retData;

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load close data: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load close data: error reading file - " + ex.getMessage());
        }

        return null;
    }
}
