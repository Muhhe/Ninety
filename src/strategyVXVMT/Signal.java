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
public class Signal {
    public enum Type {
        VXX, XIV, None
    }
    
    public double exposure = 0;
    public Type type = Type.None;
}
