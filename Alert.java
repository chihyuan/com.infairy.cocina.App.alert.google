package com.infairy.cocina.App.Alert.google;



import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.infairy.cocina.SDK.Layout.Layout;
import com.infairy.cocina.SDK.Layout.LayoutTag;
import com.infairy.cocina.SDK.device.DevicePool;
import com.infairy.cocina.SDK.gene.EventTTSDNA;
import com.infairy.cocina.SDK.property.Property;
import com.infairy.smarthome.tools.DBTools;
import com.infairy.smarthome.tools.Tools;



public class Alert implements BundleActivator, Runnable,EventHandler{
	DevicePool device=null;
	Property property;

	Dictionary dict = new Hashtable();

	BundleContext context;

	String ZID="";

	
	boolean monitor=false;
	
	String lat="",lng="";

	Layout layout;
	public void start(BundleContext context) throws Exception {
		this.context=context;
		property=(Property)Tools.getService(context, Property.class.getName(), "(Property=Setting)");
		device=(DevicePool)Tools.getService(context, DevicePool.class.getName(), property.getDeviceService());
		layout=(Layout)Tools.getService(context, Layout.class.getName(), "(FUNCTION=LAYOUT)");
		
		Dictionary props = new Hashtable();
		props.put("Bundle-Alias", LanguagePack("Google示警"));
		//props.put("Bundle-Function", "GoogleAlert");
		boolean TF=device.registerBundle(context, this, props);

		/* Set registered fucntions */
		Vector functions=new Vector();
		functions.addElement(new Object[]{LanguagePack("启动监控"), "startMonitor", null});
		functions.addElement(new Object[]{LanguagePack("取消监控"),"stopMonitor", null});
		functions.addElement(new Object[]{LanguagePack("即时预报"),"realTime", null});
		dict.put("Class", this.getClass()); //register device class Object
		dict.put("Alias",LanguagePack("Google示警")); //register device alias name which will show on all UI.
		dict.put("GKIND", "GoogleAlert"); //Define the device's global kind for device's used.
		dict.put("Function", functions); //register public function and for the other device called.

		Vector Operation=new Vector();
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_0, "startMonitor"});
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_1, "stopMonitor"});
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_2, "realTime"});
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_10, "Content0"});
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_11, "Content1"});
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_12, "Content2"});
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_13, "Content3"});
		Operation.addElement(new Object[]{device.DEVICE_OPERATION_USER_DEFINED_14, "Content4"});
		
		dict.put("Operation", Operation);

		
		dict.put("Layout", "Bundle"); //設定模板檔案

		/*
        add the device into Infairy@Android family.
        */
		ZID=device.addDevice(dict);
		
		//context.getBundle().getSymbolicName()
//		System.out.println("ADD Goolg Alert ZID="+ZID);

		device.FunctionVisible(ZID, LanguagePack("取消监控"), false);
		Thread me=new Thread(this);
		me.start();
		/*
		 * Listen Voice Recognition
		 * 
		 */
		device.ListenBroadcast(context, this, device.BROADCAST_CHANNEL_TTS);
		
		lat=property.getLatitude();
		lng=property.getLongitude();

