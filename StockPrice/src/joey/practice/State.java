package joey.practice;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

public class State {
	
	private LinkedList<TickerTimePrice> readinglist=new LinkedList<TickerTimePrice>() ;
	private LinkedList<TickerTimePrice> stdevFeed=new LinkedList<TickerTimePrice>();
	private LinkedList<TickerTimePrice> ohlcList=new  LinkedList<TickerTimePrice>();

// the following lock is used for writer/consumer thread communications for Reader and OHLC
	private static Lock lock=new ReentrantLock();
	private static Condition notEmpty=lock.newCondition();
	private static Condition notFull=lock.newCondition();
	
	private static final int CAP=1;
	//lock2 is used for communication between OHLC and Stdev threads
	private static Lock lock2=new ReentrantLock();
	private static Condition availableToFeed=lock2.newCondition();
	private static Condition availableToConsume=lock2.newCondition();

	//output is used to write to CSV files
	public static void output(TickerTimePrice ttp, String address, boolean isStdev, Double stdev){
		try{
			FileWriter writer=new FileWriter(address,true);
			SimpleDateFormat ft=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			writer.append(ttp.getStock());
			writer.append(", ");
			writer.append(ft.format(ttp.getTime()));
			if(isStdev){
				writer.append(", ");
				writer.append(String.format("%.6f", stdev));
			}
			else{
				for(int i=0;i<ttp.getLength();i++){
			
				writer.append(", ");
				writer.append(ttp.getPrice(i).toString());
				}
			}
			writer.append("\n");	
			writer.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	//lookUpAndChange is used by OHLC, when it takes a feed from Reader's output, and check if an entry with same stock during the same minute has exist
	// It will also decide when OHLC information is settled for a minute, and then feed to Stdev thread
	public int lookUpAndChange(String s, Date t){
		int position=-1,curr=0;
		while(curr<ohlcList.size()&&position==-1){
			if(ohlcList.get(curr).compare(s,t)==0){
				position=curr;
			}
			else if(ohlcList.get(curr).compare(s, t)==-1){
				//Since data were read in sequence, we know the Close price is settled when the time moves to the next minute
				FeedStdev(ohlcList.get(curr));
				output(ohlcList.get(curr),"./OHLC.csv",false,0.0);
				ohlcList.remove(curr);	
				curr--;
			}
			curr++;		
		}
		return position;
	}
	
	//FeedStdev takes a TickerTimePrice obj and feed it to Stdev Calculation 
	public void FeedStdev(TickerTimePrice ttp){
		lock2.lock();
		try{
			while(stdevFeed.size()==CAP){
//				System.out.print("\t\t\t\t\t\t Wait to consume");
				availableToFeed.await();
			}
			stdevFeed.offer(ttp);
			availableToConsume.signal();
		}catch(InterruptedException ex){
				ex.printStackTrace();
		}finally{
				lock2.unlock();
		}	
	}
	
	public static void main(String[] args) {

		State thisState=new State();
		Reader reader = new Reader(thisState);
		try{
			FileWriter writer1=new FileWriter("OHLC.csv",false);
			FileWriter writer2=new FileWriter("STD.csv",false);
			writer1.append("TICKER, TIME, OPEN, HIGH, LOW, CLOSE\n");
			writer2.append("TICKER, TIME, STD\n");

			writer1.close();
			writer2.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		Thread readerThread=new Thread(reader);
		OHLC ohlcThread=new OHLC(thisState);
		Thread OHLCThread=new Thread(ohlcThread);
		Stdev stdev=new Stdev(thisState);
		Thread stedevThread=new Thread(stdev);
		 readerThread.start();
		 OHLCThread.start();
		 stedevThread.start();	
		}


	public static Condition getNotFull() {		return notFull;	}
	public static Condition getNotEmpty() {		return notEmpty;}
	public static Condition getConsume() {		return availableToConsume;	}
	public static Condition getFeed() {		return availableToFeed;	}

	
	public LinkedList<TickerTimePrice> getOhlcList() {	return ohlcList;}
	public LinkedList<TickerTimePrice> getStdevFeed() {	return stdevFeed;}
	public LinkedList<TickerTimePrice> getList(){	return readinglist;}

	public Lock getLock(){		return lock;	}
	public Lock getLock2(){		return lock2;	}
	
}
