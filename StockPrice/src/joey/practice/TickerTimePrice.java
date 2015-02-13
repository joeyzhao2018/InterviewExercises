package joey.practice;
import java.text.SimpleDateFormat;
import java.util.Date;
//TickerTimePrice is the main data structure used.

public class TickerTimePrice {
	private String stock="";
	private Date time=new Date();
	
	private Double[] price;
	public TickerTimePrice(String s, Date t,Double d,int length){
		stock=s;
		time=t;
		price=new Double[length];

		for(int i=0;i<length;i++){
			price[i]=d;
		}
	}
	//compare will return -2 is two data don't belong to the same stock
	//              it will return -1 if the Date t is after this.time
	public int compare(String s, Date t){
		if(s.equals(stock)){
			SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd hh:mm");			
			if(ft.format(t).equals(ft.format(time))){
				return 0;
			}
			else{	return time.compareTo(t);	}
		}
		else{return -2;//-2 means they are not the same stock at all;
		}
	}
	
	//updateOHLC is used to update OHLC results
	public void updateOHLC(Double newprice){	
		price[1]=Math.max(price[1],newprice);
		price[2]=Math.min(price[2],newprice);
		price[3]=newprice;
	}


	public Double getPrice(Integer index) {		return price[index];	}
	public int getLength(){return this.price.length;}
	public Date getTime() {		return time;}
	public String getStock() {		return stock;}
	

}
