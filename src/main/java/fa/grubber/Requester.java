package fa.grubber;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiAuthException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import com.vk.api.sdk.queries.wall.WallGetFilter;

public class Requester {
	
	private static TransportClient transportClient;
	private static VkApiClient vk;
	private static UserActor actor;
	private static final Logger LOG = LoggerFactory.getLogger(DBHelper.class);
	
	private static void init() throws ApiAuthException
	{
		transportClient = HttpTransportClient.getInstance();
		vk = new VkApiClient(transportClient);
		actor = VKAuthorization.getVKActor();
	}
	
	// Метод, возвращающий коллекцию постов со стены паблика.
	public static List<WallpostFull> getWallPostsFromPublic(Public pub)
	{
		List<WallpostFull> list;
		int count = Integer.valueOf(Settings.settings.get("requestedwallposts"));
		
		try {
			init();
			GetResponse response = vk.wall().get(actor)
										.ownerId(pub.getPublicId() * -1)
										.filter(WallGetFilter.OWNER)
										.count(count)
										.execute();
			list = new ArrayList <WallpostFull>();
			list = response.getItems();
			LOG.debug((String.format("Запрос постов со стены выполнен успешно. Паблик %s", pub.getPublicName())));
			return list;
		}
		catch (ApiAuthException apiAuthException) {
			LOG.debug("Ошибка при запросе постов со стены" + apiAuthException.getMessage());
		}
		catch (Exception e) {
		}
		
		return null;
	}
}