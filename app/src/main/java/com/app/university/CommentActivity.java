package com.app.university;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.app.university.view.SwipeRefreshAndLoadLayout;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class CommentActivity extends Activity implements SwipeRefreshAndLoadLayout.OnRefreshListener{
    private String mEvnetID;
    private int mGroupType;
    private SwipeRefreshAndLoadLayout mSwipeLayout;
    private MessageAdapter mAdapter;
    private List<MessageItem> mMessageList;

    protected ListView mListView;
    private Context mContext;
    private ImageLoader mImageLoader;
    private int mStart = 0;
    boolean mClear = false;
    boolean mNoMore = false;
    boolean mFlagloading = false;
    boolean mPosting = false;




    public class MessageItem {

        public String title = "";
        public String eventID = "";
        public String commentID = "";
        public String content = "";
        public String groupid = "";
        public String userName = "";
        public String userID = "";
        public JSONArray imageNameList;
        public int type = 0;
        public String url = "";
        public int likenum = 0;
        public int commentnum = 0;
        public int anonymous = 0;
        public long postTime = 0;
        public int time = 0;
        public int eventType;
        private Calendar date;

        public static final int TYPE_TITLE = 0;
        public static final int TYPE_MESSAGE = 1;
        public static final int TYPE_COMMENT = 2;

        public static final int TYPE_COUNT = 3;

        public MessageItem() {

        }

        public int getType() {
            return TYPE_COMMENT;
        }

        public String getDateDisplayString(Context context) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(postTime*1000);
            Date dt = date.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            return sdf.format(dt);
        }

    }



    public class BitmapCache implements ImageLoader.ImageCache {

        private LruCache<String, Bitmap> mCache;

        public BitmapCache() {
            int maxSize = 10 * 1024 * 1024;
            mCache = new LruCache<String, Bitmap>(maxSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                }
            };
        }

        @Override
        public Bitmap getBitmap(String url) {
            return mCache.get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            mCache.put(url, bitmap);
        }

    }


    private class MessageAdapter extends BaseAdapter {
        private String TAG = "MyCourseAdapter";

        private Context context;
        private LayoutInflater inflater;
        private List<MessageItem> messageList;



        public MessageAdapter(Context context, List<MessageItem> messageList) {
            this.context = context;
            this.messageList = messageList;
        }



        @Override
        public int getCount() {
            return messageList.size();
        }

        @Override
        public Object getItem(int location) {
            return messageList.get(location);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0){
                return MessageItem.TYPE_MESSAGE;
            }
            if (messageList != null && position < messageList.size()) {
                return messageList.get(position).getType();
            }
            return super.getItemViewType(position);
        }

        @Override
        public int getViewTypeCount() {
            return MessageItem.TYPE_COUNT;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            int type = getItemViewType(position);

            switch (type) {
                case MessageItem.TYPE_MESSAGE: {
                    EventViewHolder holder = null;
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.event_item, null);
                        holder = new EventViewHolder();
                        holder.headImage = (NetworkImageView) convertView.findViewById(R.id.event_item_head);
                        holder.textName = (TextView) convertView.findViewById(R.id.event_item_name);
                        holder.textDate = (TextView) convertView.findViewById(R.id.event_item_date);
                        holder.textContent = (TextView) convertView.findViewById(R.id.event_item_content);
                        holder.contentImage1 = (NetworkImageView) convertView.findViewById(R.id.event_item_image1);
                        holder.contentImage2 = (NetworkImageView) convertView.findViewById(R.id.event_item_image2);
                        holder.contentImage3 = (NetworkImageView) convertView.findViewById(R.id.event_item_image3);
                        holder.contentImageonly = (NetworkImageView) convertView.findViewById(R.id.event_item_image_only);
                        holder.textLikeNum = (TextView) convertView.findViewById(R.id.event_item_like);
                        holder.textCommentNum = (TextView) convertView.findViewById(R.id.event_item_reply);
                        holder.layerUrlLink = (LinearLayout) convertView.findViewById(R.id.linear_url_link);
                        holder.textUrl = (TextView) convertView.findViewById(R.id.text_url);
                        holder.layerImageList = (LinearLayout) convertView.findViewById(R.id.layer_image_list);
                        holder.layerImageOne = (FrameLayout) convertView.findViewById(R.id.layer_image_one);
                        holder.LikeLayer = (FrameLayout) convertView.findViewById(R.id.event_like);
                        holder.CommentLayer = (FrameLayout) convertView.findViewById(R.id.event_comment);
                        holder.imageList.add(holder.contentImage1);
                        holder.imageList.add(holder.contentImage2);
                        holder.imageList.add(holder.contentImage3);
                        convertView.setTag(holder);
                    } else {
                        holder = (EventViewHolder) convertView.getTag();

                    }

                    if(messageList.get(position).userName.length() == 0){
                        holder.textName.setText(R.string.event_anonymous);
                    }
                    else{
                        holder.textName.setText(messageList.get(position).userName);
                    }


                    holder.textDate.setText(messageList.get(position).getDateDisplayString(mContext));
                    holder.textContent.setText(messageList.get(position).content);
                    holder.textContent.setMaxLines(1000);
                    //holder.textLikeNum.setText(String.valueOf(messageList.get(position).likenum));
                    holder.textCommentNum.setText(String.valueOf(messageList.get(position).commentnum));

                    if(messageList.get(position).url.compareTo("") == 0){
                        holder.layerUrlLink.setVisibility(View.GONE);
                        holder.textUrl.setText("");
                    }
                    else{
                        holder.layerUrlLink.setVisibility(View.VISIBLE);
                        holder.textUrl.setText(messageList.get(position).url.toString());
                        holder.layerUrlLink.setOnClickListener(new UrlItem_Click(position));
                    }

                    holder.headImage.setDefaultImageResId(R.mipmap.headphoto);
                    holder.headImage.setErrorImageResId(R.mipmap.headphoto);
                    holder.headImage.setImageUrl(NETTag.API_GET_HEADIMAGE_SMALL+"?id="+ messageList.get(position).userID+".jpg", mImageLoader);
                    //ImageLoader.ImageListener headlistener = ImageLoader.getImageListener(holder.headImage,  R.mipmap.headphoto, R.mipmap.headphoto);
                    //mImageLoader.get(NETTag.API_GET_HEADIMAGE_SMALL+"?id="+ messageList.get(position).userID+".jpg", headlistener);

                    if(messageList.get(position).imageNameList.length() == 0){
                        holder.layerImageList.setVisibility(View.GONE);
                        holder.layerImageOne.setVisibility(View.GONE);

                    }
                    else if(messageList.get(position).imageNameList.length() == 1){
                        holder.layerImageList.setVisibility(View.GONE);
                        holder.layerImageOne.setVisibility(View.VISIBLE);

                        holder.contentImageonly.setDefaultImageResId(R.drawable.abc_list_divider_mtrl_alpha);
                        holder.contentImageonly.setErrorImageResId(R.drawable.abc_list_divider_mtrl_alpha);
                        try {
                            holder.contentImageonly.setImageUrl(NETTag.API_GET_FEEDIMAGE_SMALL+"?id="+ messageList.get(position).imageNameList.getString(0), mImageLoader);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //ImageLoader.ImageListener listener = ImageLoader.getImageListener(holder.contentImageonly, R.drawable.abc_list_divider_mtrl_alpha, R.drawable.abc_list_divider_mtrl_alpha);
                        //try {
                        //    mImageLoader.get(NETTag.API_GET_FEEDIMAGE_SMALL+"?id="+ messageList.get(position).imageNameList.getString(0), listener);
                        //} catch (JSONException e) {
                        //    e.printStackTrace();
                        //}
                        holder.layerImageOne.setOnClickListener(new Image_Click(position,0));

                    }
                    else{
                        holder.layerImageList.setVisibility(View.VISIBLE);
                        holder.layerImageOne.setVisibility(View.GONE);
                        for(int i=0; i<3; i++) {
                            if(i<messageList.get(position).imageNameList.length()){
                                holder.imageList.get(i).setVisibility(View.VISIBLE);
                                holder.imageList.get(i).setOnClickListener(new Image_Click(position, i));
                                holder.imageList.get(i).setDefaultImageResId(R.drawable.abc_list_divider_mtrl_alpha);
                                holder.imageList.get(i).setErrorImageResId(R.drawable.abc_list_divider_mtrl_alpha);
                                try {
                                    holder.imageList.get(i).setImageUrl(NETTag.API_GET_FEEDIMAGE_SMALL + "?id=" + messageList.get(position).imageNameList.getString(i), mImageLoader);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else{
                                holder.imageList.get(i).setVisibility(View.GONE);
                            }
                        }

                    }

                    //holder.LikeLayer.setOnClickListener(new LikeItem_Click(position));
                    holder.CommentLayer.setOnClickListener(new CommentItem_Click(position));
                    holder.LikeLayer.setOnClickListener(new LikeItem_Click(position));
                    if(messageList.get(position).url.compareTo("") == 0){
                        holder.LikeLayer.setVisibility(View.INVISIBLE);
                    }
                    else{
                        holder.LikeLayer.setVisibility(View.VISIBLE);
                    }



                    //holder.headImage.setImageURI();
                    //holder.contentImage1.setImageURI()
                    //holder.contentImage2.setImageURI()
                    //holder.contentImage3.setImageURI()

                    break;
                }
                case MessageItem.TYPE_TITLE:{
                    EventViewHolder holder = null;
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.message_title, null);
                        holder = new EventViewHolder();

                        holder.title = (TextView) convertView.findViewById(R.id.group_title_name);

                        convertView.setTag(holder);
                    } else {
                        holder = (EventViewHolder) convertView.getTag();

                    }

                    holder.title.setText(messageList.get(position).title);

                    break;
                }
                case MessageItem.TYPE_COMMENT: {
                    EventViewHolder holder = null;
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.comment_item, null);
                        holder = new EventViewHolder();
                        holder.headImage = (NetworkImageView) convertView.findViewById(R.id.comment_item_head);
                        holder.textName = (TextView) convertView.findViewById(R.id.comment_item_name);
                        holder.textDate = (TextView) convertView.findViewById(R.id.comment_item_date);
                        holder.textContent = (TextView) convertView.findViewById(R.id.comment_item_content);
                        convertView.setTag(holder);
                    } else {
                        holder = (EventViewHolder) convertView.getTag();

                    }

                    if(messageList.get(position).userName.length() == 0){
                        holder.textName.setText(R.string.event_anonymous);
                    }
                    else{
                        holder.textName.setText(messageList.get(position).userName);
                    }

                    holder.textDate.setText(messageList.get(position).getDateDisplayString(mContext));
                    holder.textContent.setText(messageList.get(position).content);
                    holder.textContent.setMaxLines(100);

                    holder.headImage.setDefaultImageResId(R.mipmap.headphoto);
                    holder.headImage.setErrorImageResId(R.mipmap.headphoto);
                    holder.headImage.setImageUrl(NETTag.API_GET_HEADIMAGE_SMALL+"?id="+ messageList.get(position).userID+".jpg", mImageLoader);
                    //ImageLoader.ImageListener headlistener = ImageLoader.getImageListener(holder.headImage, R.mipmap.headphoto, R.mipmap.headphoto);
                    //mImageLoader.get(NETTag.API_GET_HEADIMAGE_SMALL+"?id="+ messageList.get(position).userID+".jpg", headlistener);
                    break;
                }
                default:
                    break;
            }

            return convertView;
        }


        class Image_Click implements View.OnClickListener {
            private int mposition;
            private int mIndex;
            Image_Click(int pos,int index) {
                mposition = pos;
                mIndex = index;

            }
            public void onClick(View v) {

                try {
                    Bundle bundle = new Bundle();
                    bundle.putString(Data.REMOTE_IMAGE_VIEWER_ID, messageList.get(mposition).imageNameList.getString(mIndex));
                    Intent intent = new Intent(mContext, ImageViewerActivity.class);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, 0);
                    Log.d("MessageActivity comment click pos = ", String.valueOf(mposition)  );

                } catch( Exception e ) {

                }

                Log.d("MessageActivity like click pos = ", String.valueOf(mposition)  );
            }
        }

        class CommentItem_Click implements View.OnClickListener {
            private int mposition;



            CommentItem_Click(int pos) {
                mposition = pos;

            }
            public void onClick(View v) {
                Log.d("CommentActivity comment click pos = ", String.valueOf(mposition)  );
            }
        }

        class UrlItem_Click implements View.OnClickListener {
            private int mposition;



            UrlItem_Click(int pos) {
                mposition = pos;

            }
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(Data.NOTIFY_URL, mMessageList.get(mposition).url);
                Intent intent = new Intent(mContext, WebViewerActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                Log.d("NotifyList  click pos = ", String.valueOf(mposition)  );

            }
        }

        class LikeItem_Click implements View.OnClickListener {
            private int mposition;

            LikeItem_Click(int pos) {
                mposition = pos;
            }
            public void onClick(View v) {

                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, messageList.get(mposition).url);
                startActivity(Intent.createChooser(share, getString(R.string.share)));
            }
        }


        public class EventViewHolder {
            public TextView title;
            public NetworkImageView headImage;
            public TextView  textName;
            public TextView  textDate;
            public TextView  textContent;
            public NetworkImageView contentImage1;
            public NetworkImageView contentImage2;
            public NetworkImageView contentImage3;
            public NetworkImageView contentImageonly;
            public TextView  textLikeNum;
            public TextView  textCommentNum;
            public LinearLayout  layerImageList;
            public FrameLayout  layerImageOne;
            public FrameLayout  LikeLayer;
            public FrameLayout  CommentLayer;
            public LinearLayout  layerUrlLink;
            public TextView  textUrl;

            ArrayList<NetworkImageView> imageList = new ArrayList<NetworkImageView>();


        }


    }

    public void GetMessage(int start){
        mStart = start;
        mFlagloading = true;
        mSwipeLayout.setRefreshing(true);
        if(start == 0){
            mClear = true;
        }
        GetCommentRequest stringRequest = new GetCommentRequest(this, listener, errorListener, mEvnetID, start, Data.GET_MESSAGE_NUMBER);
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);

    }


    final Response.Listener<String> dellistener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if(jsonObject.getString(NETTag.RESULT).compareTo(NETTag.OK) == 0){
                    String commentid = jsonObject.getString(NETTag.POST_COMMENT_ID);
                    for(int i=0 ; i< mMessageList.size() ; i++ ){
                        if(mMessageList.get(i).commentID.compareTo(commentid) == 0){
                            mMessageList.remove(i);
                            mAdapter.notifyDataSetChanged();
                            break;
                        }
                    }

                }
                else{

                    Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (JSONException e) {

                Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            } finally {

            }
        }
    };

    final Response.ErrorListener delerrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("CommentActivity", error.getMessage(), error);
            Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
            return;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        mContext = this;
        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        mEvnetID =  bundle.getString(Data.COMMENT_EVNET_ID);
        mGroupType = bundle.getInt(Data.GROUP_TYPE);

        Tracker t = ((UniversityApp) getApplication()).getTracker(UniversityApp.TrackerName.APP_TRACKER);
        // Set screen name.
        // Where path is a String representing the screen name.
        t.setScreenName("View Comment");
        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());

        Log.d("CommentActivity course id = ", mEvnetID);

        mImageLoader = MySingleton.getInstance(this.getApplicationContext()).getImageLoader();

        mMessageList = new ArrayList<MessageItem>();
        mAdapter = new MessageAdapter(this, mMessageList);
        mListView = (ListView) findViewById(R.id.comment_list);
        mListView.setAdapter(mAdapter);


        mSwipeLayout = (com.app.university.view.SwipeRefreshAndLoadLayout) findViewById(R.id.comment_list_swipe);
        mSwipeLayout.setOnRefreshListener(CommentActivity.this);
        mSwipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeLayout.setmMode(SwipeRefreshAndLoadLayout.Mode.PULL_FROM_START);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                if (firstVisibleItem + visibleItemCount > totalItemCount -3
                        && totalItemCount > 0
                        && mFlagloading == false
                        && mNoMore == false) {
                    Log.d("AddCourseActivity nexttime = ", String.valueOf(mStart) );

                    GetMessage(mStart);
                }
                return;
            }
        });

        final Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject.getString(NETTag.RESULT).compareTo(NETTag.OK) == 0){
                        EditText editContent = (EditText) findViewById(R.id.edit_comment);
                        editContent.setText("");
                        mPosting = false;
                        editContent.clearFocus();
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editContent.getWindowToken(), 0);
                        GetMessage(0);
                        Log.d("NewMessageActivity", "success");
                        Toast.makeText(mContext, R.string.comment_success, Toast.LENGTH_SHORT).show();
                    }
                    else{
                        mPosting = false;
                        Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (JSONException e) {
                    mPosting = false;
                    Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    return;
                } finally {

                }
            }
        };

        final Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("CommentActivity", error.getMessage(), error);
                mPosting = false;
                Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
                return;
            }
        };

        ImageButton btnPostComment = (ImageButton) findViewById(R.id.btn_post_comment);
        btnPostComment.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText editContent = (EditText) findViewById(R.id.edit_comment);
                if(editContent.length() == 0){
                    return;
                }
                if(mPosting == true){
                    return;
                }
                mPosting = true;
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(NETTag.GET_COMMENT_EVENTID, mEvnetID);
                    jsonObject.put(NETTag.POST_COMMENT_TYPE, mGroupType);
                    jsonObject.put(NETTag.POST_COMMENT_CONTENT, editContent.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                Log.d("CommentActivity post string = ", jsonObject.toString());
                PostCommentRequest stringRequest = new PostCommentRequest(mContext, listener, errorListener, jsonObject.toString());
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
            }
        });


        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                if(position == 0){
                    return false;
                }
                SharedPreferences settings = getSharedPreferences ("ID", Context.MODE_PRIVATE);
                if(mMessageList.get(position).userID.compareTo(settings.getString(Data.USER_ID,"")) == 0){
                    final CharSequence courseOption[] = { getString( R.string.delete_meddage) };

                    AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                    alert.setTitle(getString(R.string.delete_meddage));
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    alert.setSingleChoiceItems(courseOption, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (which == 0) {
                                DelCommentRequest stringRequest = new DelCommentRequest(mContext, dellistener, delerrorListener, mMessageList.get(position).commentID);
                                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
                                dialog.cancel();
                            } else {
                                dialog.cancel();
                            }
                        }
                    });
                    alert.show();
                }
                return false;
            }
        });


        GetMessage(0);

    }

    Response.Listener<String> listener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.d("CommentActivity  response = ", response);
            try {
                JSONObject jsonObject = new JSONObject(response);
                mFlagloading = false;
                if(jsonObject.getString(NETTag.RESULT).compareTo(NETTag.OK) == 0){
                    if(mClear == true){
                        mMessageList.clear();

                        MessageItem messageItem = new MessageItem();
                        JSONObject eventinfoJson = jsonObject.getJSONObject(NETTag.GET_COMMENT_EVENTINFO);
                        messageItem.eventID = eventinfoJson.getString(NETTag.GROPU_EVNET_EVENTID);
                        messageItem.content = eventinfoJson.getString(NETTag.GROPU_EVNET_CONTENT);
                        messageItem.groupid =     eventinfoJson.getString(NETTag.GROPU_EVNET_GROUPID);
                        messageItem.userName = eventinfoJson.getString(NETTag.GROPU_EVNET_NAME);
                        messageItem.userID =eventinfoJson.getString(NETTag.GROPU_EVNET_USERID);
                        messageItem.imageNameList =eventinfoJson.getJSONArray(NETTag.GROPU_EVNET_IMAGELIST);
                        messageItem.type =eventinfoJson.getInt(NETTag.GROPU_EVNET_TYPE);
                        messageItem.url =eventinfoJson.getString(NETTag.GROPU_EVNET_URL);
                        messageItem.likenum =eventinfoJson.getInt(NETTag.GROPU_EVNET_LIKENUM);
                        messageItem.commentnum =eventinfoJson.getInt(NETTag.GROPU_EVNET_COMMENTNUM);
                        messageItem.anonymous =eventinfoJson.getInt(NETTag.GROPU_EVNET_ANONYMOUS);
                        messageItem.postTime =eventinfoJson.getInt(NETTag.GROPU_EVNET_POSTTIME);
                        messageItem.time =eventinfoJson.getInt(NETTag.GROPU_EVNET_TIME);
                        mMessageList.add(messageItem);
                    }

                    mStart = Integer.valueOf(jsonObject.getString(NETTag.GROPU_EVNET_NEXT_START_TIME));
                    JSONArray jsonCourseList= new JSONArray(jsonObject.getString(NETTag.GET_COMMENT_LIST));
                    mNoMore = false;
                    if(jsonCourseList.length() < Data.GET_MESSAGE_NUMBER){
                        mNoMore = true;
                    }
                    for (int i = 0; i < jsonCourseList.length(); i++) {
                        JSONObject jsonMessageItem = jsonCourseList.optJSONObject(i);


                        MessageItem messageItem = new MessageItem();

                        messageItem.eventID = jsonMessageItem.getString(NETTag.GET_COMMENT_EVENTID);
                        messageItem.content = jsonMessageItem.getString(NETTag.POST_COMMENT_CONTENT);
                        messageItem.commentID = jsonMessageItem.getString(NETTag.POST_COMMENT_ID);
                        messageItem.userName = jsonMessageItem.getString(NETTag.POST_COMMENT_USER_NAME);
                        messageItem.userID = jsonMessageItem.getString(NETTag.POST_COMMENT_USERID);
                        messageItem.postTime =jsonMessageItem.getInt(NETTag.POST_COMMENT_POST_TIME);
                        messageItem.type =jsonMessageItem.getInt(NETTag.POST_COMMENT_TYPE);
                        mMessageList.add(messageItem);

                        mSwipeLayout.setRefreshing(false);
                    }
                }
                else{
                    mSwipeLayout.setRefreshing(false);
                    mFlagloading = false;
                    Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (JSONException e) {
                mSwipeLayout.setRefreshing(false);
                mFlagloading = false;
                Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            } finally {
                mSwipeLayout.setRefreshing(false);
                mFlagloading = false;
                if (mClear) {
                    Log.d("CommentActivity", "scroll to top");

                    mListView.setSelectionAfterHeaderView();
                    mListView.setSelection(0);
                    mClear = false;
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("CommentActivity", error.getMessage(), error);
            mSwipeLayout.setRefreshing(false);
            Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
            return;
        }
    };


    @Override
    public void onRefresh() {

        mNoMore = false;
        GetMessage(0);
        Log.d("CommentActivity  = ", "onRefresh");
        return;
    }
    @Override
    public void onLoadMore() {
        //mSwipeLayout.setRefreshing(false);
        Log.d("CommentActivity  = ", "onLoadMore");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
