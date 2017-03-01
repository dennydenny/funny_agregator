package fa.grubber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiAuthException;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;


public class VKAuthorization {

	private final static String _token = "ecd7479e0e24dff0e1095a92410f832b2959ea2e3312df33ac233c2ef4277e5913fd41acb5e9577ee28d1";
	private static UserActor _actor;
	private final static int _userId = 414240954;
	private static final Logger LOG = LoggerFactory.getLogger(VKAuthorization.class);
	
	/*
	 * ��������� �����, ������������ Actor VK
	 */
	public static UserActor getVKActor() throws ApiAuthException
	{
		if (isCurrentTokenValid())
		{
			LOG.info("����� ��������. ���������� VK Actor.");
			_actor = new UserActor(_userId, _token);
			return _actor;
		}
		else
		{
			throw new ApiAuthException("Token is invalid.");
		}
	}
	
	// �����, ������������ VK Actor ��� �������� � ������� �������.
	private static UserActor getCurrentVKActor()
	{
		_actor = new UserActor(_userId, _token);
		return _actor;
	}
	
	// �����, ���������� ���������� �������� ������.
	private static boolean isCurrentTokenValid ()
	{
		LOG.debug("��������� ���������� �������� ������...");
		TransportClient transportClient = HttpTransportClient.getInstance(); 
		VkApiClient vk = new VkApiClient(transportClient); 
		com.vk.api.sdk.objects.status.Status st;
		try {
			st = vk.status().get(getCurrentVKActor()).execute();
			if (st.getText().isEmpty())
			{
				LOG.debug("����� �� ��������.");
				return false;
			}
			else
			{
				LOG.debug("����� ��������.");
				LOG.debug("������ ������������: " + st.getText());
				return true;
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (ClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}