/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import data.CloseData;
import data.OHLCData;
import java.time.LocalDate;

/**
 *
 * @author Muhe
 */
public interface IDataGetterHistOHLC {
    public OHLCData readAdjCloseData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex);
    
    public OHLCData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, boolean skipFirstIndex);
    
    public OHLCData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int daysToRead, boolean skipFirstIndex);
    
    public String getName();
}
