/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.IBroker;
import communication.Position;
import data.CloseData;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import tradingapp.TradeFormatter;
import tradingapp.TradeTimer;

/**
 *
 * @author Muhe
 */
public class VXVMTChecker {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static boolean CheckHeldPositions(VXVMTStatus statusData, IBroker broker, int wait) {

        if (!broker.isConnected()) {
            logger.severe("Broker is not connected!");
            return false;
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

        String heldTicker = statusData.heldType.toString();

        logger.fine("Held postions on IB: " + posSize);
        for (Position position : allPositions) {
            if (position.pos == 0) {    // IB keeps stock with 0 position
                continue;
            }
            logger.fine("Stock: " + position.toString());

            if (!heldTicker.equals(position.tickerSymbol)) {
                logger.warning("Stock '" + position.tickerSymbol + "' found on IB but it's not locally saved. Position " + position.pos);
            }
        }

        boolean isOk = true;
        if (statusData.heldType != VXVMTSignal.Type.None) {

            boolean found = false;
            for (Position position : allPositions) {
                if (position.tickerSymbol.equals(heldTicker)) {
                    found = true;

                    if (statusData.heldPosition != position.pos) {
                        logger.severe("Held position mismatch for ticker: " + heldTicker + ", position on IB: " + position.pos + " vs saved: " + statusData.heldPosition);
                        isOk = false;
                    }
                    break;
                }
            }
            if (!found) {
                logger.severe("Held position not found on IB: " + heldTicker + ", position: " + statusData.heldPosition);
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

    public static boolean CheckNumber(double num) {
        if (num == 0 || Double.isNaN(num)) {
            return false;
        }
        return true;
    }

    public static boolean CheckDataIndicators(VXVMTData data) {

        if (data == null) {
            logger.severe("Data is null!");
            return false;
        }

        if (data.indicators == null) {
            logger.severe("Indicators are null!");
            return false;
        }

        if (!CheckNumber(data.indicators.actRatio)
                || !CheckNumber(data.indicators.actRatioLagged)
                || !CheckNumber(data.indicators.actVXXvalue)
                || !CheckNumber(data.indicators.actXIVvalue)
                || !CheckNumber(data.indicators.actGLDvalue)) {
            logger.severe("Some data is not valid!");
            return false;

        }

        if ((data.indicators.ratios == null)
                || (data.indicators.ratiosLagged == null)) {
            logger.severe("Some ratio is NULL!");
            return false;
        }

        if (!CheckNumber(data.indicators.ratios[0])
                || !CheckNumber(data.indicators.ratios[1])
                || !CheckNumber(data.indicators.ratios[2])) {
            logger.severe("Some ratio data is not valid!");
            return false;
        }

        if (!CheckNumber(data.indicators.ratiosLagged[0])
                || !CheckNumber(data.indicators.ratiosLagged[1])
                || !CheckNumber(data.indicators.ratiosLagged[2])) {
            logger.severe("Some ratiosLagged data is not valid!");
            return false;
        }

        logger.fine("Indicators check - OK");

        return true;
    }

    public static boolean CheckTickerData(CloseData data, String ticker) {
        if (data == null) {
            logger.warning("Failed check hist data for: " + ticker + ". Data is NULL.");
            return false;
        }
        if ((data.adjCloses.length != 151) || (data.dates.length != 151)) {
            logger.warning("Failed check hist data for: " + ticker + ". Length is not 151 but " + data.adjCloses.length);
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

        for (int i = 0; i < data.adjCloses.length; i++) {
            if (data.adjCloses[i] == 0) {
                logger.warning("Failed check hist data for: " + ticker + ". AdjClose value is 0. Date " + data.dates[i]);
                isOk = false;
                break;
            }
        }

        return isOk;
    }

    public static boolean CheckCash(VXVMTStatus statusData, IBroker broker) {

        broker.RequestAccountSummary();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }

        if (statusData.freeCapital < 0.0) {
            logger.warning("Saved cash is negative.");
        }

        logger.info("Saved free cash: " + TradeFormatter.toString(statusData.freeCapital) + ", cash on IB: " + TradeFormatter.toString(broker.GetAccountSummary().totalCashValue));

        double cashDiff = broker.GetAccountSummary().totalCashValue - statusData.freeCapital;
        double cashDiffPercent = (cashDiff / statusData.freeCapital * 100);

        if (cashDiffPercent < 0.0) {
            logger.warning("Free cash on IB is lower than saved cash. Difference: " + TradeFormatter.toString(cashDiff)
                    + "$ = " + TradeFormatter.toString(cashDiffPercent) + "%");
        } else {
            logger.fine("Difference - " + TradeFormatter.toString(cashDiff) + "$ = " + TradeFormatter.toString(cashDiffPercent) + "%");
        }

        logger.fine("Cash check - OK");

        return true;
    }
}
