package joey.practice;
import java.util.concurrent.locks.Lock;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Stdev implements Runnable {
	private boolean stdFinished=false;
	private State state;
	
	//returnCalculator holds only last TickerTimePrice(with Close price) information for each stock, it is used to calculate returns
	//Once a return is calculated, it will be added to returnList for that stock. Once the list is longer than 20, a stdev will be calculated
	Map<String, LinkedList<TickerTimePrice>> returnList=new HashMap<String, LinkedList<TickerTimePrice>>();
	Map<String, TickerTimePrice> returnCalculator=new HashMap<String, TickerTimePrice>();

	public Stdev(State s){state=s;}
	
	public Double standardDeviation(LinkedList<TickerTimePrice> theReturns){
		Double total=0.0;
		for(int i = 0; i < 20; i++){total += theReturns.get(i).getPrice(0);}
		Double mean = total/20, stdev=0.0;
		for(int i = 0; i < 20; i++){ stdev+= Math.pow((theReturns.get(i).getPrice(0)-mean),2);}
		return Math.sqrt(stdev/20);
	}
	
	@Override
	public void run() {
		while(!stdFinished){
			Lock lock=state.getLock2();
			lock.lock();
			
			try{
				while(state.getStdevFeed().isEmpty()){
//					System.out.println("Nothing to consume");
					State.getConsume().await();
				}
				TickerTimePrice feed=state.getStdevFeed().remove();
				State.getFeed().signal();
				if(feed.getStock().equals("POISON")){stdFinished=true;}
				else{				
					if(returnCalculator.containsKey(feed.getStock())){
					
						Double previous= returnCalculator.get(feed.getStock()).getPrice(3);
						Double returns=(feed.getPrice(3)-previous)/previous;

						TickerTimePrice newReturn=new TickerTimePrice(feed.getStock(),feed.getTime(),returns,1);
						if(returnList.containsKey(feed.getStock())){
						
							LinkedList<TickerTimePrice> theReturns=returnList.get(feed.getStock());
							theReturns.add(newReturn);
							
						
							if(theReturns.size()==20){
								Double stdev=standardDeviation(theReturns);
								State.output(theReturns.get(19), "./STD.csv",true,stdev);
								theReturns.removeFirst();
							}				
						}
						else{
							LinkedList<TickerTimePrice> newList=new LinkedList<TickerTimePrice>();
							newList.add(newReturn);
							returnList.put(feed.getStock(),newList);
						}						
					}
						returnCalculator.put(feed.getStock(),feed);
				}
			}catch(InterruptedException ex){
				ex.printStackTrace();
			}finally{
				lock.unlock();
			}			
		}
		System.out.println("STD Calculation Finished!");
	}

}
