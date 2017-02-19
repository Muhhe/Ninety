/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.time.LocalDate;

/**
 *
 * @author Muhe
 */
public interface IDataGetterHist {
    public CloseData readAdjCloseData(LocalDate lastDate, String tickerSymbol, int daysToRead, boolean duplicateFirst);
    
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol);
    
    public CloseData readAdjCloseData(LocalDate startDate, LocalDate endDate, String tickerSymbol, int daysToRead, boolean duplicateFirst);
    
    public String getName();
}
