/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

/**
 *
 * @author Muhe
 */
public class AccountSummary {

    public double totalCashValue = 0;
    public double netLiquidation = 0;
    public double availableFunds = 0;
    public double buyingPower = 0;

    @Override
    public String toString() {
        return "IB AccountSummary - totalCashValue: " + totalCashValue + ", netLiquidation: " + netLiquidation
                + ", \r\n availableFunds: " + availableFunds + ", buyingPower: " + buyingPower;
    }
}
