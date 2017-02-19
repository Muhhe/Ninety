/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tradingapp;

import java.time.Duration;
import java.util.logging.Logger;

/**
 *
 * @author Muhe
 */
public class CheckingThread {
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    private boolean isChecked = false;
    private String message;

    public CheckingThread(String msg) {
        message = msg;
    }
    
    public static CheckingThread StartNewCheckingThread(Duration countdown, String msg) {
        logger.finer("Checking thread with message '" + msg + "' and countdown " + countdown.toString() + " started.");
        
        CheckingThread checker = new CheckingThread(msg);
        TradeTimer.startTaskAt(TradeTimer.GetNYTimeNow().plus(countdown), checker::DoCheckOnCountdown);
        
        return checker;
    }
    
    public void SetChecked() {
        isChecked = true;
        logger.finest("Thread check with message '" + message + "' checked.");
    }
    
    private void DoCheckOnCountdown() {
        if (!isChecked) {
            logger.severe("Thread check failed: " + message);
            MailSender.SendErrors();
            return;
        }
        
        logger.finest("Thread check with message '" + message + "' succeded.");
    }
}
