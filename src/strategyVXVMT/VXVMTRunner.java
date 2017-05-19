/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.TradeOrder;

/**
 *
 * @author Muhe
 */
public class VXVMTRunner {
    public TradeOrder PrepareOrder(VXVMTSignal signal) {
        return new TradeOrder();
    }
    
    public void Run() {
        VXVMTIndicators indicators = VXVMTDataPreparator.LoadData();
        VXVMTSignal signal = VXVMTStrategy.CalculateFinalSignal(indicators);
        TradeOrder order = PrepareOrder(signal);
    }
}
