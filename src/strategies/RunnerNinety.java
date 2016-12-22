/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package strategies;

import data.CloseData;
import data.HistDataGetterYahoo;
import data.HistoricData;
import data.YahooDataGetter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import tradingapp.Timing;

/**
 *
 * @author Muhe
 */
public class RunnerNinety {
    
    private final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public Map<String, CloseData> m_closeDataMap = new HashMap<String, CloseData>(100);
    
    private Timing m_tHistData;
    private Timing m_tStartNinety;
    
    public void Start() {
        m_tHistData = new Timing(new Runnable() {
            @Override
            public void run() {
                PrepareHistData();
            }
        });
        m_tHistData.startExecutionAt(15, 50, 00);

        ZonedDateTime time = ZonedDateTime.now(ZoneId.of("America/New_York"));
        time.withHour(15).withSecond(50);
        if (time.compareTo(ZonedDateTime.now(ZoneId.of("America/New_York"))) > 0) {
            time = time.plusDays(1);
        }

        Duration timeToHist = Duration.ofSeconds(m_tHistData.computeTimeFromNowTo(15, 50, 0));

        logger.info("Scheduled");

        m_tStartNinety = new Timing(new Runnable() {
            @Override
            public void run() {
                StartNinety();
            }
        });
        m_tStartNinety.startExecutionAt(15, 59, 00);
        
        Duration timeToStart = Duration.ofSeconds(m_tHistData.computeTimeFromNowTo(15, 59, 0));
    }
    
    private void PrepareHistData() {
        HistDataGetterYahoo yahooGetter = new HistDataGetterYahoo();
        String[] tickers = getSP100();

        logger.info("Starting to load historic data");

        for (String ticker : tickers) {
            CloseData data = yahooGetter.readData(LocalDate.now(), 200, ticker);
            m_closeDataMap.put(ticker, data);
        }

        logger.info("Finished to load historic data");
    }
    
    private void StartNinety() {
        
    }
    
    String[] getSP100() {
        String[] tickers = {
            "AAPL", "ABBV", "ABT", "ACN", "AGN", "AIG", "ALL", "AMGN", "AMZN",
            "AXP", "BA", "BAC", "BIIB", "BK", "BLK", "BMY", "C", "CAT", "CELG", "CL", "CMCSA",
            "COF", "COP", "COST", "CSCO", "CVS", "CVX", "DD", "DHR", "DIS", "DOW", "DUK",
            "EMR", "EXC", "F", "FB", "FDX", "FOX", "GD", "GE",
            "GILD", "GM", "GOOG", "GS", "HAL", "HD", "HON", "IBM", "INTC",
            "JNJ", "JPM", "KMI", "KO", "LLY", "LMT", "LOW", "MA", "MCD", "MDLZ", "MDT",
            "MET", "MMM", "MO", "MON", "MRK", "MS", "MSFT", "NEE", "NKE", "ORCL", "OXY",
            "PCLN", "PEP", "PFE", "PG", "PM", "PYPL", "QCOM", "RTN", "SBUX", "SLB",
            "SO", "SPG", "T", "TGT", "TWX", "TXN", "UNH", "UNP", "UPS", "USB",
            "UTX", "V", "VZ", "WBA", "WFC", "WMT", "XOM"};

        return tickers;
    }
}
