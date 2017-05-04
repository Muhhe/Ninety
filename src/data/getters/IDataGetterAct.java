/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.getters;

import communication.IBroker;
import java.util.Map;

/**
 *
 * @author Muhe
 */
public interface IDataGetterAct {
    
    public String getName();
    
    public double readActualData(String tickerSymbol);
    
    public  Map<String, Double> readActualData(String[] tickerSymbols);
}
