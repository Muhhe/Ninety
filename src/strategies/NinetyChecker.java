/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import communication.IBBroker;
import communication.Position;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import tradingapp.TradeFormatter;
import static tradingapp.MainWindow.LOGGER_TADELOG_NAME;
import tradingapp.Settings;

/**
 *
 * @author Muhe
 */
public class NinetyChecker {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private final static Logger loggerTradeLog = Logger.getLogger(LOGGER_TADELOG_NAME);

    public static void PerformChecks(StatusDataForNinety statusData, IBBroker broker) {
        CheckHeldPositions(statusData, broker);
        CheckCash(statusData, broker);
    }
    
    public static void CheckHeldPositions(StatusDataForNinety statusData, IBBroker broker) {
        List<Position> allPositions = broker.getAllPositions();

        logger.fine("Held postions on IB: " + allPositions.size());
        for (Position position : allPositions) {
            if (position.pos == 0) {    // IB keeps stock with 0 position
                continue;
            }
            logger.fine("Stock: " + position.toString());

            if (!statusData.heldStocks.containsKey(position.tickerSymbol)) {
                logger.warning("Stock " + position.tickerSymbol + " found on IB but it's not locally saved. Position " + position.pos);
            }
        }

        boolean isOk = true;
        for (HeldStock held : statusData.heldStocks.values()) {
            boolean found = false;
            for (Position position : allPositions) {
                if (position.tickerSymbol.contentEquals(held.tickerSymbol)) {
                    found = true;

                    if (held.GetPosition() != position.pos) {
                        logger.severe("Held position mismatch for ticker: " + held.tickerSymbol + ", position on IB: " + position.pos + " vs saved: " + held.GetPosition());
                        isOk = false;
                    }
                    break;
                }
            }
            if (!found) {
                logger.severe("Held position not found on IB: " + held.tickerSymbol + ", position: " + held.GetPosition());
                isOk = false;
            }
        }
        if (isOk) {
            logger.info("Check on held position - OK");
        } else {
            logger.severe("Check on held position - FAILED");
        }
    }
    
    public static void CheckCash(StatusDataForNinety statusData, IBBroker broker) {
        logger.info("Saved current cash: " + TradeFormatter.toString(statusData.currentCash) + ", liquidation on IB: " + TradeFormatter.toString(broker.accountSummary.netLiquidation));
        
        double cashDiff = statusData.currentCash - broker.accountSummary.netLiquidation;
        double cashDiffPercent = cashDiff / statusData.currentCash * 100;
        logger.info("Difference - " + TradeFormatter.toString(cashDiff) + "$ = " + TradeFormatter.toString(cashDiffPercent) + "%");
        
        if (cashDiffPercent > 5.0) {
            logger.warning("Difference between saved cash and liquidation on IB is " + cashDiff + "$ = " + cashDiffPercent + "%");
        }
    }
}
