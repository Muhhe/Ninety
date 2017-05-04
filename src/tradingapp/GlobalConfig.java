/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import data.getters.IDataGetterAct;
import data.getters.IDataGetterHist;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Muhe
 */
public class GlobalConfig {
    private static final List<IDataGetterAct> actGetters = new ArrayList<>();
    private static final List<IDataGetterHist> histGetters = new ArrayList<>();
    
    public static boolean sendMails = true;
    
    protected GlobalConfig() {
        // Exists only to defeat instantiation.
    }
    
    public static void ClearGetters() {
        actGetters.clear();
        histGetters.clear();
    }
    
    public static void AddDataGetterAct(IDataGetterAct getter) {
        actGetters.add(getter);
    }
    
    public static void AddDataGetterHist(IDataGetterHist getter) {
        histGetters.add(getter);
    }
    
    public static IDataGetterAct[] GetDataGettersAct() {
        IDataGetterAct[] arr = new IDataGetterAct[actGetters.size()];
        return (IDataGetterAct[]) actGetters.toArray(arr);
    }
    
    public static IDataGetterHist[] GetDataGettersHist() {
        IDataGetterHist[] arr = new IDataGetterHist[histGetters.size()];
        return (IDataGetterHist[]) histGetters.toArray(arr);
    }
}
