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
public class VXVMTSignal {
    public enum Type {
        VXX, XIV, GLD, None
    }
    
    public double exposure = 0;
    public Type type = Type.None;
    
    public boolean[] XIVSignals = {false, false, false};
    public boolean[] VXXSignals = {false, false, false};

    public VXVMTSignal() {
    }
    
    public VXVMTSignal(double exposure, Type type) {
        this.exposure = exposure;
        this.type = type;
    }
    
    static VXVMTSignal.Type typeFromString(String str) {
        switch (str) {
            case "VXX":
                return Type.VXX;
            case "XIV":
                return Type.XIV;
            case "GLD":
                return Type.GLD;
            default:
                return Type.None;
        }
    }
    
    public String typeToString() {
        switch (type) {
            case VXX:
                return "VXX";
            case XIV:
                return "XIV";
            case GLD:
                return "GLD";
            default:
                return "None";
        }
    }
}
