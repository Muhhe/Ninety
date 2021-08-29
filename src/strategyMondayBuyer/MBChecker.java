/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyMondayBuyer;

import communication.IBroker;
import communication.Position;
import data.OHLCData;
import static java.lang.Math.abs;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import strategy90.TickersToTrade;
import tradingapp.GlobalConfig;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class MBChecker {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static boolean PerformChecks(MBStatus statusData, MBData stockData, IBroker broker) {
        boolean isOk = true;

        isOk &= CheckHeldPositions(statusData, broker, 30);
        isOk &= CheckCash(statusData, broker);
        isOk &= CheckStockData(stockData, statusData);

        return isOk;
    }

    public static boolean CheckHeldPositions(MBStatus statusData, IBroker broker, int wait) {
        if (GlobalConfig.isBacktest) {
            return true;
        }

        List<Position> allPositions = broker.getAllPositions(wait);
        if (allPositions == null) {
            logger.warning("Cannot get positions from IB. Skipping position check.");
            return true;
        }

        int posSize = 0;
        for (Position position : allPositions) {
            if (position.pos != 0) {    // IB keeps stock with 0 position
                posSize++;
            }
        }

        logger.fine("Held positions on IB: " + posSize);
        for (Position position : allPositions) {
            if (position.pos == 0) {    // IB keeps stock with 0 position
                continue;
            }
            logger.fine("Stock: " + position.toString());

            if (!statusData.heldTickers.containsKey(position.tickerSymbol)) {
                logger.warning("Stock '" + position.tickerSymbol + "' found on IB but it's not locally saved. Position " + position.pos);
            }
        }

        boolean isOk = true;
        for (MBHeldTicker held : statusData.heldTickers.values()) {
            boolean found = false;
            for (Position position : allPositions) {
                if (position.tickerSymbol.contentEquals(held.ticker)) {
                    found = true;

                    if (held.position != position.pos) {
                        logger.severe("Held position mismatch for ticker: " + held.ticker + ", position on IB: " + position.pos + " vs saved: " + held.ticker);
                        isOk = false;
                    }
                    break;
                }
            }
            if (!found) {
                logger.severe("Held position not found on IB: " + held.ticker + ", position: " + held.position);
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

    public static boolean CheckCash(MBStatus statusData, IBroker broker) {
        if (GlobalConfig.isBacktest) {
            return true;
        }

        logger.info("Saved current equity: " + TradeFormatter.toString(statusData.equity) + ", cash on IB: " + TradeFormatter.toString(broker.GetAccountSummary().totalCashValue));

        double cashDiff = broker.GetAccountSummary().totalCashValue - statusData.equity;
        double cashDiffPercent = cashDiff / statusData.equity * 100;

        if (cashDiffPercent < -5.0) {
            logger.warning("Difference between saved cash and cash on IB is " + TradeFormatter.toString(cashDiff)
                    + "$ = " + TradeFormatter.toString(cashDiffPercent) + "%");
        } else {
            logger.info("Difference - " + TradeFormatter.toString(cashDiff) + "$ = " + TradeFormatter.toString(cashDiffPercent) + "%");
        }

        int freePortions = MBStatus.PORTIONS_NUM - statusData.heldTickers.size();
        double availableCash = freePortions * (statusData.equity / MBStatus.PORTIONS_NUM);

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

        double buyingPowerLocal = freePortions * (statusData.equity / MBStatus.PORTIONS_NUM);

        if (buyingPowerLocal > broker.GetAccountSummary().buyingPower) {
            logger.warning("Not enough buying power on IB. Local buying power: " + buyingPowerLocal + ", on IB: " + broker.GetAccountSummary().buyingPower);
            //return false;
        }

        return true;
    }

    public static boolean CheckStockData(MBData stockData, MBStatus statusData) {
        logger.fine("Starting history data check.");
        boolean isOk = true;

        int tickerCount = TickersToTrade.GetTickers().length;
        int histCount = stockData.ohlcDataMap.size();
        int indicatorCount = stockData.indicatorsMap.size();

        if (histCount != tickerCount) {
            logger.warning("Loaded hist data of only " + histCount + " out of " + tickerCount);
        }

        if (indicatorCount != tickerCount) {
            logger.warning("Indicators for only " + indicatorCount + " tickers out of " + tickerCount);
        }

        for (MBHeldTicker held : statusData.heldTickers.values()) {
            if (!stockData.indicatorsMap.containsKey(held.ticker)) {
                logger.severe("Indicators for held stock '" + held.ticker + "' is missing!!!");
                isOk = false;
            }
        }

        for (Map.Entry<String, OHLCData> entry : stockData.ohlcDataMap.entrySet()) {
            String ticker = entry.getKey();
            OHLCData data = entry.getValue();

            isOk &= CheckTickerOHLC(data, ticker);
        }

        if (isOk) {
            logger.fine("History data check - OK");
        } else {
            logger.warning("History data check - FAILED");
        }

        return isOk;
    }

    public static boolean CheckTickerData(OHLCData data, String ticker) {
        if (data == null) {
            logger.warning("Failed check hist data for: " + ticker + ". Data is NULL.");
            return false;
        }
        if ((data.adjCloses.length != MBData.DAYS_TO_LOAD) || (data.dates.length != MBData.DAYS_TO_LOAD)) {
            logger.warning("Failed check hist data for: " + ticker + ". Length is " + data.adjCloses.length);
            return false;
        }

        boolean isOk = CheckDates(data.dates, ticker);
        isOk &= CheckTickerOHLC(data, ticker);

        return isOk;
    }

    public static boolean CheckTickerOHLC(OHLCData data, String ticker) {

        if (!CheckValues(data.opens, data.dates, ticker + " opens")) {
            return false;
        }
        if (!CheckValues(data.highs, data.dates, ticker + " highs")) {
            return false;
        }
        if (!CheckValues(data.lows, data.dates, ticker + " lows")) {
            return false;
        }
        if (!CheckValues(data.adjCloses, data.dates, ticker + " adjCloses")) {
            return false;
        }

        return true;
    }

    public static boolean CheckDates(LocalDate[] dates, String ticker) {

        LocalDate checkDate = TradeTimer.GetLocalDateNow().minusDays(1);

        for (LocalDate date : dates) {
            while (!TradeTimer.IsTradingDay(checkDate)) {
                checkDate = checkDate.minusDays(1);
            }

            if (date.compareTo(checkDate) != 0) {
                logger.warning("Failed check hist data for: " + ticker + ". Date should be " + checkDate + " but is " + date);
                return false;
            }

            // backtest don't know about holidays, check only first date
            if (GlobalConfig.isBacktest) {
                return true;
            }

            checkDate = checkDate.minusDays(1);
        }
        return true;
    }

    public static boolean CheckValues(double[] values, LocalDate[] dates, String ticker) {
        if (values == null) {
            logger.warning("Failed check hist data for: " + ticker + ". Data is NULL.");
            return false;
        }
        if (values.length != MBData.DAYS_TO_LOAD) {
            logger.warning("Failed check hist data for: " + ticker + ". Length is " + values.length);
            return false;
        }

        int offset = 0;//GlobalConfig.isBacktest ? 0 : 1;

        double lastValue = 0;
        for (int i = offset; i < values.length; i++) {
            if (values[i] == 0) {
                logger.warning("Failed check hist data for: " + ticker + ". AdjClose value is 0. Date " + dates[i] + ". Index: " + i);
                return false;
            }

            if (i < 1) {
                lastValue = values[i];
                continue;
            }

            double diffRatio = (lastValue - values[i]) / lastValue;
            if (diffRatio > 0.5) {
                logger.warning("Failed check hist data for: " + ticker + ". AdjClose value for date " + dates[i] + " is " + values[i]
                        + ", for " + dates[i - 1] + " it's " + values[i - 1] + ". Difference " + diffRatio * 100 + "%.");
                return false;
            }
            lastValue = values[i];
        }

        return true;
    }
}
