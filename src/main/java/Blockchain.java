import java.awt.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

class Block
{
    public int number;
    public int duration;
    public long target;
    Block prevBlock = null;

    public Block() {
        target = Long.MAX_VALUE / 100;
    }

}

public class Blockchain {
    static private final Lock _mutex = new ReentrantLock(true);

    public static List log = new ArrayList<>();
    public static final BigInteger MAXIMAL_WORK_TARGET = new BigInteger("0000FFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
    public static BigInteger CURRENT = new BigInteger("0000FFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
    public static Date lastAdded = new Date();
    public static Block currentBlock = new Block();
    public static int limit = 25;
    public static DynamicChart dd = new DynamicChart();

    public static void showWindow() {
        EventQueue.invokeLater(() -> Blockchain.dd.display());
    }

    public static boolean checkWork(String hex){
        _mutex.lock();
        if(currentBlock.number == limit) {
            _mutex.unlock();
            return false; // do not allow more than 25 into it
        }

        BigInteger test = new BigInteger(hex, 16);
        if(test.compareTo(CURRENT)==-1){
            _mutex.unlock();
            return true;
        }
        _mutex.unlock();
        return false;
    }

    public static void add(){
        _mutex.lock();
        currentBlock.number++;

        if(currentBlock.number == limit)
            Logger.getGlobal().info("proof-of-work limit for current block hit, pausing");

        _mutex.unlock();
    }

    public static void generateNewBlock(){
        _mutex.lock();
        Date now = new Date();
        long seconds = (now.getTime()-lastAdded.getTime())/1000;
        currentBlock.duration = (int)seconds;
        Logger.getGlobal().info("Found new block! (height: " + log.size() + ", tx num = " + currentBlock.number + ")");

        if(log.size()>0)
            currentBlock.prevBlock = (Block) log.get(log.size()-1);
        log.add(currentBlock);

        dd.addBlock(log.size(), currentBlock.number, currentBlock.duration, currentBlock);


        currentBlock = new Block();
        lastAdded = new Date();

        retarget();

        _mutex.unlock();

    }

    /* You are only allowed to edit inbetween this comment and the comment signalizing the end.
    You furthermore are only allowed to use the information in the Block objects stored in the Blockchain array called log.
    Performance and storage must be in O(1) regardless of the length of the blockchain.
     */

    static int POW_RETARGET_DEPTH = 10;
    static int WE_WANT_X_POW_PER_MINUTE = 10;
    static int DESIRED_AVG_TRANSACTION_TIME = 60 / WE_WANT_X_POW_PER_MINUTE;
    // POW_RETARGET_STEEPNESS defines the retargeting steepness. small value = flat retargeting; big value = steep retargeting.
    // 0 < POW_RETARGET_STEEPNESS < 10  --  Values between 0.2 and 2 are recommended.
    static double POW_RETARGET_STEEPNESS = 1.1d;
    // ALLOWED_AVG_TRANSACTION_TIME_ERROR is used to skip retargeting if error is below this threshold.
    // 0 <= ALLOWED_AVG_TRANSACTION_TIME_ERROR < 10  --  Values between 0.2 and 2 are recommended.
    static double ALLOWED_AVG_TRANSACTION_TIME_ERROR = 1.1d;
    private static void retarget() {
        // here, retarget the target value!!!
    	if (log.size() > 0) {
        	Block b = (Block)log.get(log.size() - 1);
        	double lastAvgTime = (0.5d + b.duration) / (double)b.number;
        	if (Math.abs(lastAvgTime - DESIRED_AVG_TRANSACTION_TIME) < ALLOWED_AVG_TRANSACTION_TIME_ERROR) {
                Logger.getGlobal().info("error below threshold - skip retargeting");
        		return;
        	}
    	}
    	double retargetSteepness = POW_RETARGET_STEEPNESS;
        double weightedTotalTxCount = 1;
        double weightedTotalDuration = DESIRED_AVG_TRANSACTION_TIME;
        int count = Math.min(POW_RETARGET_DEPTH, (log.size() + 1) / 2);
        for(int i = count; i > 0; --i) {
        	double weight = count + 1 - i;
        	Block b = (Block)log.get(log.size() - i);
        	if (b.number == 0) {
          	    weightedTotalTxCount += weight;
                weightedTotalDuration += weight * 60d;
        	} else {
                weightedTotalTxCount += weight * b.number;
                weightedTotalDuration += weight * (0.5d + b.duration); // milliseconds are cut off, i.e., floor(seconds)
        	}
        	retargetSteepness = POW_RETARGET_STEEPNESS;
        	if (b.number == 0 || b.number == limit) {
        		retargetSteepness *= 1.8d;
        		// Avoid overweighting border cases after we come back to normal.
        		for (; i > 1; --i) {
            		Block bNext = (Block)log.get(log.size() - i + 1);
            		if (b.number != bNext.number)
            			break;
                    Logger.getGlobal().info("skipped border case with "+b.number+" transactions");
        		}
        	}
        }
        // flatten retargeting a bit
        if (retargetSteepness == POW_RETARGET_STEEPNESS && count * 3 > POW_RETARGET_DEPTH * 2)
        	retargetSteepness *=  0.6d;

        double weightedAvgTransactionTime = weightedTotalDuration / weightedTotalTxCount;
        double multiplicator = Math.pow(weightedAvgTransactionTime / DESIRED_AVG_TRANSACTION_TIME, retargetSteepness);
        BigInteger mult = BigInteger.valueOf((long)(multiplicator*10000));

        // We assume that CURRENT will never be changed outside of retarget
        // Otherwise one would need a field that stores this value - it's an easy modification
        CURRENT = CURRENT.multiply(mult).divide(BigInteger.valueOf(10000));
        if(CURRENT.compareTo(MAXIMAL_WORK_TARGET) == 1)
        	CURRENT = MAXIMAL_WORK_TARGET;
        else if(CURRENT.compareTo(BigInteger.ZERO) < 1)
        	CURRENT = BigInteger.ONE;
        Logger.getGlobal().info("multiplicator = " + multiplicator + "; weightedAvgTransactionTime = " + weightedAvgTransactionTime);
    }
 
    /* End of editing section
     */

}
