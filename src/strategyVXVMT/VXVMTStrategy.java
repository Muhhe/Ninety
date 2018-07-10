/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

/**
 *
 * @author Muhe
 */
public class VXVMTStrategy {

    static final double[] weights = {0.45, 0.35, 0.20};

    static public VXVMTSignal CalculateSignalForDay(double[] ratioSMAs, double actRatio) {
        
        VXVMTSignal signal = new VXVMTSignal();
        double voteForSVXY = 0;
        double voteForVXX = 0;
        
        for (int i = 0; i < ratioSMAs.length; i++) {
            if (actRatio < ratioSMAs[i] && actRatio < 1) {
                voteForSVXY += weights[i];
                signal.SVXYSignals[i] = true;
            } else if (actRatio > ratioSMAs[i] && actRatio > 1) {
                voteForVXX += weights[i];
                signal.VXXSignals[i] = true;
            }
        }

        VXVMTSignal.Type selectedSignal = VXVMTSignal.Type.None;
        double targetPortion = 0;
        if (voteForVXX > voteForSVXY) {
            selectedSignal = VXVMTSignal.Type.VXX;
            targetPortion = voteForVXX;
            if (voteForVXX < 0.7) {
                selectedSignal = VXVMTSignal.Type.None;
                targetPortion = 0;
            }
        } else if (voteForVXX < voteForSVXY) {
            selectedSignal = VXVMTSignal.Type.SVXY;
            targetPortion = voteForSVXY;
        }
        
        signal.type = selectedSignal;
        signal.exposure = targetPortion;
        
        return signal;
    }

    static public VXVMTSignal CalculateFinalSignal(VXVMTData data) {

        VXVMTSignal laggedSignal = CalculateSignalForDay(data.indicators.ratiosLagged, data.indicators.actRatioLagged);
        
        if (laggedSignal.type == VXVMTSignal.Type.SVXY) {
            return laggedSignal;
        }
        
        VXVMTSignal todaysSignal = CalculateSignalForDay(data.indicators.ratios, data.indicators.actRatio);

        if ((laggedSignal.type == VXVMTSignal.Type.VXX) && (todaysSignal.type == VXVMTSignal.Type.VXX)) {
            return todaysSignal;
        } /*else if ((todaysSignal.type == VXVMTSignal.Type.None) || (todaysSignal.type == VXVMTSignal.Type.VXX)) {
            return new VXVMTSignal(1.0, VXVMTSignal.Type.GLD);
        }*/
        return new VXVMTSignal();
    }
}
