/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategy90;

import communication.BrokerIB;
import communication.IBroker;
import communication.Position;
import data.CloseData;
import data.TickersToTrade;
import static java.lang.Math.abs;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import tradingapp.Settings;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class NinetyChecker {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static boolean PerformChecks(StatusDataForNinety statusData, StockDataForNinety stockData, IBroker broker) {
        boolean isOk = true;
        
        isOk &= CheckHeldPositions(statusData, broker);
        isOk &= CheckCash(statusData, broker);
        isOk &= CheckStockData(stockData, statusData);
        
        return isOk;
    }

    public static boolean CheckHeldPositions(StatusDataForNinety statusData, IBroker broker) {
        List<Position> allPositions = broker.getAllPositions();

        int posSize = 0;
        for (Position position : allPositions) {
            if (position.pos != 0) {    // IB keeps stock with 0 position
                posSize++;
            }
        }

        logger.fine("Held postions on IB: " + posSize);
        for (Position position : allPositions) {
            if (position.pos == 0) {    // IB keeps stock with 0 position
                continue;
            }
            logger.fine("Stock: " + position.toString());

            if (!statusData.heldStocks.containsKey(position.tickerSymbol)) {
                logger.warning("Stock '" + position.tickerSymbol + "' found on IB but it's not locally saved. Position " + position.pos);
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
        
        return isOk;
    }

    public static boolean CheckCash(StatusDataForNinety statusData, IBroker broker) {
        logger.info("Saved current cash: " + TradeFormatter.toString(statusData.currentCash) + ", cash on IB: " + TradeFormatter.toString(broker.GetAccountSummary().totalCashValue));

        double cashDiff = broker.GetAccountSummary().totalCashValue - statusData.currentCash;
        double cashDiffPercent = abs(cashDiff / statusData.currentCash * 100);

        if (cashDiffPercent > 5.0) {
            logger.warning("Difference between saved cash and cash on IB is " + TradeFormatter.toString(cashDiff)
                    + "$ = " + TradeFormatter.toString(cashDiffPercent) + "%");
        } else {
            logger.info("Difference - " + TradeFormatter.toString(cashDiff) + "$ = " + TradeFormatter.toString(cashDiffPercent) + "%");
        }

        int freePortions = 20 - statusData.GetBoughtPortions();
        double availableCash = freePortions * statusData.GetOnePortionValue() / Settings.leverage;

        logger.info("Saved current available cash: " + TradeFormatter.toString(availableCash)
                + ", available funds on IB: " + TradeFormatter.toString(broker.GetAccountSummary().availableFunds));

        double availableCashDiff = broker.GetAccountSummary().availableFunds - availableCash;
        double availableCashDiffPercent = abs(availableCashDiff / availableCash * 100);

        if ((availableCashDiffPercent > 5.0) && (availableCashDiff < 0)) {
            logger.warning("Difference between saved available cash and available funds on IB is " + TradeFormatter.toString(availableCashDiff)
                    + "$ = " + TradeFormatter.toString(availableCashDiffPercent) + "%");
        } else {
            logger.info("Difference - " + TradeFormatter.toString(availableCashDiff) + "$ = " + TradeFormatter.toString(availableCashDiffPercent) + "%");
        }
        
        double buyingPowerLocal = freePortions * statusData.GetOnePortionValue();
        
        if (buyingPowerLocal > broker.GetAccountSummary().buyingPower) {
            logger.severe("Not enough buying power on IB. Local buying power: " + buyingPowerLocal + ", on IB: " + broker.GetAccountSummary().buyingPower);
            return false;
        }
        
        return true;
    }

    public static boolean CheckStockData(StockDataForNinety stockData, StatusDataForNinety statusData) {
        logger.fine("Starting history data check.");
        boolean isOk = true;

        int tickerCount = TickersToTrade.GetTickers().length;
        int histCount = stockData.closeDataMap.size();
        int indicatorCount = stockData.indicatorsMap.size();

        if (histCount != tickerCount) {
            logger.warning("Loaded hist data of only " + histCount + " out of " + tickerCount);
        }

        if (indicatorCount != tickerCount) {
            logger.warning("Indicators for only " + indicatorCount + " tickers out of " + tickerCount);
        }

        for (HeldStock held : statusData.heldStocks.values()) {
            if (!stockData.indicatorsMap.containsKey(held.tickerSymbol)) {
                logger.severe("Indicators for held stock '" + held.tickerSymbol + "' is missing!!!");
                isOk = false;
            }
        }

        for (Map.Entry<String, CloseData> entry : stockData.closeDataMap.entrySet()) {
            String ticker = entry.getKey();
            CloseData data = entry.getValue();

            isOk &= CheckTickerData(data, ticker);
        }

        if (isOk) {
            logger.fine("History data check - OK");
        } else {
            logger.warning("History data check - FAILED");
        }
        
        return isOk;
    }

    public static boolean CheckTickerData(CloseData data, String ticker) {
        if (data == null) {
            logger.warning("Failed check hist data for: " + ticker + ". Data is NULL.");
            return false;
        }
        if ((data.adjCloses.length != 200) || (data.dates.length != 200)) {
            logger.warning("Failed check hist data for: " + ticker + ". Length is not 200 but " + data.adjCloses.length);
            return false;
        }
        
        boolean isOk = true;
        LocalDate checkDate = TradeTimer.GetLocalDateNow();
        while (!TradeTimer.IsTradingDay(checkDate)) {
            checkDate = checkDate.minusDays(1);
        }
        for (LocalDate date : data.dates) {
            if (date.compareTo(checkDate) != 0) {
                logger.warning("Failed check hist data for: " + ticker + ". Date should be " + checkDate + " but is " + date);
                isOk = false;
                break;
            }

            checkDate = checkDate.minusDays(1);
            while (!TradeTimer.IsTradingDay(checkDate)) {
                checkDate = checkDate.minusDays(1);
            }
        }

        isOk &= CheckTickerAdjCloses(data, ticker);
        
        return isOk;
    }

    public static boolean CheckTickerAdjCloses(CloseData data, String ticker) {
        double lastAdjClose = 0;
        for (int i = 0; i < data.adjCloses.length; i++) {
            if (data.adjCloses[i] == 0) {
                logger.warning("Failed check hist data for: " + ticker + ". AdjClose value is 0. Date " + data.dates[i]);
                return false;
            }

            if (i < 1) {
                lastAdjClose = data.adjCloses[i];
                continue;
            }

            double diffRatio = (lastAdjClose - data.adjCloses[i]) / lastAdjClose;
            if (diffRatio > 0.3) {
                logger.warning("Failed check hist data for: " + ticker + ". AdjClose value for date " + data.dates[i] + " is " + data.adjCloses[i]
                        + ", for " + data.dates[i - 1] + " it's " + data.adjCloses[i - 1] + ". Difference " + diffRatio * 100 + "%.");
                return false;
            }
            lastAdjClose = data.adjCloses[i];
        }

        return true;
    }
}
