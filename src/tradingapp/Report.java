/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import communication.IBroker;
import data.CloseData;
import data.getters.DataGetterActGoogle;
import data.getters.DataGetterActIB;
import data.getters.DataGetterHistAlpha;
import data.getters.DataGetterHistGoogle;
import data.getters.IDataGetterAct;
import data.getters.IDataGetterHist;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class Report {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static class TempProfitData {

        public TempProfitData(double profit, double cash) {
            profitAbs = profit;
            investCash = cash;
            days = 1;
        }

        double GetProfitProc() {
            return profitAbs / investCash * 100;
        }

        int days = 0;
        double profitAbs = 0;
        double investCash = 0;
    }

    static class Stats {

        List<TempProfitData> tempData = new ArrayList<>();

        String GetProfitStr() {
            return TradeFormatter.toString(GetProfit());
        }

        String GetProfitProcStr() {
            return TradeFormatter.toString(GetProfitProc());
        }

        double GetProfit() {

            double profit = 0;
            for (TempProfitData data : tempData) {
                profit += data.profitAbs;
            }
            return profit;
        }

        double GetProfitProc() {

            double profit = 0;
            for (TempProfitData data : tempData) {
                profit += data.GetProfitProc();
            }
            return profit;
        }

        int GetDays() {

            int days = 0;
            for (TempProfitData data : tempData) {
                days += data.days;
            }
            return days;
        }

        void AddDay(double dayProfit, double investCash) {
            if (tempData.isEmpty() || (tempData.get(tempData.size() - 1).investCash != investCash)) {
                tempData.add(new TempProfitData(dayProfit, investCash));
                return;
            }

            tempData.get(tempData.size() - 1).profitAbs += dayProfit;
            tempData.get(tempData.size() - 1).days++;
        }
    }

    static public int GetNrOfDaysInEquity() {
        BufferedReader br = null;
        int count = 0;
        try {
            br = new BufferedReader(new FileReader(FilePaths.equityPathFile));
            br.readLine();
            while (br.readLine() != null) {
                count++;
            }
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot find report file: " + ex);
        } catch (IOException ex) {
            logger.severe("Error in generation of report: " + ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                logger.severe("Error in generation of report: " + ex);
            }
        }
        
        return count;
    }

    static public void Generate(IDataGetterHist hGetter, String refTicker, boolean reinvest) {
        BufferedReader br = null;
        BufferedWriter writer = null;
        try {
            File file = new File(FilePaths.reportPathFile);
            file.createNewFile();

            writer = new BufferedWriter(new FileWriter(file));

            br = new BufferedReader(new FileReader(FilePaths.equityPathFile));
            br.readLine(); // skip first line

            String line;
            String cvsSplitBy = ",";

            int month = -1;
            int year = -1;

            double lastDayProfit = 0;

            Stats monthStats = new Stats();
            Stats yearStats = new Stats();
            Stats totalStats = new Stats();

            line = br.readLine();
            LocalDate firstDate = LocalDate.parse(line.split(cvsSplitBy)[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            if (hGetter == null) {
                hGetter = new DataGetterHistAlpha();
            }
            
            CloseData refData = hGetter.readAdjCloseData(firstDate, TradeTimer.GetLastTradingDay(), refTicker, true);
            if (refData == null) {
                logger.warning("Report failed: cannot load hist data for - " + refTicker);
                return;
            }

            IDataGetterAct aGetter = new DataGetterActGoogle();
            refData.adjCloses[0] = aGetter.readActualData(refTicker);
            refData.dates[0] = TradeTimer.GetLastTradingDay();

            if (refData.adjCloses[0] == 0) {
                logger.warning("Report failed: cannot load act data for - " + refTicker);
                return;
            }

            if (!refData.dates[refData.dates.length - 1].equals(firstDate)) {
                logger.warning("Report failed: Dates not matching - " + refData.dates[refData.dates.length - 1].toString() + " vs " + firstDate.toString());
                return;
            }

            double investCashRef = refData.adjCloses[0];

            int indexRef = refData.dates.length - 1;

            Stats monthStatsRef = new Stats();
            Stats yearStatsRef = new Stats();
            Stats totalStatsRef = new Stats();

            double reinvestCash = 0;

            while ((line = br.readLine()) != null) {

                indexRef--;

                String[] dateLine = line.split(cvsSplitBy);

                LocalDate parsedDate = LocalDate.parse(dateLine[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                double investCash = Double.parseDouble(dateLine[2]);
                double profit = Double.parseDouble(dateLine[3]);

                if (reinvestCash == 0) {
                    reinvestCash = investCash;
                }

                if (!refData.dates[indexRef].equals(parsedDate)) {
                    logger.warning("Report failed: Dates not matching - " + refData.dates[indexRef].toString() + " vs " + parsedDate.toString());
                    return;
                }

                if (parsedDate.getMonthValue() != month) {

                    if (month > 0) {
                        String msg = "Month: " + month
                                + " | Days " + monthStats.GetDays()
                                + " | Profit " + monthStats.GetProfitStr()
                                + "$ = " + monthStats.GetProfitProcStr()
                                + "% (" + refTicker + " " + monthStatsRef.GetProfitProcStr() + "%)";

                        logger.info(msg);

                        writer.write(msg);
                        writer.write("\r\n");
                    }

                    monthStats = new Stats();
                    monthStatsRef = new Stats();

                    month = parsedDate.getMonthValue();
                }

                if (parsedDate.getYear() != year) {

                    if (year > 0) {
                        String msg = "Year: " + year + " | Days " + yearStats.GetDays()
                                + " | Profit " + yearStats.GetProfitStr()
                                + "$ = " + yearStats.GetProfitProcStr() + "% (" + refTicker + " " + yearStatsRef.GetProfitProcStr() + "%)"
                                + " | Projected profit " + TradeFormatter.toString(yearStats.GetProfitProc() * (252.0 / yearStats.GetDays())) + "%";
                        logger.info(msg);

                        writer.write(msg);
                        writer.write("\r\n");
                    }

                    yearStats = new Stats();
                    yearStatsRef = new Stats();

                    year = parsedDate.getYear();
                }

                if (reinvest) {
                    investCash = reinvestCash;
                    reinvestCash = Double.parseDouble(dateLine[1]);
                }

                monthStats.AddDay(profit - lastDayProfit, investCash);
                yearStats.AddDay(profit - lastDayProfit, investCash);
                totalStats.AddDay(profit - lastDayProfit, investCash);

                lastDayProfit = profit;

                double profitRef = refData.adjCloses[indexRef] - refData.adjCloses[indexRef + 1];

                monthStatsRef.AddDay(profitRef, investCashRef);
                yearStatsRef.AddDay(profitRef, investCashRef);
                totalStatsRef.AddDay(profitRef, investCashRef);
            }

            String msgMonth = "Month: " + month
                    + " | Days " + monthStats.GetDays()
                    + " | Profit " + monthStats.GetProfitStr()
                    + "$ = " + monthStats.GetProfitProcStr() + "% (" + refTicker + " " + monthStatsRef.GetProfitProcStr() + "%)";

            String msgYTD = "YTD: " + year + " | Days " + yearStats.GetDays()
                    + " | Profit " + yearStats.GetProfitStr()
                    + "$ = " + yearStats.GetProfitProcStr() + "% (" + refTicker + " " + yearStatsRef.GetProfitProcStr() + "%)"
                    + " | Projected profit " + TradeFormatter.toString(yearStats.GetProfitProc() * (252.0 / yearStats.GetDays())) + "%";

            String msgYear = "Total | Days " + totalStats.GetDays()
                    + " | Profit " + totalStats.GetProfitStr()
                    + "$ = " + totalStats.GetProfitProcStr() + "% (" + refTicker + " " + totalStatsRef.GetProfitProcStr() + "%)"
                    + " | Avg. yearly profit " + TradeFormatter.toString((totalStats.GetProfitProc() * (252.0 / totalStats.GetDays()))) + "%";

            logger.info(msgMonth);
            logger.info(msgYTD);
            logger.info(msgYear);

            writer.write(msgMonth);
            writer.write("\r\n");
            writer.write(msgYTD);
            writer.write("\r\n");
            writer.write(msgYear);
            writer.write("\r\n");

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot find report file: " + ex);
        } catch (IOException ex) {
            logger.severe("Error in generation of report: " + ex);
        } finally {
            try {
                br.close();
                writer.close();
            } catch (IOException ex) {
                logger.severe("Error in generation of report: " + ex);
            }
        }
    }
}
