package org.moment.lib.utils;

import org.moment.lib.bean.CircleItem;
import org.moment.lib.bean.CommentItem;
import org.moment.lib.bean.FavortItem;
import org.moment.lib.bean.PhotoInfo;
import org.moment.lib.bean.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class DatasUtil {
	public static final String[] CONTENTS = { "",
			"悟空和唐僧一起上某卫视非诚勿扰,悟空上台,24盏灯全灭。理由:1.没房没车只有一根破棍. 2.保镖职业危险.3.动不动打妖精,对女生不温柔. 4.坐过牢,曾被压五指山下500年。唐僧上台，哗!灯全亮。 理由:1.公务员； 2.皇上兄弟，后台最硬 3.精通梵文等外语 4.长得帅 5.最关键一点：有宝马！",
			"People cry, not because they\\'re weak. It\\'s because they\\'ve been strong for too long. ",
			"哈哈哈哈",
			"http://t.elecfans.com/c382.html?elecfans_trackid=www_Article_detail_m" };
	public static final String[] HEADIMG = {
			"http://b-ssl.duitang.com/uploads/item/201608/21/20160821194924_UCvFZ.jpeg",
			"http://www.feizl.com/upload2007/2014_06/1406272351394618.png",
			"http://pic2.zhimg.com/50/v2-1c3bd9fe6c6a28c5ca3a678549dfde28_hd.jpg",
			"http://pic1.zhimg.com/50/v2-6444e641d0235006e81bc4210b5da89b_hd.jpg",
			"http://v1.qzone.cc/avatar/201408/20/17/23/53f468ff9c337550.jpg!200x200.jpg",
			"http://cdn.duitang.com/uploads/item/201408/13/20140813122725_8h8Yu.jpeg",
			"http://img.woyaogexing.com/touxiang/nv/20140212/9ac2117139f1ecd8%21200x200.jpg",
			"http://pic3.zhimg.com/50/v2-280218dcc9db1a9108b867bd81b29745_hd.jpg"};

	public static List<User> users = new ArrayList<User>();
	public static List<PhotoInfo> PHOTOS = new ArrayList<>();
	/**
	 * 动态id自增长
	 */
	private static int circleId = 0;
	/**
	 * 点赞id自增长
	 */
	private static int favortId = 0;
	/**
	 * 评论id自增长
	 */
	private static int commentId = 0;
	public static final User curUser = new User("0", "我", HEADIMG[0]);
	static {
		User user1 = new User("1", "张三", HEADIMG[1]);
		User user2 = new User("2", "李四", HEADIMG[2]);
		User user3 = new User("3", "老王", HEADIMG[3]);
		User user4 = new User("4", "赵六", HEADIMG[4]);
		User user5 = new User("5", "田七", HEADIMG[5]);
		User user6 = new User("6", "Naoki", HEADIMG[6]);
		User user7 = new User("7", "测试长名字，测试长名字", HEADIMG[7]);

		users.add(curUser);
		users.add(user1);
		users.add(user2);
		users.add(user3);
		users.add(user4);
		users.add(user5);
		users.add(user6);
		users.add(user7);

		PhotoInfo p1 = new PhotoInfo();
		p1.url = "http://file02.16sucai.com/d/file/2014/0704/e53c868ee9e8e7b28c424b56afe2066d.jpg";
		p1.w = 640;
		p1.h = 792;

		PhotoInfo p2 = new PhotoInfo();
		p2.url = "http://dmimg.5054399.com/allimg/pkm/pk/22.jpg";
		p2.w = 640;
		p2.h = 792;

		PhotoInfo p3 = new PhotoInfo();
		p3.url = "http://file02.16sucai.com/d/file/2014/0829/372edfeb74c3119b666237bd4af92be5.jpg";
		p3.w = 950;
		p3.h = 597;

		PhotoInfo p4 = new PhotoInfo();
		p4.url = "http://58pic.ooopic.com/58pic/12/87/82/74h58PICrsx.jpg";
		p4.w = 533;
		p4.h = 800;

		PhotoInfo p5 = new PhotoInfo();
		p5.url = "http://file02.16sucai.com/d/file/2015/0408/779334da99e40adb587d0ba715eca102.jpg";
		p5.w = 700;
		p5.h = 467;

		PhotoInfo p6 = new PhotoInfo();
		p6.url = "http://img03.tooopen.com/uploadfile/downs/images/20110714/sy_20110714135215645030.jpg";
		p6.w = 700;
		p6.h = 467;

		PhotoInfo p7 = new PhotoInfo();
		p7.url = "http://dl.ppt123.net/pptbj/201603/2016030410190920.jpg";
		p7.w = 1024;
		p7.h = 640;

		PhotoInfo p8 = new PhotoInfo();
		p8.url = "http://pic4.nipic.com/20091101/3672704_160309066949_2.jpg";
		p8.w = 1024;
		p8.h = 768;

		PhotoInfo p9 = new PhotoInfo();
		p9.url = "http://pic4.nipic.com/20091203/1295091_123813163959_2.jpg";
		p9.w = 1024;
		p9.h = 640;

		PhotoInfo p10 = new PhotoInfo();
		p10.url = "http://pic31.nipic.com/20130624/8821914_104949466000_2.jpg";
		p10.w = 1024;
		p10.h = 768;

		PHOTOS.add(p1);
		PHOTOS.add(p2);
		PHOTOS.add(p3);
		PHOTOS.add(p4);
		PHOTOS.add(p5);
		PHOTOS.add(p6);
		PHOTOS.add(p7);
		PHOTOS.add(p8);
		PHOTOS.add(p9);
		PHOTOS.add(p10);
	}

	public static List<CircleItem> createCircleDatas() {
		List<CircleItem> circleDatas = new ArrayList<CircleItem>();
		for (int i = 0; i < 15; i++) {
			CircleItem item = new CircleItem();
			User user = getUser();
			item.setId(String.valueOf(circleId++));
			item.setUser(user);
			item.setContent(getContent());
			item.setCreateTime("12月24日");

			item.setFavorters(createFavortItemList());
			item.setComments(createCommentItemList());
			int type = getRandomNum(10) % 2;
			if (type == 0) {
				item.setType("1");// 链接
				item.setLinkImg("http://cdn2.image.apk.gfan.com/asdf/PImages/2014/12/26/211610_2d6bc9db3-77eb-4d80-9330-cd5e95fa091f.png");
				item.setLinkTitle("百度一下，你就知道");
			} else if(type == 1){
				item.setType("2");// 图片
				item.setPhotos(createPhotos());
			}else {
				item.setType("3");// 视频
				String videoUrl = "http://xidaokun.io/cicledemo.s.qupai.me/v/80c81c19-7c02-4dee-baca-c97d9bbd6607.mp4";
                String videoImgUrl = "http://file02.16sucai.com/d/file/2014/1124/68d1ffe81ad8f4fc84d580be7b556521.jpg";
				item.setVideoUrl(videoUrl);
                item.setVideoImgUrl(videoImgUrl);
			}
			circleDatas.add(item);
		}

		return circleDatas;
	}

	public static User getUser() {
		return users.get(getRandomNum(users.size()));
	}

	public static String getContent() {
		return CONTENTS[getRandomNum(CONTENTS.length)];
	}

	public static int getRandomNum(int max) {
		Random random = new Random();
		int result = random.nextInt(max);
		return result;
	}

	public static List<PhotoInfo> createPhotos() {
		List<PhotoInfo> photos = new ArrayList<PhotoInfo>();
		int size = getRandomNum(PHOTOS.size());
		if (size > 0) {
			if (size > 9) {
				size = 9;
			}
			for (int i = 0; i < size; i++) {
				PhotoInfo photo = PHOTOS.get(getRandomNum(PHOTOS.size()));
				if (!photos.contains(photo)) {
					photos.add(photo);
				} else {
					i--;
				}
			}
		}
		return photos;
	}

	public static List<FavortItem> createFavortItemList() {
		int size = getRandomNum(users.size());
		List<FavortItem> items = new ArrayList<FavortItem>();
		List<String> history = new ArrayList<String>();
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				FavortItem newItem = createFavortItem();
				String userid = newItem.getUser().getId();
				if (!history.contains(userid)) {
					items.add(newItem);
					history.add(userid);
				} else {
					i--;
				}
			}
		}
		return items;
	}

	public static FavortItem createFavortItem() {
		FavortItem item = new FavortItem();
		item.setId(String.valueOf(favortId++));
		item.setUser(getUser());
		return item;
	}
	
	public static FavortItem createCurUserFavortItem() {
		FavortItem item = new FavortItem();
		item.setId(String.valueOf(favortId++));
		item.setUser(curUser);
		return item;
	}

	public static List<CommentItem> createCommentItemList() {
		List<CommentItem> items = new ArrayList<CommentItem>();
		int size = getRandomNum(10);
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				items.add(createComment());
			}
		}
		return items;
	}

	public static CommentItem createComment() {
		CommentItem item = new CommentItem();
		item.setId(String.valueOf(commentId++));
		item.setContent("哈哈");
		User user = getUser();
		item.setUser(user);
		if (getRandomNum(10) % 2 == 0) {
			while (true) {
				User replyUser = getUser();
				if (!user.getId().equals(replyUser.getId())) {
					item.setToReplyUser(replyUser);
					break;
				}
			}
		}
		return item;
	}
	
	/**
	 * 创建发布评论
	 * @return
	 */
	public static CommentItem createPublicComment(String content){
		CommentItem item = new CommentItem();
		item.setId(String.valueOf(commentId++));
		item.setContent(content);
		item.setUser(curUser);
		return item;
	}
	
	/**
	 * 创建回复评论
	 * @return
	 */
	public static CommentItem createReplyComment(User replyUser, String content){
		CommentItem item = new CommentItem();
		item.setId(String.valueOf(commentId++));
		item.setContent(content);
		item.setUser(curUser);
		item.setToReplyUser(replyUser);
		return item;
	}
	
	
	public static CircleItem createVideoItem(String videoUrl, String imgUrl){
		CircleItem item = new CircleItem();
		item.setId(String.valueOf(circleId++));
		item.setUser(curUser);
		//item.setContent(getContent());
		item.setCreateTime("12月24日");

		//item.setFavorters(createFavortItemList());
		//item.setComments(createCommentItemList());
        item.setType("3");// 图片
        item.setVideoUrl(videoUrl);
        item.setVideoImgUrl(imgUrl);
		return item;
	}
}
