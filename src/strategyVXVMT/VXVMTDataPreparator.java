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
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class VXVMTDataPreparator {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
        
    static public VXVMTIndicators LoadData(IBroker broker) {
        IDataGetterHist getter = new DataGetterHistCBOE();
        
        logger.info("Loading VXV");
        CloseData dataVXV = getter.readAdjCloseData(TradeTimer.GetLocalDateNow(), "VXV", 151, true);
        logger.info("Loading VXMT");
        CloseData dataVXMT = getter.readAdjCloseData(TradeTimer.GetLocalDateNow(), "VXMT", 151, true);

        if ((dataVXV.adjCloses.length != 151) || (dataVXMT.adjCloses.length != 151)) {
            logger.severe("Failed to read data. Lenght does not match.");
            return null;
        }
        
        IDataGetterAct actGetter = new DataGetterActIB(broker);
        
        dataVXV.adjCloses[0] = actGetter.readActualData("VXV");
        dataVXV.dates[0] = TradeTimer.GetLocalDateNow();
        
        dataVXMT.adjCloses[0] = actGetter.readActualData("VXMT");
        dataVXMT.dates[0] = TradeTimer.GetLocalDateNow();
        
        double[] ratio = new double[151];
        LocalDate[] dates = new LocalDate[151];
        for (int i = 0; i < 151; i++) {

            ratio[i] = dataVXV.adjCloses[i] / dataVXMT.adjCloses[i];
            dates[i] = dataVXV.dates[i];
        }

        CloseData dataRatio = new CloseData(0);
        dataRatio.adjCloses = ratio;
        dataRatio.dates = dates;
        
        VXVMTIndicators indicators = new VXVMTIndicators();
        
        indicators.actRatioLagged = ratio[1];
        indicators.ratiosLagged[0] = IndicatorCalculator.SMA(60, ratio, 1);
        indicators.ratiosLagged[1] = IndicatorCalculator.SMA(125, ratio, 1);
        indicators.ratiosLagged[2] = IndicatorCalculator.SMA(150, ratio, 1);
        
        indicators.actRatio = ratio[0];
        indicators.ratios[0] = IndicatorCalculator.SMA(60, ratio, 0);
        indicators.ratios[1] = IndicatorCalculator.SMA(125, ratio, 0);
        indicators.ratios[2] = IndicatorCalculator.SMA(150, ratio, 0);
        
        indicators.actVXXvalue = actGetter.readActualData("VXX");
        indicators.actXIVvalue = actGetter.readActualData("XIV");
        
        return indicators;
    }
}
