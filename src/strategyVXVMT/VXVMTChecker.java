/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategyVXVMT;

import communication.IBroker;
import communication.Position;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class VXVMTChecker {

    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static boolean CheckHeldPositions(VXVMTStatus statusData, IBroker broker) {
        List<Position> allPositions = broker.getAllPositions();

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
}
