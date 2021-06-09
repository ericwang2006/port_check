package com.firstsunlight.port_check;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity
{

	private Button btnConnect;
	private EditText edtIP;
	private EditText edtPort;
	private TextView txtMessage;
	private RadioButton RadioTCP;

	static class MyHandler extends Handler
	{
		WeakReference<MainActivity> mActivity;

		MyHandler(MainActivity activity)
		{
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg)
		{
			MainActivity theActivity = mActivity.get();
			switch (msg.what)
			{
			case 1024:
				theActivity.txtMessage.setText("端口状态:开放");
				theActivity.txtMessage.setTextColor(Color.parseColor("#43CD80"));
				theActivity.btnConnect.setText("开始检测");
				theActivity.btnConnect.setEnabled(true);
				break;
			case 1025:
				theActivity.txtMessage.setText("端口状态:关闭");
				theActivity.txtMessage.setTextColor(Color.RED);
				theActivity.btnConnect.setText("开始检测");
				theActivity.btnConnect.setEnabled(true);
				break;
			default:
				break;
			}
		}
	};

	private Handler mHandler = new MyHandler(this);

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnConnect = (Button) findViewById(R.id.btnConnect);
		edtIP = (EditText) findViewById(R.id.edtIP);
		edtPort = (EditText) findViewById(R.id.edtPort);
		txtMessage = (TextView) findViewById(R.id.txtMessage);
		RadioTCP = (RadioButton) findViewById(R.id.radio0);

		btnConnect.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final String sIP = edtIP.getText().toString().trim();

				Pattern pattern = Pattern.compile("^[A-Za-z0-9-.]{1,63}$");
				Matcher matcher = pattern.matcher(sIP);
				if (!matcher.matches())
				{
					txtMessage.setText("请输入合法域名或者IP地址！");
					txtMessage.setTextColor(Color.RED);
					return;
				}

				final String sPort = edtPort.getText().toString().trim();
				pattern = Pattern.compile("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");
				matcher = pattern.matcher(sPort);
				if (!matcher.matches())
				{
					txtMessage.setText("端口范围必须是1-65535！");
					txtMessage.setTextColor(Color.RED);
					return;
				}

				btnConnect.setText("正在检测,请等待...");
				txtMessage.setText("");
				btnConnect.setEnabled(false);
				new Thread(new Runnable()
				{
					public void run()
					{
						int iR;
						if (RadioTCP.isChecked())
							iR = TcpScan(sIP, Integer.parseInt(sPort), 5000);
						else
							iR = UdpScan(sIP, Integer.parseInt(sPort), 5000);
						if (iR > 0)
							mHandler.sendEmptyMessage(1024);
						else
							mHandler.sendEmptyMessage(1025);
					}
				}).start();
			}
		});
	}

	// http://www.what21.com/sys/view/java_java-tool_1456896107736.html
	public static int UdpScan(String host, int port, int timeOut)
	{
		int flag = 0;
		DatagramSocket socket = null;
		byte[] data = host.getBytes();
		try
		{
			socket = new DatagramSocket();
			socket.setSoTimeout(timeOut);
			socket.setTrafficClass(0x04 | 0x10);
			socket.connect(new InetSocketAddress(host, port));
			socket.send(new DatagramPacket(data, data.length));
			byte[] receive = new byte[4096];
			DatagramPacket dp = new DatagramPacket(receive, 4096);
			socket.receive(dp);
			if (dp != null && dp.getData() != null)
			{
				flag = 1;// 有响应,大概率端口开放
			}
		}
		catch (SocketTimeoutException e)
		{
			flag = 2; // 超时,可能端口开放
		}
		catch (PortUnreachableException e)
		{
			flag = -1; // 端口不通,大概率端口未开放(ICMP_PORT_UNREACH)
		}
		catch (Exception e)
		{
			flag = 0; // 未知异常,可能端口未开放
		}
		finally
		{
			try
			{
				if (socket != null)
				{
					socket.close();
				}
			}
			catch (Exception e)
			{
			}
		}
		return flag;
	}

	public static int TcpScan(String host, int port, int timeOut)
	{
		int flag = 0;
		Socket socket = null;
		try
		{
			// socket = new Socket(host, port);
			// socket.setSoTimeout(timeOut);
			socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), 5000);
			flag = 1;
		}
		catch (IOException e)
		{
		}
		finally
		{
			try
			{
				if (socket != null)
				{
					socket.close();
				}
			}
			catch (Exception e)
			{
			}
		}
		return flag;
	}

}
