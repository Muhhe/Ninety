/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import communication.IBroker;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class DataGetterActIB implements IDataGetterAct {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private IBroker broker;

    public DataGetterActIB(IBroker broker) {
        this.broker = broker;
    }
    
    @Override
    public String getName() {
        return "IB";
    }
    
    @Override
    public double readActualData(String tickerSymbol) {
        if (broker == null) {
            logger.warning("Broker not set for act data getter.");
            return 0;
        }
        
        return broker.GetLastPrice(tickerSymbol);
    }
    
    @Override
    public  Map<String, Double> readActualData(String[] tickerSymbols) {
        if (broker == null) {
            logger.warning("Broker not set for act data getter.");
            return null;
        }
        
        Map<String, Double> map = new HashMap<>(tickerSymbols.length);
        for (String tickerSymbol : tickerSymbols) {
            double actData = broker.GetLastPrice(tickerSymbol);
            if (actData != 0) {
                map.put(tickerSymbol, actData);
            }
        }
        
        return map;
    }
}
