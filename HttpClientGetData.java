
package com.zjcjavat.httpClient;

import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.DefaultCookieSpecProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/**
 * Created by bamboo on 2016-1-23.
 *email:zjcjava@126.com
 * 登录TMS后台获取路线信息
 */
public class HttpClientGetData {
	
	private  static String account="account";
	private  static String password="password";
	private  static String projectId="194";
	//private  static String excelFormat="%s\t204\t杭州\t浙江省\t杭州\t%s\t%s\t%s\t公路吨	0	999	0	0	0	0\r\n";
	private  static String excelFormat="INSERT sys_often_line  VALUES (%s,'751','%s','浙江省','%s','%s','%s','%s','公路吨',0,999,0,0,0,0);\r\n";
	private  static Integer rowNo=938235;//
	
    public static void main(String[] args){
    	
    	 //创建一个HttpClient
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        try {
    	
	    	 //创建一个get请求用来接收_xsrf信息
	    	HttpGet get = new HttpGet("http://tms.XXXXXX.com/");
	        //1进入登录页面并获取cookie
	        CloseableHttpResponse response = httpClient.execute(get);
	        String cookieStr=setCookie(response);
	        response.close();
	        //2构造post和登录数据
	        List<NameValuePair> valuePairs = new LinkedList<NameValuePair>();
	        valuePairs.add(new BasicNameValuePair("account", account));
	        valuePairs.add(new BasicNameValuePair("password", password));
	        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairs, Consts.UTF_8);
	        //创建一个post请求
	        HttpPost post = new HttpPost("http://tms.XXXXX.com/");
	        post.setHeader("Cookie", cookieStr);//
	        System.out.println(cookieStr);
	        //注入post数据
	        post.setEntity(entity);
	        HttpResponse httpResponse = httpClient.execute(post);
	        //打印登录是否成功信息
	        //printResponse(httpResponse);
	        cookieStr=setCookie(httpResponse);//再次获取cookie备用为下次请求使用
	        System.out.println(cookieStr);
	        //3get请求跳转到后台页面
	        HttpGet deskGet = new HttpGet("http://tms.XXXX.com/DeskTop");
	        //获取_xsrf
	        CloseableHttpResponse deskGetResponse = httpClient.execute(deskGet);
	        //打印登录是否成功信息
	        String deskGetHtml = EntityUtils.toString(deskGetResponse.getEntity());
            String loginName = deskGetHtml.split("<li class=\"wel\">")[1].split("（<span")[0];
            System.out.println(loginName+"已进入后台管理系统...");
            deskGetResponse.close();
            //4创建一个post请求
	        HttpPost routPpost = new HttpPost("http://tms.XXXX.com/Register/ViewPrice");
	        routPpost.setHeader("Cookie", cookieStr);//
	        List<NameValuePair> projectParam = new LinkedList<NameValuePair>();
	        projectParam.add(new BasicNameValuePair("projectId", projectId));
	        UrlEncodedFormEntity projectParamEntity = new UrlEncodedFormEntity(projectParam, Consts.UTF_8);
	        //注入post数据
	        routPpost.setEntity(projectParamEntity);
	        HttpResponse routResponse = httpClient.execute(routPpost);
	        //打印获取到的路线页面信息
	        //printResponse(routResponse);
	        String htmlStrl=printResponse(routResponse);
	        //System.out.println(htmlStrl);
	        tmsPase(htmlStrl);
            
            

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    ///知乎登录的demo 省略
    
    
    
    public static String printResponse(HttpResponse httpResponse)
            throws ParseException, IOException {
    	String htmlConent=null;
        // 获取响应消息实体
        HttpEntity entity = httpResponse.getEntity();
        // 响应状态
        System.out.println("status:" + httpResponse.getStatusLine());
        System.out.println("headers:");
        HeaderIterator iterator = httpResponse.headerIterator();
        while (iterator.hasNext()) {
            System.out.println("\t" + iterator.next());
        }
        // 判断响应实体是否为空
        if (entity != null) {
            String responseString = EntityUtils.toString(entity);
            System.out.println("response length:" + responseString.length());
            System.out.println("response content:"
                    + responseString.replace("\r\n", ""));
            
            htmlConent=responseString.replace("\r\n", "");
        }
        return htmlConent;
    }
    public static Map<String,String> cookieMap = new HashMap<String, String>(64);
    //从响应信息中获取cookie
    public static String setCookie(HttpResponse httpResponse)
    {
        System.out.println("----setCookieStore");
        Header headers[] = httpResponse.getHeaders("Set-Cookie");
        if (headers == null || headers.length==0)
        {
            System.out.println("----there are no cookies");
            return null;
        }
        String cookie = "";
        for (int i = 0; i < headers.length; i++) {
            cookie += headers[i].getValue();
            if(i != headers.length-1)
            {
                cookie += ";";
            }
        }
        String cookies[] = cookie.split(";");
        for (String c : cookies)
        {
            c = c.trim();
            if(cookieMap.containsKey(c.split("=")[0]))
            {
                cookieMap.remove(c.split("=")[0]);
            }
            cookieMap.put(c.split("=")[0], c.split("=").length == 1 ? "":(c.split("=").length ==2?c.split("=")[1]:c.split("=",2)[1]));
        }
        System.out.println("----setCookieStore success");
        String cookiesTmp = "";
        for (String key :cookieMap.keySet())
        {
            cookiesTmp +=key+"="+cookieMap.get(key)+";";
        }
        return cookiesTmp.substring(0,cookiesTmp.length()-2);
    }
    
    /**
     * 获取合同路线的起始地内容
     */
    public static void tmsPase(String htmlStrl) {
        Document doc;
        String startP="";
        try {
        	if(htmlStrl!=null){
        		doc = Jsoup.parse(htmlStrl);
        	}else{
        		File input = new File("C:/Users/Administrator/Desktop/新建文件夹/ViewPrice.html");
           	 	doc = Jsoup.parse(input,"UTF-8","http://www.oschina.net/");
        	}
        	 
           // doc = Jsoup.connect("https://shop142735991.taobao.com/index.htm?spm=a1z10.1-c.w5002-12948943046.2.nFo25t").get();
            //System.out.println(doc.toString());
            Element selectDivs = doc.getElementsByAttributeValue("class","selectStyle").first();
            System.out.println(selectDivs.select("option[selected]").attr("value")+selectDivs.select("option[selected]").text());//项目名称
            Elements tableHtmls = doc.select("table.fixedTable");
            for (Element tableHtml :tableHtmls) {
            	startP=tableHtml.select("thead").first().select("th[colspan]").text();
            	System.out.println("起始地\t"+startP);
                
                Element tbodyHtml = tableHtml.select("tbody").first();
                Elements trHtmls = tableHtml.getElementsByTag("tr");//获取行
                String stopS="";
                
                for (Element trDom :trHtmls) {
                	Elements tdHtmls = trDom.getElementsByTag("td");//获取列
                	 for (Element tdDom :tdHtmls) {
                		//System.out.println("---");
                     	if(tdDom.attr("style").length()>0){//起始地的行
                     		
                     		if(tdDom.select("[colspan=5]").size()>0){
                     			String startPlace=tdDom.select("[colspan=5]").first().text();
                         		//System.out.println(tdDom.select("[colspan=5]").first().text());
                     			stopS=startPlace;
                         		//System.out.println(stopS+"\t"+tdDom.attr("style"));
                     			//System.out.println("116886	204	杭州	浙江省	杭州	修武县	河南省	新乡	公路吨	0	999	315	0	0	1020.8");
                     		}
                     		
                     	}else {
                     		String stopP=tdDom.text().split("（")[0];//地区名称
                     		if(stopP.length()>0){
                     			rowNo++;
                     			String stopC=tdDom.text().split("（")[1].replace("）", "");//县市
                     			//System.out.println(stopS+"\t"+stopC);//目的地
                     			System.out.printf(excelFormat,rowNo,startP,startP,stopP,stopS,stopC);//目的地
                     		}
                     		
                     	}
                	 }
                	 //System.out.println();
                	
                	
                }
            }
            
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    
}