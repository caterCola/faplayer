package org.stagex.danmaku.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keke.player.R;
import org.stagex.danmaku.adapter.ChannelAdapter;
import org.stagex.danmaku.adapter.ChannelInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.nmbb.oplayer.scanner.ChannelListBusiness;
import com.nmbb.oplayer.scanner.DbHelper;
import com.nmbb.oplayer.scanner.POChannelList;

public class SearchActivity extends Activity {

	private static final String LOGTAG = "SearchActivity";
	private ListView search_list = null;
	private EditText mSearch = null;

	private List<POChannelList> listChannel = null;

	/* 频道收藏的数据库 */
	private DbHelper<POChannelList> mDbHelper;
	private Map<String, Object> mDbWhere = new HashMap<String, Object>(2);

	/* 顶部标题栏的控件 */
	private TextView button_back;
	private ImageView button_delete;

	// 更新收藏频道的数目
	private SharedPreferences sharedPreferences;
	private Editor editor;
	private int fav_num = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.search);

		/* 获得收藏类别的list */
		search_list = (ListView) findViewById(R.id.search_list);
		// 防止滑动黑屏
		search_list.setCacheColorHint(Color.TRANSPARENT);

		/* 频道收藏的数据库 */
		mDbHelper = new DbHelper<POChannelList>();

		/* 顶部标题栏的控件 */
		button_back = (TextView) findViewById(R.id.back_btn);
		button_delete = (ImageView) findViewById(R.id.delete_btn);
		/* 搜索栏 */
		mSearch = (EditText) findViewById(R.id.search_txt);

		mSearch.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				setSearchView();
			}
		});

		/* 设置监听 */
		setListensers();

		// 更新收藏频道的数目
		sharedPreferences = getSharedPreferences("keke_player", MODE_PRIVATE);
		editor = sharedPreferences.edit();
	}

	/*
	 * 设置其他未分类台源的channel list
	 */
	private void setSearchView() {
		// clear old listview
		if (listChannel != null) {
			listChannel.clear();
		}

		// get edit text
		String searchName = mSearch.getText().toString();
		if (searchName.length() > 0) {
			// get search channel list
			listChannel = ChannelListBusiness.getAllSearchChannels("name",
					searchName);
			ChannelAdapter adapter = new ChannelAdapter(this, listChannel);
			search_list.setAdapter(adapter);
			search_list.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					// TODO Auto-generated method stub
					POChannelList info = (POChannelList) search_list
							.getItemAtPosition(arg2);
					// Log.d("ChannelInfo",
					// "name = " + info.getName() + "[" + info.getUrl() + "]");

					// startLiveMedia(info.getUrl(), info.getName());
					showAllSource(info.getAllUrl(), info.name,
							info.program_path, info.save);
				}
			});

			// 增加长按频道收藏功能
			search_list
					.setOnItemLongClickListener(new OnItemLongClickListener() {

						@Override
						public boolean onItemLongClick(AdapterView<?> arg0,
								View arg1, int arg2, long arg3) {
							POChannelList info = (POChannelList) search_list
									.getItemAtPosition(arg2);
							showFavMsg(arg1, info);
							return true;
						}
					});

			search_list.setOnScrollListener(new OnScrollListener() {

				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					// TODO Auto-generated method stub

				}
			});
		} else {
			ChannelAdapter adapter = new ChannelAdapter(this, listChannel);
			search_list.setAdapter(adapter);
		}
	}

	/**
	 * 显示所有的台源
	 */
	private void showAllSource(ArrayList<String> all_url, String name,
			String path, Boolean isStar) {
		// 如果该节目只有一个候选源地址，那么直接进入播放界面
		if (all_url.size() == 1) {
			Intent intent = new Intent(SearchActivity.this,
					PlayerActivity.class);
			ArrayList<String> playlist = new ArrayList<String>();
			playlist.add(all_url.get(0));
			intent.putExtra("selected", 0);
			intent.putExtra("playlist", playlist);
			intent.putExtra("title", name);
			intent.putExtra("channelStar", isStar);
			startActivity(intent);
		} else {
			// 否则进入候选源界面
			Intent intent = new Intent(SearchActivity.this,
					ChannelSourceActivity.class);
			intent.putExtra("all_url", all_url);
			intent.putExtra("channel_name", name);
			intent.putExtra("program_path", path);
			intent.putExtra("channelStar", isStar);
			startActivity(intent);
		}
	}

	/**
	 * 提示是否收藏
	 */
	private void showFavMsg(View view, POChannelList info) {

		final ImageView favView = (ImageView) view.findViewById(R.id.fav_icon);
		final POChannelList saveInfo = info;

		fav_num = sharedPreferences.getInt("fav_num", 0);
		Log.d(LOGTAG, "===>current fav_num = " + fav_num);

		// 为提升用户点击广告的热情，特地将收藏频道数目超过3个的的积分额度为100积分
		if (fav_num >= 3) {
			// FIXME 此处可以修改积分限制
			if (sharedPreferences.getInt("pointTotal", 0) < 100) {
				new AlertDialog.Builder(SearchActivity.this)
						.setIcon(R.drawable.ic_dialog_alert)
						.setTitle("温馨提示")
						.setMessage(
								"您的积分不足100分，暂时只能收藏3个频道！\n您可以到【设置】中打开应用推荐赚取相应的积分，感谢您的支持！")
						.setPositiveButton("赚积分",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										Intent intent = new Intent();
										intent.setClass(SearchActivity.this,
												SetupActivity.class);
										startActivity(intent);
									}
								})
						.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
									}
								}).show();

				return;
			}
		}

		new AlertDialog.Builder(SearchActivity.this)
				.setIcon(R.drawable.ic_dialog_alert).setTitle("温馨提示")
				.setMessage("确定收藏该直播频道吗？")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing - it will close on its own
						// TODO 增加加入数据库操作
						favView.setVisibility(View.VISIBLE);
						updateDatabase(saveInfo);
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).show();
	}

	/**
	 * 收藏后更新某一条数据信息
	 * 
	 */
	private void updateDatabase(POChannelList channelList) {
		if (channelList.save == false) {
			// 如果重复点击，只算一次添加
			// 收藏频道数加1
			editor.putInt("fav_num", fav_num + 1);
			editor.commit();
		}
		channelList.save = true;

		// update
		Log.i(LOGTAG, "==============>" + channelList.name + "###"
				+ channelList.poId + "###" + channelList.save);

		mDbHelper.update(channelList);
	}

	// Listen for button clicks
	private void setListensers() {
		button_delete.setOnClickListener(goListener);
		button_back.setOnClickListener(goListener);
	}

	// 按键监听
	private Button.OnClickListener goListener = new Button.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back_btn:
				// 回到上一个界面(Activity)
				finish();
				break;
			case R.id.delete_btn:
				// 删除所有的收藏的频道
				if (mSearch.getText().toString().length() > 0)
					mSearch.setText(null);
				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	};
}
