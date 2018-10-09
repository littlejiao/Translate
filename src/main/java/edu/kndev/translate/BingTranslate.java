package edu.kndev.translate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import lombok.extern.log4j.Log4j;

@Log4j
public class BingTranslate {
	private boolean status = true;
	private CloseableHttpClient client;
	
	public static void main(String[] args) throws Exception {
		BingTranslate bt = new BingTranslate();
		String result = bt.getResult("机器学习");
		System.out.println(bt.getStatus());
	}
	
	public BingTranslate() {
		// TODO Auto-generated constructor stub
		client = HttpClients.createDefault();
	}

	public boolean getStatus() {
		return status;
	}
	
	public String getResult(String text) {
		return getTranslate(text);
	}

	private String getTranslate(String text) {
		String IG = "";
		String url = "http://cn.bing.com/translator/";
		HttpGet httpGet = new HttpGet(url);

		httpGet.addHeader("Host", "cn.bing.com");
		httpGet.addHeader("Connection", "keep-alive");
		httpGet.addHeader("Upgrade-Insecure-Requests", "1");
		httpGet.addHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
		httpGet.addHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		httpGet.addHeader("Accept-Encoding", "gzip, deflate");
		httpGet.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
		httpGet.addHeader("referer", "http://www.bing.com/translator/");	
		try {
			CloseableHttpResponse response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			Document doc = Jsoup.parse(EntityUtils.toString(entity));
			Element ele = doc.getElementsByTag("script").get(1);
			IG = ele.toString().split(",")[4].substring(4, 36);
			response.close();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status = false;
			log.info("Bing 获取参数IG失败");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status = false;
			log.info("Bing 获取参数IG失败");
		}
		
		if (text == null || text == "")
			return null;

		String result = "";
		// 第二个IID参数不知道从哪儿获取，先设定为1
		String url_post = "http://cn.bing.com/ttranslate?&category=&IG=" + IG + "&IID=translator.5036.1";
		log.info("开始爬取Bing翻译");
		HttpPost httpPost = new HttpPost(url_post);
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();

		nvps.add(new BasicNameValuePair("text", text));
		nvps.add(new BasicNameValuePair("from", "zh-CHS"));
		nvps.add(new BasicNameValuePair("to", "en"));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.info("Bing翻译post参数编码错误");
		}
		httpPost.setHeader("Host", "cn.bing.com");
		httpPost.setHeader("Connection", "keep-alive");
		httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
		// httpPost.addHeader("Content-Length", "42");
		httpPost.setHeader("Origin", "http://cn.bing.com");
		httpPost.setHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
		httpPost.setHeader("Accept-Encoding", "gzip, deflate");
		httpPost.setHeader("Accept", "*/*");
		httpPost.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
		httpPost.setHeader("referer", "http://cn.bing.com/translator/");
		try {
			CloseableHttpResponse resp = client.execute(httpPost);
			HttpEntity entity = resp.getEntity();
			Document document = Jsoup.parse(EntityUtils.toString(entity));
			String body_content = document.getElementsByTag("body").text();

			JSONObject json = new JSONObject(body_content);
			int statuscode = (Integer) json.get("statusCode");
			result = json.getString("translationResponse");
			if (statuscode == 200 && result.length() != 0) {
				log.info("从Bing 找到" + "“" + text + "”" + "的英文翻译" + result);
			} else {
				log.info("没有从Bing 中找到" + text + "的英文翻译");
			}
			resp.close();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status = false;
			log.info("Bing翻译失败");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			status = false;
			log.info("Bing翻译失败");
		}
		return result;
	}

}
