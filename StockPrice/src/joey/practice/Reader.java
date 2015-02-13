package joey.practice;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.*;
//Reader is used to load the data into State.readinglist for OHLC thread to consume

public class Reader implements Runnable{
	private State state;
	public Reader(State st){this.state=st;}
		 
	public void run() {		 
		String csvFile = "./data.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		SimpleDateFormat ft=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
		try {
			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {
		 
				String[] lineContent = line.split(cvsSplitBy);
				Date t;
				try {
					t = ft.parse(lineContent[1]);
					Double d=Double.parseDouble(lineContent[2]);
					TickerTimePrice ttp= new TickerTimePrice(lineContent[0],t,d,1);
					Lock lock=state.getLock();
					lock.lock();
					
						try{
							while(state.getList().size()==100000){
//								System.out.println("Full");
								State.getNotFull().await();							
							}
							state.getList().add(ttp);
							State.getNotEmpty().signal();
										
						}catch (InterruptedException ex){
							ex.printStackTrace();
						}finally{
							lock.unlock();
						}

				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			//The following part is used to tell OHLC okay to stop;
			//A poison feed;
			Lock lock=state.getLock();
			lock.lock();
				try{
					while(state.getList().size()==100000){
//						System.out.println("Full");
						State.getNotFull().await();				
					}
					Date d= new Date();
					TickerTimePrice EOF=new TickerTimePrice("Ending Signal",d,0.0,1);
					state.getList().add(EOF);
					State.getNotEmpty().signal();								
				}catch (InterruptedException ex){
					ex.printStackTrace();
				}finally{
					lock.unlock();
				}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		System.out.println("Reader Thread Done");
	  }
		 
	}


