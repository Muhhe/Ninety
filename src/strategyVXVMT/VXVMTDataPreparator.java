/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.IBroker;
import data.CloseData;
import data.IndicatorCalculator;
import data.getters.DataGetterActIB;
import data.getters.IDataGetterAct;
import data.getters.IDataGetterHist;
import java.time.LocalDate;
import java.util.logging.Logger;
import tradingapp.GlobalConfig;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class VXVMTDataPreparator {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static public VXVMTData LoadData(IBroker broker) {
        //logger.warning("REMOVE!");
        //logger.warning("REMOVE!");
        //double actVXV = 10;//LoadActData("VIX3M");
        //double actVXMT = 11;//LoadActData("VXMT");
        double actVXV = LoadActData("VIX3M");
        double actVXMT = LoadActData("VXMT");
        
        if (actVXV == 0 || actVXMT == 0) {
            return null;
        }

        VXVMTData data = new VXVMTData();

        boolean failedHist = false;
        for (IDataGetterHist dataGetter : GlobalConfig.GetDataGettersHist()) {

            logger.info("Loading VXV");
            data.dataVXV = dataGetter.readAdjCloseData(TradeTimer.GetLocalDateNow(), "VIX3M", 151, true);
            logger.info("Loading VXMT");
            data.dataVXMT = dataGetter.readAdjCloseData(TradeTimer.GetLocalDateNow(), "VXMT", 151, true);

            if (data.dataVXV == null || data.dataVXV.adjCloses == null || data.dataVXV.dates == null || data.dataVXV.adjCloses.length == 0 || data.dataVXV.dates.length == 0) {
                logger.warning("Failed to load VXV from " + dataGetter.getName());
                failedHist = true;
                continue;
            }

            if (data.dataVXMT == null || data.dataVXMT.adjCloses == null || data.dataVXMT.dates == null || data.dataVXMT.adjCloses.length == 0 || data.dataVXMT.dates.length == 0) {
                logger.warning("Failed to load VXMT from " + dataGetter.getName());
                failedHist = true;
                continue;
            }

            data.dataVXV.adjCloses[0] = actVXV;
            data.dataVXV.dates[0] = TradeTimer.GetLocalDateNow();

            data.dataVXMT.adjCloses[0] = actVXMT;
            data.dataVXMT.dates[0] = TradeTimer.GetLocalDateNow();

            if (!VXVMTChecker.CheckTickerData(data.dataVXV, "VIX3M")
                    || !VXVMTChecker.CheckTickerData(data.dataVXMT, "VXMT")) {
                failedHist = true;
                logger.warning("Failed to load hist data from " + dataGetter.getName());
                continue;
            }

            if (failedHist) {
                logger.warning("Loading data successful from " + dataGetter.getName());
                failedHist = false;
            }

            break;
        }

        if (failedHist) {
            logger.severe("Failed to load hist data!");
            return null;
        }

        logger.info("Data loaded successfuly");

        return data;
    }
    
    static public void UpdateIndicators(IBroker broker, VXVMTData data) {
        UpdateActData(broker, data);
        ComputeIndicators(data);
    }

    static public void ComputeIndicators(VXVMTData data) {

        if (data == null) {
            return;
        }
        
        logger.info("Calculating indicators.");

        double[] ratio = new double[151];
        LocalDate[] dates = new LocalDate[151];
        for (int i = 0; i < 151; i++) {

            ratio[i] = data.dataVXV.adjCloses[i] / data.dataVXMT.adjCloses[i];
            dates[i] = data.dataVXV.dates[i];
        }

        CloseData dataRatio = new CloseData(0);
        dataRatio.adjCloses = ratio;
        dataRatio.dates = dates;

        data.indicators.actRatioLagged = ratio[1];
        data.indicators.ratiosLagged[0] = IndicatorCalculator.SMA(60, ratio, 1);
        data.indicators.ratiosLagged[1] = IndicatorCalculator.SMA(125, ratio, 1);
        data.indicators.ratiosLagged[2] = IndicatorCalculator.SMA(150, ratio, 1);

        data.indicators.actRatio = ratio[0];
        data.indicators.ratios[0] = IndicatorCalculator.SMA(60, ratio, 0);
        data.indicators.ratios[1] = IndicatorCalculator.SMA(125, ratio, 0);
        data.indicators.ratios[2] = IndicatorCalculator.SMA(150, ratio, 0);
        
        /*logger.severe("Delete this!");
        data.indicators.actRatio = 0.6;
        data.indicators.actRatioLagged = 0.9;
        data.indicators.ratiosLagged[0] = 0.7;
        data.indicators.ratiosLagged[1] = 0.7;
        data.indicators.ratiosLagged[2] = 0.7;*/

        logger.fine("Indicators calculated");
    }

    static void UpdateActData(IBroker broker, VXVMTData data) {
        
        //logger.warning("REMOVE!");
        //double actVXV = data.dataVXV.adjCloses[0];//LoadActData("VIX3M");
        //double actVXMT = data.dataVXMT.adjCloses[0];//LoadActData("VXMT");
        double actVXV = LoadActData("VIX3M");
        double actVXMT = LoadActData("VXMT");

        if (actVXV == 0 || actVXMT == 0) {
            logger.warning("Failed to update act index value!");
            return;
        }

        data.dataVXMT.adjCloses[0] = actVXMT;
        data.dataVXV.adjCloses[0] = actVXV;
        
        IDataGetterAct actGetter = new DataGetterActIB(broker);
        double actVXX = actGetter.readActualData("VXX");
        double actXIV = actGetter.readActualData("XIV");
        double actGLD = actGetter.readActualData("GLD");

        if (actVXX == 0 || actXIV == 0|| actGLD == 0) {
            logger.warning("Failed to update act XIV/VXX/GLD value!");
            return;
        }
        data.indicators.actVXXvalue = actVXX;
        data.indicators.actXIVvalue = actXIV;
        data.indicators.actGLDvalue = actGLD;
    }
    
    static double LoadActData(String ticker) {
        boolean failedAct = false;
        double dataValue = 0;
        for (IDataGetterAct dataGetter : GlobalConfig.GetDataGettersAct()) {
            dataValue = dataGetter.readActualData(ticker);

            if (dataValue == 0) {
                logger.warning("Failed to load act value of '" + ticker + "' from " + dataGetter.getName());
                failedAct = true;
                continue;
            }
            
            if (failedAct) {
                logger.warning("Loading act value of '" + ticker + "' successful from " + dataGetter.getName());
                failedAct = false;
            }

            break;
        }
        
        if (dataValue == 0) {
            logger.severe("Failed to load act value of '" + ticker + "'!");
            return 0;
        }
        
        return dataValue;
    }
}
