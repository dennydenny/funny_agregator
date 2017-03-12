package fa.common;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiAuthException;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.base.responses.OkResponse;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import com.vk.api.sdk.objects.wall.responses.RepostResponse;
import com.vk.api.sdk.queries.groups.GroupField;
import com.vk.api.sdk.queries.wall.WallGetFilter;

import fa.grubber.VKAuthorization;

public class Requester {
	
	private static TransportClient transportClient;
	private static VkApiClient vk;
	private static UserActor actor;
	private static final Logger LOG = LoggerFactory.getLogger(Requester.class);
	private static final int _ourPublicId = Integer.valueOf(Settings.settings.get("our_public_id"));
	
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
			LOG.error("Ошибка API при запросе постов со стены" + apiAuthException.getMessage());
		}
		catch (Exception e) {
			LOG.error("Ошибка при запросе постов со стены" + e.getMessage());
		}
		
		return null;
	}

	// Метод, возвращающий число подписчиков паблика в VK.
	public static int getPublicSubsCount(Public pub) throws ApiException, ClientException
	{
		if (pub == null) throw new IllegalStateException("Public is null.");
		
		LOG.info(String.format("Получаем инфо о кол-ве подписчиков в группе VK. PublicId: %d", pub.getPublicId()));
		
		try {
			init();
			
			List<GroupFull> response = vk.groups().getById(actor)
					.groupId(String.valueOf(pub.getPublicId()))
					.fields(GroupField.MEMBERS_COUNT)
					.execute();

			if (!response.isEmpty() && response.size() == 1)
			{
				// Число подписчиков.
				int count = response.get(0).getMembersCount();
				
				LOG.debug(String.format("Инфо о кол-ве подписчкиков паблика успешно загружена. Чило: %d, PublicId: %d",
							count,
							pub.getPublicId()));
				return count;
			}
			else LOG.error(String.format("Не удалось получить число подписчиков группы. PublicId: %d", pub.getPublicId()));
			
		} catch (Exception e) {
			LOG.error(String.format("При получении кол-ва подписчиков паблика VK возникла ошибка %S", e.getMessage()));
		}
		return 0;
	}

	public static void repostPostToPublic (DownloadedPost post)
	{
		try {
			init();
			RepostResponse resp = vk.wall().repost(actor, post.ToRepostObject())
					.groupId(_ourPublicId)
					.execute();
			OkResponse ok = resp.getSuccess();
			if (ok.getValue() == 1)
			{
				LOG.info(String.format("Пост успешно опубликован. PostId: %d, PublicId: %d.", 
            			post.getPostId(),
            			post.getPublicId()));
			}
			else
			{
				LOG.warn(String.format("Отсутствует информация об успешной публикации поста. PostId: %d, PublicId: %d.", 
            			post.getPostId(),
            			post.getPublicId()));	
			}
		}
		catch (Exception e)
		{
			LOG.error("Ошибка при репосте поста на стену" + e.getMessage());
		}	
	}
}