package com.infairy.cocina.App.Alert.google;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

/**
 * Send string by http connection
 * @author C.Y. Chang@Infairy
 *
 */
public class HTTPSend extends Thread{
	String url;
	
	String DEBUG="HTTPSEND";
	
	boolean needReply=false;
	
	String Authen="";
	/**
	 * HTTP constructor
	 * @param url URL
	 * @param needReply 1: need response reply string to the browser
	 *                   0: just sent
	 */
	public HTTPSend(String url, String Authen, String needReply){
		this.url=url;
		if(!this.url.toLowerCase().substring(0,7).equals("http://"))
			this.url="http://"+this.url;
		this.Authen=Authen;
		if(needReply.equals("1"))
			this.needReply=true;
	}
	/**
	 * HTTP constructor
	 * @param url URL
	 * @param Authen certificate String
	 * @param needReply 1: need response reply string to the browser
	 *                   0: just sent
	 */
	public HTTPSend(String url, String needReply){
		this.url=url;
		if(!this.url.toLowerCase().substring(0,7).equals("http://"))
			this.url="http://"+this.url;
		if(needReply.equals("1"))
			this.needReply=true;
	}
	
	/**
	 * Send string to the assigned url
	 * @return repsonse string
	 */
	public String send(){
		URL web=null;
		try{
	
			web=new URL(url);
		
		}catch(MalformedURLException mue){
			//Tools.outln(DEBUG,"HttpSend Err:"+mue.getMessage());
			return"Fail:503\r\n";
		}
		
        URLConnection conn;
        try{
        	conn= web.openConnection();
        	if(Authen!=null && !Authen.equals(""))
        		conn.setRequestProperty("Authorization", "Basic " + Authen);
        }catch(IOException e){
        	return "Fail:505\r\n";
        }
        
        //conn.getContentLength();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        if(needReply){
        	/*
        	String resp="";
        	try{
        		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        		String inputLine="";
        		
				while ((inputLine = in.readLine()) != null) {
					resp+=inputLine+"@@";
					try{
						Thread.sleep(200);
					}catch(InterruptedException e){}
				}
				in.close();
        	}catch(IOException ioe){
        		//Tools.outln(DEBUG,"err.1:"+ioe.getMessage());
        		return "Fail:502, HTTP Send Error Message:\n"+ioe.getMessage()+"\r\n";
        	}
        	
        	return resp;
        	*/
        	ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
        	byte[] kk=new byte[1024];
        	try{
	        	InputStream is = conn.getInputStream();
	        	
	        	while(true){
	        		int ic = is.read(kk);
	        		if(ic==-1) break;
	        		/*
					for (int i= 0; i< ic; i++) {
						int chkIC = (int) kk[i];
						if (chkIC < 0)
							chkIC += 256;
						bStrm.write(chkIC);
					}*/
	        		bStrm.write(kk, 0, ic);
	        	}
	        	byte[] Data = bStrm.toByteArray();
	        	return new String(Data);
        	}catch(IOException ex){}
        }
        
        
        return "Success\r\n";
        
        
        
	}
	
	Vector Header=new Vector();
	
	public void setHeader(Vector header){
		Header=header;
	}
	
	
	
	
	public String post(String rawData){
		HttpURLConnection con;
		String ret="";
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);
    		if(Header.size()>0){
    			for(int i=0; i<Header.size(); i++){
    				String[] h=(String[])Header.elementAt(i);
    				if(h.length==2)
    					con.setRequestProperty(h[0], h[1]);
    			}
    		}

//            con.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
//            con.setRequestProperty("Accept","*/*");
            con.getOutputStream().write(rawData.getBytes("UTF-8"));
            
            int status = con.getResponseCode();
            
            InputStream response;
            if(status>=400)
            	response= con.getErrorStream();// .getInputStream();
            else
            	response= con.getInputStream();

            if(response!=null){
	            BufferedReader reader = new BufferedReader(new InputStreamReader(response));
	            for (String line ; (line = reader.readLine()) != null;) {
//	                System.out.println(line);
	                ret+=line+"\r\n";
	            }
	            reader.close();
            }
        } catch (FileNotFoundException e) {
            System.out.println("HTTPSend Post FileNotFoundException Error:"+e.getMessage());
        } catch (MalformedURLException e) {
        	System.out.println("HTTPSend Post MalformedURLException Error:"+e.getMessage());
        } catch (IOException e) {
        	System.out.println("HTTPSend Post IOException Error:"+e.getMessage());
        } catch (Exception e) {
        	System.out.println("HTTPSend Post Exception Error:"+e.getMessage());
        }
        return ret;
	}
}
