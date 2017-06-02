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
import data.getters.DataGetterHistCBOE;
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
        IDataGetterAct[] actGetters = GlobalConfig.GetDataGettersAct();
        if (actGetters.length == 0) {
            logger.severe("No Act data getter defined!");
            return null;
        }
        
        VXVMTData data = new VXVMTData();

        double actVXV = actGetters[0].readActualData("VXV");
        double actVXMT = actGetters[0].readActualData("VXMT");
        
        if (actVXV == 0 || actVXMT == 0) {
            logger.severe("Failed to load act index value!");
            return null;
        }

        boolean failedHist = false;
        for (IDataGetterHist dataGetter : GlobalConfig.GetDataGettersHist()) {

            logger.info("Loading VXV");
            data.dataVXV = dataGetter.readAdjCloseData(TradeTimer.GetLocalDateNow(), "VXV", 151, true);
            logger.info("Loading VXMT");
            data.dataVXMT = dataGetter.readAdjCloseData(TradeTimer.GetLocalDateNow(), "VXMT", 151, true);

            if (data.dataVXV == null || data.dataVXV.adjCloses == null || data.dataVXV.dates == null || data.dataVXV.adjCloses.length == 0 || data.dataVXV.dates.length == 0) {
                logger.warning("Failed to load VXV");
                failedHist = true;
                continue;
            }

            if (data.dataVXMT == null || data.dataVXMT.adjCloses == null || data.dataVXMT.dates == null || data.dataVXMT.adjCloses.length == 0 || data.dataVXMT.dates.length == 0) {
                logger.warning("Failed to load VXMT");
                failedHist = true;
                continue;
            }

            data.dataVXV.adjCloses[0] = actVXV;
            data.dataVXV.dates[0] = TradeTimer.GetLocalDateNow();

            data.dataVXMT.adjCloses[0] = actVXMT;
            data.dataVXMT.dates[0] = TradeTimer.GetLocalDateNow();

            if (!VXVMTChecker.CheckTickerData(data.dataVXV, "VXV")
                    || !VXVMTChecker.CheckTickerData(data.dataVXMT, "VXMT")) {
                failedHist = true;
                continue;
            }
            failedHist = false;
            break;
        }

        if (failedHist) {
            logger.severe("Failed to load data!");
            return null;
        }
        
        logger.info("Data loaded successfuly");
        
        return data;
    }
    
    static public void ComputeIndicators(VXVMTData data) {
        
        logger.info("Calculating indicators.");
        
        IDataGetterAct[] actGetters = GlobalConfig.GetDataGettersAct();
        if (actGetters.length == 0) {
            logger.severe("No Act data getter defined!");
            return;
        }
        
        double[] ratio = new double[151];
        LocalDate[] dates = new LocalDate[151];
        for (int i = 0; i < 151; i++) {

            ratio[i] = data.dataVXV.adjCloses[i] / data.dataVXMT.adjCloses[i];
            dates[i] = data.dataVXV.dates[i];
        }

        CloseData dataRatio = new CloseData(0);
        dataRatio.adjCloses = ratio;
        dataRatio.dates = dates;

        data.indicators = new VXVMTIndicators();

        data.indicators.actRatioLagged = ratio[1];
        data.indicators.ratiosLagged[0] = IndicatorCalculator.SMA(60, ratio, 1);
        data.indicators.ratiosLagged[1] = IndicatorCalculator.SMA(125, ratio, 1);
        data.indicators.ratiosLagged[2] = IndicatorCalculator.SMA(150, ratio, 1);

        data.indicators.actRatio = ratio[0];
        data.indicators.ratios[0] = IndicatorCalculator.SMA(60, ratio, 0);
        data.indicators.ratios[1] = IndicatorCalculator.SMA(125, ratio, 0);
        data.indicators.ratios[2] = IndicatorCalculator.SMA(150, ratio, 0);

        data.indicators.actVXXvalue = actGetters[0].readActualData("VXX");
        data.indicators.actXIVvalue = actGetters[0].readActualData("XIV");
        
        logger.fine("Indicators calculated successfuly");
    }

    static void UpdateActData(IBroker broker, VXVMTData data) {
        IDataGetterAct[] actGetters = GlobalConfig.GetDataGettersAct();
        if (actGetters.length == 0) {
            logger.severe("No Act data getter defined!");
            return;
        }        
        
        double actVXV = actGetters[0].readActualData("VXV");
        double actVXMT = actGetters[0].readActualData("VXMT");
        
        if (actVXV == 0 || actVXMT == 0) {
            logger.warning("Failed to update act index value!");
            return;
        }
        
        data.dataVXMT.adjCloses[0] = actVXMT;
        data.dataVXV.adjCloses[0] = actVXV;
    }
}
