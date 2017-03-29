package fa.grubber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiAuthException;
import com.vk.api.sdk.exceptions.ApiAuthValidationException;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;

import fa.common.Settings;


public class VKAuthorization {

	private final static String _token = Settings.settings.get("token");
	private static UserActor _actor;
	private final static int _userId = Integer.valueOf(Settings.settings.get("user_id"));
	private static final Logger LOG = LoggerFactory.getLogger(VKAuthorization.class);
	
	/*
	 * Публичный метод, возвращающий Actor VK
	 */
	public static UserActor getVKActor() throws ApiAuthException
	{
		if (isCurrentTokenValid())
		{
			LOG.info("Токен валидный. Возвращаем VK Actor.");
			_actor = new UserActor(_userId, _token);
			return _actor;
		}
		else
		{
			throw new ApiAuthException("Token is invalid.");
		}
	}
	
	// Метод, возвращающий VK Actor без проверок с текущим токеном.
	private static UserActor getCurrentVKActor()
	{
		_actor = new UserActor(_userId, _token);
		return _actor;
	}
	
	// Метод, проверящий валидность текущего токена.
	private static boolean isCurrentTokenValid ()
	{
		LOG.debug("Проверяем валидность текущего токена...");
		TransportClient transportClient = HttpTransportClient.getInstance(); 
		VkApiClient vk = new VkApiClient(transportClient); 
		com.vk.api.sdk.objects.status.Status st;
		try {
			st = vk.status().get(getCurrentVKActor()).execute();
			if (st.getText().isEmpty())
			{
				LOG.debug("Токен не валидный.");
				return false;
			}
			else
			{
				LOG.debug("Токен валидный.");
				LOG.debug("Статус пользователя: " + st.getText());
				return true;
			}
		}
		catch (ApiAuthValidationException apiAuthValidationException) {
			LOG.error("Ошибка валидации API при проверке валидности токена " 
					+ apiAuthValidationException.getMessage() 
					+ " " 
					+ apiAuthValidationException.getRedirectUri());
			return false;
		} 
		catch (ApiException e) {
			e.printStackTrace();
			return false;
		} 
		catch (ClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}