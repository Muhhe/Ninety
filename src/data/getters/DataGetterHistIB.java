/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import communication.IBroker;
import data.CloseData;
import data.OHLCData;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 *
 * @author Muhe Only for last 200 adjusted close values, dates are ignored
 */
public class DataGetterHistIB implements IDataGetterHist {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private IBroker broker;

    public DataGetterHistIB(IBroker broker) {
        this.broker = broker;
    }

    @Override
    public String getName() {
        return "IB";
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
        CloseData data = broker.GetCloseData(tickerSymbol);
        if (data != null && data.adjCloses.length != 0) {
            data.adjCloses[0] = 0;
            data.dates[0] = LocalDate.MIN;
        }
        return data;
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
        OHLCData data = broker.GetOHLCData(tickerSymbol);
        if (skipFirstIndex && data != null && data.adjCloses.length != 0) {
            data.adjCloses[0] = 0;
            data.dates[0] = LocalDate.MIN;
        }
        return data;
    }

}
