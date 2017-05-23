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
        VXX, XIV, None
    }
    
    public double exposure = 0;
    public Type type = Type.None;
    
    static VXVMTSignal.Type typeFromString(String str) {
        switch (str) {
            case "VXX":
                return Type.VXX;
            case "XIV":
                return Type.XIV;
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
            default:
                return "None";
        }
    }
}