//		System.out.println("Google alert my position lat="+lat+", Lng="+lng);
		
		String isMonitor=DBTools.getDB(this,"GoogleAlert");
		if(isMonitor.trim().equals("Y"))
				startMonitor();
			
		MakeLayout(isMonitor.trim());
		
		myPosition();
		
	}

	private void saveAlert(){
		DBTools.setDB(this,"GoogleAlert", "N", "Y");
	}
	
	private void delAlert(){
		DBTools.rmDB(this,"GoogleAlert");
	}
	
	public void startMonitor(){
		monitor=true;
		saveAlert(); //紀錄是否監控警示
		device.FunctionVisible(ZID, LanguagePack("启动监控"), false);
		device.FunctionVisible(ZID, LanguagePack("取消监控"), true);
		Tools.addTTS(LanguagePack("我是谷歌示警系统，我将开始帮您监控你所在区域的警报"));
		
		layout.removeElement(this, ZID, monitorID);
		monitorID=layout.createImage(this, LayoutID, device.DEVICE_OPERATION_USER_DEFINED_1, "", 101, 101, 52, 342, "","","", "icon/Disable.png","#ffffff",22,"",0,"");
		layout.update(ZID,LayoutID,monitorID);
	}
	
	public void stopMonitor(){
		monitor=false;
		delAlert(); //移除監控警示
		device.FunctionVisible(ZID, LanguagePack("启动监控"), true);
		device.FunctionVisible(ZID, LanguagePack("取消监控"), false);
		Tools.addTTS(LanguagePack("我是谷歌示警系统，取消监控你所在区域的警报"));
		noLatLngWarning=false;
		layout.removeElement(this, ZID, monitorID);
		monitorID=layout.createImage(this, LayoutID, device.DEVICE_OPERATION_USER_DEFINED_0, "", 101, 101, 52, 342, "","","", "icon/Enable.png","#ffffff",22,"",0,"");
		layout.update(ZID,LayoutID,monitorID);
	}
	
	public void realTime(){
		//if(lat.equals(""))
			lat=property.getLatitude();
		//if(lng.equals(""))
			lng=property.getLongitude();

		if(lat.equals("") || lng.equals("")){
			Tools.addTTS(LanguagePack("我是谷歌示警系统，找不到您的卫星座标，所以，直接以台湾全区做搜寻"));
			getAlert("21.9432", "119.228255", "25.381255" ,"122.029183", true);
		}else{
			double LAT=Double.parseDouble(lat);
			double LNG=Double.parseDouble(lng);
			double[] axis=LatLngUtil.getAround(LNG, LAT, 30000);  //計算30公里半徑的座標
			getAlert(String.valueOf(axis[1]), String.valueOf(axis[0]), String.valueOf(axis[3]) ,String.valueOf(axis[2]), true);
		}
	}
	
	boolean noLatLngWarning=false;
	public void Active(){
		lat=property.getLatitude();
		lng=property.getLongitude();

		if(lat.equals("") || lng.equals("")){
			if(!noLatLngWarning){
				Tools.addTTS(LanguagePack("我是谷歌示警系统，找不到您的卫星座标，所以，直接以台湾全区做搜寻"));
				noLatLngWarning=true;
			}
			getAlert("21.9432", "119.228255", "25.381255" ,"122.029183", false);
		}else{
			double LAT=Double.parseDouble(lat);
			double LNG=Double.parseDouble(lng);
			double[] axis=LatLngUtil.getAround(LNG, LAT, 35000);  //計算35公里半徑的座標
			getAlert(String.valueOf(axis[1]), String.valueOf(axis[0]), String.valueOf(axis[3]) ,String.valueOf(axis[2]), false);
		}
		//getAlert("18.887105046357703", "109.48440747656252", "28.083172277531034","129.25979810156252");
		//getAlert("14.07429248513469","99.59671216406252", "32.41850014709136","139.14749341406252");
		
	}


	private void getAlert(String lat1, String lng1, String lat2, String lng2, boolean isRealtime){
		String data="{\"method\":\"search\",\"params\":[,[,[,"+lat1+","+lng1+"] ,[,"+lat2+","+lng2+"] ]     ,11,\"zh\",,\"is:active event_type:met\",,,,0,\"TW\",,,0] }";
		String url="http://google.org/publicalerts/g/search";
//System.out.println("Google Alert Check Area:\n"+lat1+","+lng1+" - "+lat2+","+lng2+"\n****************");					
		String ret=parseJSON(url, data, isRealtime);
//System.out.println("Google Alert response:\n"+ret+"\n********************");
//		if(ret.trim().equals("")){
//			ret="aa\nbb\ncc\ndd\nee\r\n";
//			ret+="aa\nbb\ncc\ndd\nee\r\n";
//		}
		
		if(!ret.trim().equals("")){
			String chk[]=Tools.split(ret.trim(), "\r\n");
			//String speak="我是Google示警系統,您所在的區域目前共有"+chk.length+"項警報,";
			String speak=LanguagePack("我是谷歌示警系统，您所在的区域目前共有XX项警报")+",";
			speak=Tools.replace(speak, "XX", String.valueOf(chk.length));
//			clearAllMessage();  //先清除所有訊息
			
			moveMessage(chk.length);
			
			String nowDate=Tools.getSystemTime(property.TimeZoneID, "yyyy-MM-dd");
			String nowTime=Tools.getSystemTime(property.TimeZoneID, "HH:mm:ss");
			for(int i=0; i<chk.length; i++){
				//speak+="第"+(i+1)+"項,";
				speak+=Tools.replace(LanguagePack("第XX项"), "XX", String.valueOf((i+1)));
				//警報類型, 地區, 內容, 發佈者, 更新時間
				String aler[]=Tools.split(chk[i],"\n");
				//speak+=aler[3]+"於"+aler[4]+"發佈"+aler[0];
				
				String who=Tools.replace(LanguagePack("XX于YY发布ZZ"), "XX", aler[3]);
				who=Tools.replace(who, "YY", aler[4]);
				who=Tools.replace(who, "ZZ", aler[0]);
				speak+=who+",";
				speak+=LanguagePack("影响区域")+","+aler[1]+",";
				speak+=LanguagePack("内容为")+":"+aler[2]+",";
				
				
				if(i<5){
					layout.editLayout(this, ZID, PubDate[i], device.LAYOUT_ITEM_TEXT, aler[4]);
					layout.editLayout(this, ZID, Date[i], device.LAYOUT_ITEM_TEXT, nowDate);
					layout.editLayout(this, ZID, Time[i], device.LAYOUT_ITEM_TEXT, nowTime);
					layout.editLayout(this, ZID, Content[i], device.LAYOUT_ITEM_TEXT, aler[0]+" | "+LanguagePack("影响区域")+":"+aler[1]+" | "+aler[2]);
				}
			}
		
			layout.update(ZID, "", "");
			if(speak.indexOf("地震")!=-1)
				Tools.addAlarmTTS(speak);
			else
				Tools.addTTS(speak);
		}else{
			if(isRealtime){
				Tools.addTTS(LanguagePack("我是谷歌示警系统，您所在的区域目前没有任何警示预报"));
//				clearAllMessage();
				moveMessage(1);
				
				String nowDate=Tools.getSystemTime(property.TimeZoneID, "yyyy-MM-dd");
				String nowTime=Tools.getSystemTime(property.TimeZoneID, "HH:mm:ss");
				layout.editLayout(this, ZID, Date[0], device.LAYOUT_ITEM_TEXT, nowDate);
				layout.editLayout(this, ZID, Time[0], device.LAYOUT_ITEM_TEXT, nowTime);
				layout.editLayout(this, ZID, Content[0], device.LAYOUT_ITEM_TEXT, "目前沒有任何警報訊息");

				layout.update(ZID, "", "");
			}
		}
	}


	
	Vector ALERT=new Vector();
	private String parseJSON(String url, String rawData, boolean isRealtime) {
		String RET="";


		HTTPSend send=new HTTPSend(url, "1");
		Vector header=new Vector();
		header.add(new String[]{"Content-Type","application/javascript; charset=utf-8"});
		header.add(new String[]{"Host","google.org"});
		header.add(new String[]{"Referer","http://google.org/publicalerts"});
		header.add(new String[]{"X-GWT-Module-Base","http://google.org/publicalerts/"});
		header.add(new String[]{"X-GWT-Permutation","12093DB6A996099D9CF491F97AC35216"});
		header.add(new String[]{"User-Agent","Mozilla/5.0 ( compatible )"});
		header.add(new String[]{"Accept","*/*"});
		header.add(new String[]{"Accept-Encoding","gzip, deflate,sdch"});
		header.add(new String[]{"Accept-Language","zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-CN;q=0.2"});
		header.add(new String[]{"connection","keep-alive"});
		
		
//		header.add(new String[]{"DNT","1"});
		send.setHeader(header);
		String ret=send.post(rawData);
		
//System.out.println("ret>"+ret);			
		send=null;
		
		
		if(ret==null || ret.trim().equals("")){
			return "";
		}else{

			String chk=getJSONField(ret, "result",0);
//			System.out.println("chk[0]="+chk);
			
			try{
				
				JSONArray ja = new JSONArray(chk);
				
//				System.out.println("L="+ja.length());
				for(int i=0; i<ja.length(); i++){
					
					if(i==4){
						JSONArray ja4=ja.getJSONArray(4);
						//System.out.println("*******************共有"+ja4.length()+"警報**********");
						if(ja4.length()==0){
							ALERT.removeAllElements();
						}else{
							for(int j=0; j<ja4.length(); j++){
								JSONArray ja41=ja4.getJSONArray(j);
								/*System.out.println("\t\t第"+(j+1)+"個警報:");
								System.out.println("\t\t"+ja41.getString(13));
								System.out.println("\t\t發佈者:"+ja41.getString(9));
								System.out.println("\t\t警報類型:"+ja41.getString(3));
								System.out.println("\t\t地區:"+ja41.getString(5));
								System.out.println("\t\t內容:"+ja41.getJSONArray(11).get(0));
								System.out.println("\t\n******************************");
								*/
								//警報類型, 地區, 內容, 發佈者, 更新時間
								
								boolean tf=addAlert(ja41.getString(3),ja41.getString(5),ja41.getJSONArray(11).get(0)+"",ja41.getString(9),ja41.getString(13));
								if(tf || isRealtime){
								
									String TM=ja41.getString(13);  //更新時間
									if(TM.indexOf("更新時間：")==0)
										TM=TM.substring("更新時間：".length());
									RET+=ja41.getString(3)+"\n"+ja41.getString(5)+"\n"+ja41.getJSONArray(11).get(0)+"\n"+ja41.getString(9)+"\n"+TM+"\r\n";
									
								}
							}
						}
					}
				}
				
			}catch(JSONException je){
				RET="";
			}
		}
			
		return RET;
	}

	private String parseJSON(String url, String reply) {
		String ret="";
			HTTPSend send=new HTTPSend(url, "1");
			ret=send.send();
			send=null;
		if(ret.trim().equals("")){
			return "";
		}else{

			int n=reply.indexOf("@");
			int n1=reply.indexOf("@",n+1);
			String cmd=reply.substring(n+1,n1);
			
			//檢查=string()
			int idx=cmd.indexOf("=string(");
			String stringCondition="";
			if(idx!=-1){
				int idx1=cmd.indexOf(")", idx+1);
				if(idx1!=-1){
					stringCondition=cmd.substring(idx+"=string(".length(), idx1);
					cmd=cmd.substring(0,idx)+cmd.substring(idx1+1);
				}
			}
//System.out.println(stringCondition);
//System.out.println(cmd);
			String cmds[]=Tools.split(cmd, ",");

			String chk=getJSONField(ret, cmds[0],0);

			String RET="";
			if(chk.equals("")) RET="";
			else{				
				try{
					chk=chk.substring(0,1).equals("[")?chk:"["+chk+"]";
					JSONArray jsonArray = new JSONArray(chk);

					String condition=cmds[1];
					if(!stringCondition.equals(""))
						condition+="=="+stringCondition;
					
					String retField=cmds[2];
					String conditionField="";
					String conditionValue="";
					String cond[]=new String[2];
					cond[0]="";
					cond[1]="";
					if(condition.indexOf("==")!=-1)
						cond=Tools.split(condition, "==");
					else if(condition.indexOf(">=")!=-1)
						cond=Tools.split(condition, ">=");
					else if(condition.indexOf("<=")!=-1)
						cond=Tools.split(condition, "<=");
					else if(condition.indexOf("<")!=-1)
						cond=Tools.split(condition, "<");
					else if(condition.indexOf(">")!=-1)
						cond=Tools.split(condition, ">");
					else if(condition.indexOf("!=")!=-1)
						cond=Tools.split(condition, "!=");
					conditionField=cond[0];
					conditionValue=cond[1];


					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject jsonObject = jsonArray.getJSONObject(i);

						if(!conditionField.equals("")){
							
							if(condition.indexOf("==")!=-1){
								String conval=jsonObject.get(conditionField).toString();
//System.out.println(conditionField+" > "+conval+ " = "+conditionValue);
								if(conval.equals(conditionValue)){
									if(retField.indexOf(".")==-1){
										RET=jsonObject.get(retField).toString();
									}else{
										String[] fields=Tools.split(retField,".");
										String h=jsonObject.get(fields[0]).toString();
										RET=getJSONField(h, retField , fields.length-1);
									}
									break;
								}
							}else if(condition.indexOf(">=")!=-1){
								double conval=jsonObject.getDouble(conditionField);
								if(conval>=Double.parseDouble(conditionValue)){
									if(retField.indexOf(".")==-1){
										RET=jsonObject.get(retField).toString();
									}else{
										String[] fields=Tools.split(retField,".");
										String h=jsonObject.get(fields[0]).toString();
										RET=getJSONField(h, retField , fields.length-1);
									}
									break;
								}
							}else if(condition.indexOf("<=")!=-1){
								double conval=jsonObject.getDouble(conditionField);
								if(conval<=Double.parseDouble(conditionValue)){
									if(retField.indexOf(".")==-1){
										RET=jsonObject.get(retField).toString();
									}else{
										String[] fields=Tools.split(retField,".");
										String h=jsonObject.get(fields[0]).toString();
										RET=getJSONField(h, retField , fields.length-1);
									}
									break;
								}
							}else if(condition.indexOf("<")!=-1){
								double conval=jsonObject.getDouble(conditionField);
								if(conval<Double.parseDouble(conditionValue)){
									if(retField.indexOf(".")==-1){
										RET=jsonObject.get(retField).toString();
									}else{
										String[] fields=Tools.split(retField,".");
										String h=jsonObject.get(fields[0]).toString();
										RET=getJSONField(h, retField , fields.length-1);
									}
									break;
								}
							}else if(condition.indexOf(">")!=-1){
								double conval=jsonObject.getDouble(conditionField);
								if(conval>Double.parseDouble(conditionValue)){
									if(retField.indexOf(".")==-1){
										RET=jsonObject.get(retField).toString();
									}else{
										String[] fields=Tools.split(retField,".");
										String h=jsonObject.get(fields[0]).toString();
										RET=getJSONField(h, retField , fields.length-1);
									}
									break;
								}
							}else if(condition.indexOf("!=")!=-1){
								String conval=jsonObject.get(conditionField).toString();
								if(!conval.equals(conditionValue)){
									if(retField.indexOf(".")==-1){
										RET=jsonObject.get(retField).toString();
									}else{
										String[] fields=Tools.split(retField,".");
										String h=jsonObject.get(fields[0]).toString();
										RET=getJSONField(h, retField , fields.length-1);
									}
									break;
								}
							}
						}else{
							if(retField.indexOf(".")==-1){
								RET=jsonObject.get(retField).toString();
							}else{
								String[] fields=Tools.split(retField,".");
								String h=jsonObject.get(fields[0]).toString();
								RET=getJSONField(h, retField , fields.length-1);
							}
							break;
						}
					}
				}catch(JSONException je){
					System.out.println("?????"+je.getMessage());
				}
			}
			if(!RET.equals(""))
				RET=reply.substring(0,n)+RET+reply.substring(n1+1);
			return RET;
		}
	}

	private boolean addAlert(String type, String area, String content, String publisher, String dt){
		//警報類型, 地區, 內容, 發佈者, 更新時間 
//System.out.println("add "+type+" "+content);
		//檢查是否已經發布過了
		boolean found=false;
		for(int i=ALERT.size()-1;i>=0; i--){
			String[] alert=(String[])ALERT.elementAt(i);
			long tm=Long.parseLong(alert[0]);
			if(System.currentTimeMillis()/1000-tm<6*60*60){  //6小時內不要重覆說
				if(alert[1].equals(type) && alert[2].equals(area) && alert[3].equals(content)){
					found=true;
					break;
				}
			}else{
				ALERT.remove(i);
				System.out.println("remove ??");
			}
		}
//System.out.println("found="+found);		
		if(!found)
			ALERT.add(new String[]{(System.currentTimeMillis()/1000)+"", type, area, content, publisher, dt});
			
		return !found;
	}
	private String getJSONField(String jsonString, String field, int idx){
		String[] fi=Tools.split(field, ".");
		if(idx>fi.length-1) return "";

		String retVal="";
		try{
			String para=jsonString.substring(0,1).equals("[")?jsonString:"["+jsonString+"]";

			JSONArray jsonArray = new JSONArray(para);
			for (int j = 0; j < jsonArray.length(); j++) {
				if(idx>fi.length-1) break;
				JSONObject jsonSubObject = jsonArray.getJSONObject(j);

				retVal=jsonSubObject.get(fi[idx])+"";

				if(retVal==null) break;
				if(++idx<fi.length){
					return getJSONField(retVal, field, idx);
				}


			}
		}catch(JSONException e){
			System.out.println("~err~~~"+e.getMessage()+"\n~~~~~~~~~~~~~~~~~~~~~~~~~~~ ~~~~~~~~~~~~~~~~~~~~~~~~~~");
			return retVal;
		}
		return retVal;
	}


	public void stop(BundleContext arg0) throws Exception {
		QUIT=true;
		if(!ZID.equals("")){
//			device.removeDevice(ZID);
//			layout.removeLayout(this, ZID);
//			device.unregisterBundle(context, this, "");
			device.stopInfairyBundle(context, this,  ZID, "");
		}
		
	}

	boolean QUIT=false;
	public void run() {
		// TODO Auto-generated method stub
		
		
		long tm=System.currentTimeMillis()/1000;
		while(!QUIT){
			if(monitor && System.currentTimeMillis()/1000-tm>=60){//每分鐘檢查一次
				Active();
				tm=System.currentTimeMillis()/1000;
			}
	
			lat=property.getLatitude();
			lng=property.getLongitude();

			if(!lat.equals("") && !lng.equals("")){
				if(!isGotMyPos)
					myPosition();
			}

			
			try{
				Thread.sleep(3000);  
			}catch(InterruptedException e){}
		}
		
	}

	public void handleEvent(Event evnt) {
		if(evnt==null) return;
		EventTTSDNA ETO=(EventTTSDNA)evnt.getProperty(device.BROADCAST_TTS_VOICE_TRIGGER_EVENTOBJECT);
		if(ETO==null) return;

		String bid=ETO.FROM_ID;//(String) evnt.getProperty(device.BROADCAST_TTS_VOICE_FROM_ID);
		
		if(!bid.equals("") && !bid.equals(ZID)) //若是有指定廣播對象bid,而且不是自己的ZID, 就不要處理
			return;
		
//		String LATLNG=(String) evnt.getProperty(device.BROADCAST_TTS_VOICE_POSITION_LATLNG);

//		if(!LATLNG.equals("")){
			lat=ETO.LATITUDE;
			lng=ETO.LONGITUDE;
//		}
	}

	String[][] LANG=new String[][]{
/*			{"启动监控","啟動監控","Start"}
			{"取消监控","取消監控","Cancel"},
			{"即时预报","即時預報","Report Now"},
			{"Google灾害示警系统 - 台湾区","Google災害示警系統-台灣區","Google Alart"},
			{"我是谷歌示警系统，我将开始帮您监控你所在区域的警报","我是Google示警系統,我將開始幫您監控你所在區域的警報","This is Google alert, I will begin to help you monitor alarms in your area"},
			{"我是谷歌示警系统，取消监控你所在区域的警报","我是Google示警系統,取消監控你所在區域的警報","This is Google alert, cancel the alarm monitoring in your area"},
			{"我是谷歌示警系统，找不到您的卫星座标，所以，直接以台湾全区做搜寻","我是Google示警系統, 找不到您的衛星座標, 所以, 直接以台灣全區做搜尋","我是Google示警系統, 找不到您的衛星座標, 所以, 直接以台灣全區做搜尋"},
			{"我是谷歌示警系统，您所在的区域目前共有XX项警报","我是Google示警系統,您所在的區域目前共有XX項警報","This is Google alert , now we have XX entry alarm  in your area"},
			{"第XX项","第XX項","XX"},
			{"XX于YY发布ZZ","XX於YY發佈ZZ","XX issued a ZZ on YY"},
			{"影响区域","影響區域","Affected area "},
			{"内容为","內容為","The detail is "},
			{"我是谷歌示警系统，您所在的区域目前没有任何警示预报","我是Google示警系統,您所在的區域目前沒有任何警示預報","This is Google alert, There are no any alert in your area"}
*/
			{"启动监控","啟動監控","start monitoring"},
			{"取消监控","取消監控","stop monitoring"},
			{"即时预报","即時預報","real-time forecast"},
			{"Google示警","Google示警","Google Alert"},
			{"我是谷歌示警系统，我将开始帮您监控你所在区域的警报","我是Google示警系統,我將開始幫您監控你所在區域的警報","This is Google Alert, I will start to monitor the alarm of your area for you"},
			{"我是谷歌示警系统，取消监控你所在区域的警报","我是Google示警系統,取消監控你所在區域的警報","This is Google Alert, we had stopped monitoring the alarm of your area"},
			{"我是谷歌示警系统，找不到您的卫星座标，所以，直接以台湾全区做搜寻","我是Google示警系統, 找不到您的衛星座標, 所以, 直接以台灣全區做搜尋","This is Google alert, couldn't find your Satellite coordinates, therefore, use whole Taiwan as searching area"},
			{"我是谷歌示警系统，您所在的区域目前共有XX项警报","我是Google示警系統,您所在的區域目前共有XX項警報","This is Google Alert, there are XX alarms in your area "},
			{"第XX项","第XX項","the XXth item"},
			{"XX于YY发布ZZ","XX於YY發佈ZZ","XX announced ZZ on YY "},
			{"影响区域","影響區域","arae of influence "},
			{"内容为","內容為","the content is "},
			{"我是谷歌示警系统，您所在的区域目前没有任何警示预报","我是Google示警系統,您所在的區域目前沒有任何警示預報","This is Google Alert, there is no any alarm in your area now."}

	};
	private String LanguagePack(String S){
		
		for(int i=0; i<LANG.length; i++){
			if(LANG[i][0].equals(S))
				return LANG[i][property.LANGUAGE>=LANG[i].length?0:property.LANGUAGE];
		}
		
		return S;
		
	}

	boolean isGotMyPos=false;
	private boolean myPosition(){
//		"^http^JSON=maps.googleapis.com/maps/api/geocode/json?latlng=[getLatLng]&sensor=true&language=zh-TW",
//		"^http^您现在的#inText=地址,位置#大约在@results,,formatted_address@"
		String lat=property.getLatitude();
		String lng=property.getLongitude();
//System.out.println("lat="+lat+", lng="+lng);		
		if(!lat.equals("") && !lng.equals("")){
			String url="maps.googleapis.com/maps/api/geocode/json?latlng="+lat+","+lng+"&sensor=true&language=zh-TW";
//System.out.println(url);		
			String reply="@results.address_components,types=string([\"administrative_area_level_3\",\"political\"]),long_name@";
			String dist=parseJSON(url, reply);
			reply="@results.address_components,types==[\"route\"],long_name@";
			String road=parseJSON(url, reply);
			
			reply="@results.address_components,types=string([\"administrative_area_level_1\",\"political\"]),long_name@";
			String city=parseJSON(url, reply);
			
//System.out.println(dist+" "+road+" "+RoadID+" "+AreaID);
			layout.editLayout(this, ZID, RoadID, device.LAYOUT_ITEM_TEXT, road);
			layout.editLayout(this, ZID, AreaID, device.LAYOUT_ITEM_TEXT, city+ dist);
			isGotMyPos=true;
			
			layout.update(ZID, LayoutID, "");
		}
		return true;
	}

	String LayoutID="";
	String AreaID="", RoadID="";
	String[] PubDate=new String[5];
	String[] Date=new String[5];
	String[] Time=new String[5];
	String[] Content=new String[5];
	String monitorID="";
	private void MakeLayout(String isMonitor){	
		String[] contentKey={
				 device.DEVICE_OPERATION_USER_DEFINED_10,
	             device.DEVICE_OPERATION_USER_DEFINED_11,
	             device.DEVICE_OPERATION_USER_DEFINED_12,
	             device.DEVICE_OPERATION_USER_DEFINED_13,
	             device.DEVICE_OPERATION_USER_DEFINED_14};

		
		LayoutTag la=new LayoutTag();
		la.bundleID=ZID;
		la.LayoutWidth=800;
		la.LayoutHeight=450;
		la.title="Google示警系統";
		LayoutID=layout.createUI(this, la);
//		LayoutID=layout.createLayout(ZID, "Google示警系統", 800,450);
		

		layout.createImage(this, LayoutID, "", "", 800,450,0,0,"","","", "icon/Background.png","#ffffff",22,"",0,"");
		if(isMonitor.equals("Y"))
			monitorID=layout.createImage(this, LayoutID, device.DEVICE_OPERATION_USER_DEFINED_1,"",  101, 101, 52, 342, "","","", "icon/Disable.png","#ffffff",22,"",0,"");
		else
			monitorID=layout.createImage(this, LayoutID, device.DEVICE_OPERATION_USER_DEFINED_0, "", 101, 101, 52, 342, "","","", "icon/Enable.png","#ffffff",22,"",0,"");
		layout.createImage(this, LayoutID, device.DEVICE_OPERATION_USER_DEFINED_2,"",  101, 101, 192, 342, "","","", "icon/UpdateNow.png","#ffffff",22,"",0,"");
		
		AreaID=layout.createText(this, LayoutID, "", "", 160,24,60,172,"Searching",device.LAYOUT_ITEM_ALIGN_LEFT, "", "", "#c8c800", 24, "" , 0, "");
		RoadID=layout.createText(this, LayoutID, "", "", 228,52,66,221,"Loading...",device.LAYOUT_ITEM_ALIGN_CENTER, "", "", "#009400", 45, "" , 0, "");

		for(int i=0; i<5; i++){
			Date[i]=layout.createText(this, LayoutID, "", "", 78,16,377,74+(i*71),"",device.LAYOUT_ITEM_ALIGN_RIGHT, "", "", TextColor[i], 16, "" , 0, "");
			Time[i]=layout.createText(this, LayoutID, "", "", 78,16,377,90+(i*71),"",device.LAYOUT_ITEM_ALIGN_RIGHT, "", "", TextColor[i], 16, "" , 0, "");
			PubDate[i]=layout.createText(this, LayoutID, "", "", 78,16,377,110+(i*71),"",device.LAYOUT_ITEM_ALIGN_RIGHT, "", "", TextColor[i], 16, "" , 0, "");
			Content[i]=layout.createText(this, LayoutID, contentKey[i], "", 307,58,471,78+(i*71),"",device.LAYOUT_ITEM_ALIGN_LEFT, "", "", "#ffffff", 16, "" , 0, "");
		}
		layout.createImage(this, LayoutID, device.LAYOUT_KEY_APP_SYSTEM_DEFINED, "", 40,44, 0, 0,"","",device.LAYOUT_KEY_APP_HOME,"icon/Home.png","#FFFFFF",32,"#68c300", 0, "");
		
		layout.update(ZID, "", "");
		
	}

	String[] TextColor={"#ff2108", "#ff3d72", "#ff5c56", "#ff877e", "#ffafa6"}; //五種顏色
	
	public void Content0(){
		content(0);
	}
	public void Content1(){
		content(1);
	}
	public void Content2(){
		content(2);
	}
	public void Content3(){
		content(3);
	}
	public void Content4(){
		content(4);
	}
	
	private void content(int n){ //讀出Content 內容
		String cont=layout.getLayoutValue(ZID, Content[n], device.LAYOUT_ITEM_TEXT);
		if(cont!=null && !cont.trim().equals(""))
			Tools.addTTS(cont);
	}
	private void clearAllMessage(){
		for(int i=0; i<5; i++){
			layout.editLayout(this, ZID, PubDate[i], device.LAYOUT_ITEM_TEXT, "");
			layout.editLayout(this, ZID, Date[i], device.LAYOUT_ITEM_TEXT, "");
			layout.editLayout(this, ZID, Time[i], device.LAYOUT_ITEM_TEXT, "");
			layout.editLayout(this, ZID, Content[i], device.LAYOUT_ITEM_TEXT, "");
		}
	}

	
	private void moveMessage(int n){
//System.out.println("Move n="+n);		
		if(n>5)
			clearAllMessage();
		else{
			for(int i=4; i>=n; i--){

				String pubdate_i=layout.getLayoutValue(ZID, PubDate[i-n], device.LAYOUT_ITEM_TEXT);
				layout.editLayout(this, ZID, PubDate[i], device.LAYOUT_ITEM_TEXT, pubdate_i);
				String date_i=layout.getLayoutValue(ZID, Date[i-n], device.LAYOUT_ITEM_TEXT);
				layout.editLayout(this, ZID, Date[i], device.LAYOUT_ITEM_TEXT, date_i);
				String time_i=layout.getLayoutValue(ZID, Time[i-n], device.LAYOUT_ITEM_TEXT);
				layout.editLayout(this, ZID, Time[i], device.LAYOUT_ITEM_TEXT, time_i);
				String content_i=layout.getLayoutValue(ZID, Content[i-n], device.LAYOUT_ITEM_TEXT);
				layout.editLayout(this, ZID, Content[i], device.LAYOUT_ITEM_TEXT, content_i);
			}
			
			for(int i=0; i<n; i++){
//System.out.println("   clear "+i);
				layout.editLayout(this, ZID, PubDate[i], device.LAYOUT_ITEM_TEXT, "");
				layout.editLayout(this, ZID, Date[i], device.LAYOUT_ITEM_TEXT, "");
				layout.editLayout(this, ZID, Time[i], device.LAYOUT_ITEM_TEXT, "");
				layout.editLayout(this, ZID, Content[i], device.LAYOUT_ITEM_TEXT, "");
			}
		}
		layout.update(ZID, "", "");		

	}
	
	

}
