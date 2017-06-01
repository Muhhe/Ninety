/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import backtesting.BacktesterVXVMT;

/**
 *
 * @author Muhe
 */
public class VXVMTStrategy {

    static final double[] weights = {0.45, 0.35, 0.20};

    static private VXVMTSignal CalculateSignalForDay(double[] ratioSMAs, double actRatio) {
        
        double voteForXIV = 0;
        double voteForVXX = 0;
        
        for (int i = 0; i < ratioSMAs.length; i++) {
            if (actRatio < ratioSMAs[i] && actRatio < 1) {
                voteForXIV += weights[i];
            } else if (actRatio > ratioSMAs[i] && actRatio > 1) {
                voteForVXX += weights[i];
            }
        }

        VXVMTSignal.Type selectedSignal = VXVMTSignal.Type.None;
        double targetPortion = 0;
        if (voteForVXX > voteForXIV) {
            selectedSignal = VXVMTSignal.Type.VXX;
            targetPortion = voteForVXX;
            if (voteForVXX < 0.7) {
                selectedSignal = VXVMTSignal.Type.None;
                targetPortion = 0;
            }
        } else if (voteForVXX < voteForXIV) {
            selectedSignal = VXVMTSignal.Type.XIV;
            targetPortion = voteForXIV;
        }
        
        VXVMTSignal signal = new VXVMTSignal();
        signal.type = selectedSignal;
        signal.exposure = targetPortion;
        
        return signal;
    }

    static public VXVMTSignal CalculateFinalSignal(VXVMTData indicators) {

        VXVMTSignal laggedSignal = CalculateSignalForDay(indicators.ratiosLagged, indicators.actRatioLagged);
        
        if (laggedSignal.type != VXVMTSignal.Type.VXX) {
            return laggedSignal;
        }
        
        VXVMTSignal todaysSignal = CalculateSignalForDay(indicators.ratios, indicators.actRatio);

        if (todaysSignal.type == VXVMTSignal.Type.VXX) {
            return todaysSignal;
        }
        return new VXVMTSignal();
    }
}
