/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backtesting;

import static backtesting.BacktesterBear.UpdateEquityFile;
import data.CloseData;
import data.getters.DataGetterHistFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Logger;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class BacktesterIntraday {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static class TimeData {

        double high;
        double low;
    }

    static class DayData {

        LocalDate date;
        ArrayList<TimeData> times = new ArrayList<>();

        public double GetClose() {
            return (times.get(times.size() - 1).high - times.get(times.size() - 1).low) / 2;
        }
        
        public double GetCloseHigh() {
            return times.get(times.size() - 1).high;
        }
        
        public double GetCloseLow() {
            return times.get(times.size() - 1).high;
        }
    }

    static class Data {

        ArrayList<DayData> days = new ArrayList<>();
    }

    static Data LoadIntradayData() {

        int limit = 1000;
        Data data = new Data();
        try (BufferedReader br = new BufferedReader(new FileReader("CL-201803-NYMEX.scid_BarData.txt"))) {

            LocalDate currentDay = LocalDate.MIN;
            DayData dayData = new DayData();
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");

                LocalDate parsedDate = LocalDate.parse(tokens[0], DateTimeFormatter.ofPattern("yyyy/M/d"));

                if (!currentDay.equals(parsedDate)) {
                    dayData = new DayData();
                    dayData.date = parsedDate;
                    currentDay = parsedDate;
                    data.days.add(dayData);
                }

                TimeData timeData = new TimeData();
                timeData.high = Double.parseDouble(tokens[3]);
                timeData.low = Double.parseDouble(tokens[4]);

                dayData.times.add(timeData);

                /*if (limit-- < 0) {
                    break;
                }*/
            }

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load close data: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load close data: error reading file - " + ex.getMessage());
        }

        return data;
    }

    static CloseData LoadCloseData() {
        try (BufferedReader br = new BufferedReader(new FileReader("CL-201803-NYMEX.dly_BarData.txt"))) {

            ArrayList<Double> arrCloseVals = new ArrayList<>();
            ArrayList<LocalDate> arrDates = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");

                double adjClose = Double.parseDouble(tokens[1]);
                LocalDate parsedDate = LocalDate.parse(tokens[0], DateTimeFormatter.ofPattern("yyyy/M/d"));

                arrDates.add(parsedDate);
                arrCloseVals.add(adjClose);
            }

            CloseData retData = new CloseData(0);

            retData.adjCloses = arrCloseVals.stream().mapToDouble(Double::doubleValue).toArray();

            retData.dates = new LocalDate[arrDates.size()];
            retData.dates = arrDates.toArray(retData.dates);

            return retData;

        } catch (FileNotFoundException ex) {
            logger.severe("Cannot load close data: file not found - " + ex.getMessage());
        } catch (IOException ex) {
            logger.severe("Cannot load close data: error reading file - " + ex.getMessage());
        }

        return null;
    }

    static int GetTrend(int i, Data data) {

        double trend = 0;
        for (int j = 1; j < 8; j++) {
            trend += data.days.get(i - j).GetClose() - data.days.get(i - j - 1).GetClose();
        }

        if (trend > 0) {
            return 1;
        } else {
            return -1;
        }
    }

    static public void RunBacktest() {
        Data intraData = LoadIntradayData();

        /*for (DayData day : intraData.days) {
            logger.warning(day.date.toString());
            for (TimeData time : day.times) {
                logger.warning(Double.toString(time.high));
            }
        }*/

        //CloseData closeData = LoadCloseData();

        /*for (LocalDate date : closeData.dates) {
            logger.warning(date.toString());
        }*/
        
        File equityFile = new File("equity.csv");
        equityFile.delete();
        
        double stopLoss = 300;
        double profitTarget = 200;
        
        double cash = 10000;
        int position = 0;

        for (int i = 9; i < intraData.days.size(); i++) {
            int trend = GetTrend(i, intraData);
            
            double price;
            if (trend > 0) {
                price = intraData.days.get(i-1).GetCloseHigh();
                
                position = (int)(cash / price);
                
                cash -= position * price;
            } else {
                price = intraData.days.get(i-1).GetCloseLow();
                
                position = -(int)(cash / price);
                
                cash += -position * price;
            }
            
            for (int j = 0; j <  intraData.days.get(i).times.size(); j++) {
                double high = intraData.days.get(i).times.get(j).high;
                double low = intraData.days.get(i).times.get(j).low;
                
                // SL
                if (trend > 0) {
                    if ((price - low) * position > stopLoss) {
                        cash += position * low;
                        position = 0;
                    }
                } else {
                    if ((high - price) * -position > stopLoss) {
                        cash -= -position * high;
                        position = 0;
                    }
                }
                
                // PT
                if (trend > 0) {
                    if ((high - price) * position > profitTarget) {
                        cash += position * high;
                        position = 0;
                    }
                } else {
                    if ((price - low) * -position > profitTarget) {
                        cash -= -position * low;
                        position = 0;
                    }
                }
            }
            
            if (position > 0) {
                cash += position * intraData.days.get(i).GetCloseLow();
                position = 0;
            }
            
            if (position < 0) {
                cash -= -position * intraData.days.get(i).GetCloseHigh();
                position = 0;
            }
            
            
            TradeTimer.SetToday(intraData.days.get(i).date);
            
            UpdateEquityFile(cash, "equity.csv", (Integer.toString(trend)));
            
            
            logger.warning(Double.toString(cash));
        }
    }
    
    static public void UpdateEquityFile(double currentCash, String path, String addInfo) {
        Writer writer = null;
        try {
            File equityFile = new File(path);
            equityFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(equityFile, true), "UTF-8"));
            String line = TradeTimer.GetLocalDateNow().toString() + "," + currentCash;
            if (addInfo != null) {
                line += "," + addInfo;
            }
            line += "\r\n";
            writer.append(line);

            logger.fine("Updated equity file with value " + currentCash);
        } catch (FileNotFoundException ex) {
            logger.severe("Cannot find equity file: " + ex);
        } catch (IOException ex) {
            logger.severe("Error updating equity file: " + ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                logger.severe("Error updating equity file: " + ex);
            }
        }
    }

}
