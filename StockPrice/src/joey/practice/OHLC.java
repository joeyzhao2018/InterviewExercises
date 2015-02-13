package joey.practice;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.locks.*;
//OHLC is used to calculate Open High Low Clos prices
public class OHLC implements Runnable{
	private State state;
	private boolean ohlcFinished=false;
	public OHLC(State st){	this.state=st;	}
	
	@Override
	public void run() {
		LinkedList<TickerTimePrice> inputSource=state.getList();
		LinkedList<TickerTimePrice> ohlcList=state.getOhlcList();
		while(!ohlcFinished){
				Lock lock=state.getLock();
				lock.lock();
			 try{		
				while(inputSource.isEmpty()){
					State.getNotEmpty().await();	
				}
				TickerTimePrice readingLine=inputSource.remove();
				State.getNotFull().signal();
				if(readingLine.getStock().equals("Ending Signal")){ ohlcFinished=true;}
				else{
					Integer find=state.lookUpAndChange(readingLine.getStock(),readingLine.getTime());
					if(find!=-1){ 
						ohlcList.get(find).updateOHLC(readingLine.getPrice(0));
					}
					else{
						TickerTimePrice newTTP=new TickerTimePrice (readingLine.getStock(),readingLine.getTime(),readingLine.getPrice(0),4);
						ohlcList.add(newTTP);
					}
				}
				}	catch(InterruptedException ex){
					ex.printStackTrace();
				}finally{
					lock.unlock();
				} 
				}
			 
		
		while(!ohlcList.isEmpty()){
			//the rest of OHLC information should also be settled and feed to Stdev thread
			TickerTimePrice ttp=ohlcList.remove();
			state.FeedStdev(ttp);	}
		//Following is used to tell Stdev Thread when to stop;
		Date d= new Date();
		TickerTimePrice POISON=new TickerTimePrice("POISON",d,0.0,1);
		state.FeedStdev(POISON);
	
	System.out.println("OHLC Calculation Finished!");
	}
	}
	
	


