package edu.kndev.translate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import lombok.extern.log4j.Log4j;

/**
 * 1.应用相关前缀 pre 2.请求相关数据 calc 3.生成签名 sign 4.生成认证信息, 通过 Authorization header,
 * 发送请求接口
 */
@Log4j
public class SougouTranslate {
	private boolean status = true;

	public static void main(String[] args) {
		SougouTranslate a = new SougouTranslate();
		String result = a.getResult("自然语言处理");
		System.out.println(a.getStatus());
	}

	public boolean getStatus() {
		return status;
	}

	public String getResult(String text) {
		if (text == null || text == "") {
			return null;
		}

		String result = "";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		log.info("开始爬取SouGou翻译");
		// 控制台中的密钥
		String ak = "3wC2D0acnQAM4nbZ4PYAHgFp";
		String sk = "P7IryJE2FTg7UUqyjGiXJo-8RTUdy1aX";
		// 原始语言
		String from = "zh-CHS";
		// 目标翻译语言
		String to = "en";
		// HTTP请求方式
		String method = "GET";
		// 输入的语言
		String q = rawurlencode(text);
		// String q = text;
		// 请求的url
		String url = "http://api.ai.sogou.com/pub/nlp/translate?q=" + q + "&from=" + from + "&to=" + to;
		// 从url获得下面的信息用于加密
		String host = "api.ai.sogou.com";
		String path = "/pub/nlp/translate";
		// 请求参数
		String query = "q=" + q + "&from=" + from + "&to=" + to;
		// 排列后的请求参数
		String arg = "from=" + from + "&q=" + q + "&to=" + to;
		// 签名的前缀
		String pre = "sac-auth-v1/" + ak + "/" + System.currentTimeMillis() / 1000 + "/3600";
		// 签名的数据
		String calc = pre + "\n" + "GET" + "\n" + host + "\n" + path + "\n" + arg;
		// 根据签名的前缀和数据生成对应的密钥
		String hmac = sha256_HMAC(calc, sk.getBytes(StandardCharsets.UTF_8));

		// 生成的hmac生成最终码
		String sign = pre + "/" + hmac;

		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-Type", "application/json");
		httpGet.addHeader("Authorization", sign);
		String body_content = "";
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet);
			// System.out.println(response.getStatusLine().getStatusCode());
			Document doc = Jsoup.parse(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
			body_content = doc.getElementsByTag("body").text();
			JSONObject json = new JSONObject(body_content);
			if ((Integer) json.get("status") == 0) {
				JSONArray trans_result = (JSONArray) json.get("trans_result");
				JSONObject trans_text = (JSONObject) trans_result.get(0);
				result = trans_text.getString("trans_text");
				if (result.length() != 0) {
					log.info("从SouGou 找到" + "“" + text + "”" + "的英文翻译" + result);
				} else {
					log.info("没有从SouGou 中找到" + text + "的英文翻译");
				}
			}
			response.close();
		} catch (IOException e) {
			e.printStackTrace();
			status = false;
			log.info("SouGou 翻译失败");

		}
		return result;
	}

	// php中的urlencode
	private String rawurlencode(String query) {
		String queryCode = null;

		try {
			queryCode = URLEncoder.encode(query, String.valueOf(StandardCharsets.UTF_8)).replace("*", "%2A");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.info("SouGou翻译编码错误");
		}

		return queryCode;
	}

	// 密钥生成
	private String sha256_HMAC(String message, byte[] secret) {
		String hash = "";
		try {
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(secret, "HmacSHA256");
			sha256_HMAC.init(secret_key);
			byte[] bytes = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
			hash = new String(java.util.Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
			// hash = org.apache.commons.codec.binary.Base64.encodeBase64String(bytes);
			// System.out.println(hash);
		} catch (Exception e) {
			// System.out.println("Error HmacSHA256 ===========" + e.getMessage());
			e.printStackTrace();
			log.info("SouGou翻译密钥生成失败");
		}
		return hash;
	}

}
