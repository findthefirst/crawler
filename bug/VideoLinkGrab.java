package bug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoLinkGrab {

	public static void main(String[] args) {
		VideoLinkGrab videoLinkGrab = new VideoLinkGrab();
	//	videoLinkGrab.saveData("http://www.zxba.cc//movie/list/-2015----p");
	//	videoLinkGrab.saveData("http://www.zxba.cc/1-2015-------.html");
		videoLinkGrab.saveData("http://www.66ys.tv/aiqingpian/");
	}

	/**
	 * ����ȡ�������ݱ��������ݿ���
	 * 
	 * @param baseUrl
	 *            �������
	 * @return null
	 */
	public void saveData(String baseUrl) {
		Map<String, Boolean> oldMap = new LinkedHashMap<String, Boolean>(); // �洢����-�Ƿ񱻱���

		Map<String, String> videoLinkMap = new LinkedHashMap<String, String>(); // ��Ƶ��������
		String oldLinkHost = ""; // host

		Pattern p = Pattern.compile("(https?://)?[^/\\s]*"); // ���磺http://www.zifangsky.cn
		Matcher m = p.matcher(baseUrl);
		if (m.find()) {
			oldLinkHost = m.group();
		}

		oldMap.put(baseUrl, false);
		videoLinkMap = crawlLinks(oldLinkHost, oldMap);
		// ������Ȼ�����ݱ��������ݿ���
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/mydata", "root", "root");
		//	Connection connection = JDBCDemo.getConnection();
			for (Map.Entry<String, String> mapping : videoLinkMap.entrySet()) {
				PreparedStatement pStatement = connection
						.prepareStatement("insert into movie(MovieName,MovieLink) values(?,?)");
				pStatement.setString(1, mapping.getKey());
				pStatement.setString(2, mapping.getValue());
				pStatement.executeUpdate();
				pStatement.close();
				// System.out.println(mapping.getKey() + " : " +
				// mapping.getValue());
			}
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ץȡһ����վ���п���ץȡ����ҳ���ӣ���˼·��ʹ���˹�������㷨 ��δ�������������Ӳ��Ϸ���GET���� һֱ���������������϶�û�ܷ����µ�����
	 * ���ʾ���ܷ����µ������ˣ��������
	 * 
	 * ��һ�����ӷ�������ʱ���Ը���ҳ�����������������Ҫ����Ƶ���ӣ��ҵ�����뼯��videoLinkMap
	 * 
	 * @param oldLinkHost
	 *            �������磺http://www.zifangsky.cn
	 * @param oldMap
	 *            �����������Ӽ���
	 * 
	 * @return ��������ץȡ������Ƶ�������Ӽ���
	 */
	private Map<String, String> crawlLinks(String oldLinkHost, Map<String, Boolean> oldMap) {
		Map<String, Boolean> newMap = new LinkedHashMap<String, Boolean>(); // ÿ��ѭ����ȡ����������
		Map<String, String> videoLinkMap = new LinkedHashMap<String, String>(); // ��Ƶ��������
		String oldLink = "";
	
		for (Map.Entry<String, Boolean> mapping : oldMap.entrySet()) {
			// System.out.println("link:" + mapping.getKey() + "--------check:"
			// + mapping.getValue());
			// ���û�б�������
			if (!mapping.getValue() ) {    
				oldLink = mapping.getKey();
				// ����GET����
				try {
					URL url = new URL(oldLink);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(2500);
					connection.setReadTimeout(2500);

				     if (connection.getResponseCode() == 200) {
						InputStream inputStream = connection.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "gb2312"));
						String line = "";
						Pattern pattern = null;
						Matcher matcher = null;
						// ��Ӱ����ҳ�棬ȡ�����е���Ƶ�������ӣ�����������ץȡ����ҳ��
						if (isMoviePage(oldLink)) {
							boolean checkTitle = false;
							String title = "";
							while ((line = reader.readLine()) != null) {
								// ȡ��ҳ���е���Ƶ����
								if (!checkTitle) {
									pattern = Pattern.compile("([^\\s]+).*?</title>");
									matcher = pattern.matcher(line);
									if (matcher.find()) {
										title = matcher.group(1);
										checkTitle = true;
									//�����õ�	System.out.println(title);
										continue;
									}
								}
								// ȡ��ҳ���е���Ƶ��������
							//	pattern = Pattern.compile("(thunder:[^\"]+).*thunder[rR]es[tT]itle=\"[^\"]*\"");
							//	pattern = Pattern.compile("(magnet[^\"]*)");
								 pattern = Pattern.compile("(http://pan.baidu.com[^\"]*)</td>$");
								matcher = pattern.matcher(line);
								if (matcher.find()) {
								//	System.out.println("�ж��Ƿ�ȡ��ҳ���е���Ƶ��������");
									videoLinkMap.put(title, matcher.group(1));
									System.out.println("��Ƶ���ƣ� " + title + " ------ ��Ƶ���ӣ�" + matcher.group(1));
								
									break; // ��ǰҳ���Ѿ�������
								}
							}
						}
						// ��Ӱ�б�ҳ��
						else if (checkUrl(oldLink)) {
							while ((line = reader.readLine()) != null) {

								pattern = Pattern.compile("<a href=\"([^\"\\s]*)\"");
								matcher = pattern.matcher(line);
								while (matcher.find()) {
									String newLink = matcher.group(1).trim(); // ����
									// �жϻ�ȡ���������Ƿ���http��ͷ
									if (!newLink.startsWith("http")) {
										if (newLink.startsWith("/"))
											newLink = oldLinkHost + newLink;
										else
											newLink = oldLinkHost + "/" + newLink;
									}
									// ȥ������ĩβ�� /
									if (newLink.endsWith("/"))
										newLink = newLink.substring(0, newLink.length() - 1);
									// ȥ�أ����Ҷ���������վ������
									if (!oldMap.containsKey(newLink) && !newMap.containsKey(newLink)
											&& (checkUrl(newLink) || isMoviePage(newLink))) {
//										if (videoLinkMap.size() > 250) {                  //videoLinkMap.size() ��������ƬԴ�����Ŀ
//											return videoLinkMap;
//										}
										System.out.println("temp: " + newLink);
										newMap.put(newLink, false);
									}
								}
							}
						}

						reader.close();
						inputStream.close();
					}
					connection.disconnect();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				oldMap.replace(oldLink, false, true);
			}
		}
		// �������ӣ���������
		if (!newMap.isEmpty()) { //�������������
			oldMap.putAll(newMap);
			videoLinkMap.putAll(crawlLinks(oldLinkHost, oldMap)); // ����Map�����ԣ����ᵼ�³����ظ��ļ�ֵ��
		}
		return videoLinkMap;
	}
	
	/**
	 * �ж��Ƿ���2015��ĵ�Ӱ�б�ҳ��
	 * 
	 * @param url
	 *            �����URL
	 * @return ״̬
	 */
	public boolean checkUrl(String url) {
	//	Pattern pattern = Pattern.compile("http://www.zxba.cc/1-2015-------.html\\d*");
		Pattern pattern = Pattern.compile("http://www.66ys.tv/aiqingpian/\\d*");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find())
			return true; // 2015����б�
		else
			return false;
	}

	/**
	 * �ж�ҳ���Ƿ��ǵ�Ӱ����ҳ��
	 * 
	 * @param url
	 *            ҳ������
	 * @return ״̬
	 */
	public boolean isMoviePage(String url) {
	//	Pattern pattern = Pattern.compile("http://www.zxba.cc/movie/\\d+");
		Pattern pattern = Pattern.compile("http://www.66ys.tv/aiqingpian/\\d+");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find())
			return true; // ��Ӱҳ��
		else
			return false;
	}

}