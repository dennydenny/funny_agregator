package fa.grubber;

import java.util.ArrayList;
import java.util.List;

import com.vk.api.sdk.objects.wall.WallpostFull;

import fa.common.DownloadedPost;

public class wallPosts2DPostsConverter {
	
	// �����, �������������� ����������� ������ ������ ������� WallpostFull � ������ ������� DownloadedPost.
	public static ArrayList<DownloadedPost> convert(List<WallpostFull> posts)
	{
		if (posts.isEmpty() || posts == null) throw new IllegalStateException("������ ������ ��� ����������� ����.");
		
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