package fa.grubber;

import java.util.ArrayList;
import java.util.List;

import com.vk.api.sdk.objects.wall.WallpostFull;

import fa.common.DownloadedPost;

public class wallPosts2DPostsConverter {
	
	// ћетод, осуществл€ющий конвертацию списка постов формата WallpostFull в список формата DownloadedPost.
	public static ArrayList<DownloadedPost> convert(List<WallpostFull> posts)
	{
		if (posts.isEmpty() || posts == null) throw new IllegalStateException("—писок постов дл€ конвертации пуст.");
		
		ArrayList<DownloadedPost> downloadedPosts = new ArrayList<DownloadedPost>();
    	for (WallpostFull post : posts)
    	{
    		DownloadedPost dpost = new DownloadedPost(post.getOwnerId() * -1, 
    				post.getId(), 
    				post.getText(), 
    				post.getLikes().getCount(), 
    				post.getReposts().getCount(),
    				post.getViews().getCount(),
    				post.getDate());
    		downloadedPosts.add(dpost);
    	}
		return downloadedPosts;
	}
}